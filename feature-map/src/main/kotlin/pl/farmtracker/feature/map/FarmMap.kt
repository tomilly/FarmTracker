package pl.farmtracker.feature.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

private val PolandCenter = LatLng(52.07, 19.48)
private const val MY_LOCATION_ZOOM = 16.0
private const val MY_LOCATION_TRANSITION_MS = 750L

/**
 * Mapa MapLibre w Compose. Stan (warstwy, prośby o wyśrodkowanie) przychodzi z góry;
 * MapView żyje tak długo jak ten composable i podąża za cyklem życia ekranu.
 */
@Composable
internal fun FarmMap(
    baseLayer: BaseLayer,
    showParcels: Boolean,
    locationEnabled: Boolean,
    centerOnMeRequest: Int,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    MapViewLifecycle(mapView)

    AndroidView(factory = { mapView }, modifier = modifier)

    LaunchedEffect(mapView) {
        mapView.getMapAsync { mapLibreMap ->
            mapLibreMap.cameraPosition = CameraPosition.Builder()
                .target(PolandCenter)
                .zoom(MapUiState.INITIAL_ZOOM)
                .build()
            // Zawsze północ u góry i widok z góry – obrócona/pochylona mapa dezorientuje (BRIEF §4).
            mapLibreMap.uiSettings.isRotateGesturesEnabled = false
            mapLibreMap.uiSettings.isTiltGesturesEnabled = false
            mapLibreMap.addOnCameraIdleListener { currentOnZoomChanged(mapLibreMap.cameraPosition.zoom) }
            mapLibreMap.setStyle(Style.Builder().fromUri(MapSources.BASE_STYLE_URL)) { loaded ->
                loaded.usePolishLabels()
                loaded.addFarmLayers()
                style = loaded
            }
            map = mapLibreMap
        }
    }

    LaunchedEffect(style, baseLayer, showParcels) {
        style?.applyVisibility(baseLayer, showParcels)
    }

    LaunchedEffect(map, style, locationEnabled, centerOnMeRequest) {
        val mapLibreMap = map ?: return@LaunchedEffect
        val loadedStyle = style ?: return@LaunchedEffect
        if (!locationEnabled || !mapLibreMap.showMyLocation(context, loadedStyle)) return@LaunchedEffect
        if (centerOnMeRequest > 0) {
            // Śledzenie przesuwa mapę do pozycji (także gdy GPS dopiero ją ustali); przesunięcie palcem je wyłącza.
            // Zoom podajemy razem z trybem – osobne zoomWhileTracking() jest ignorowane, zanim warstwa pozycji się pokaże.
            mapLibreMap.locationComponent.setCameraMode(
                CameraMode.TRACKING,
                MY_LOCATION_TRANSITION_MS,
                MY_LOCATION_ZOOM,
                null,
                null,
                null,
            )
        }
    }
}

@Composable
private fun MapViewLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // Wyjście z ekranu mapy, gdy aktywność dalej działa – domykamy cykl MapView ręcznie.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }
}

/** Ortofotomapa pod etykietami mapy bazowej (zdjęcie + nazwy miejscowości), działki nad zdjęciem. */
private fun Style.addFarmLayers() {
    addSource(
        RasterSource(
            MapSources.ORTHO_SOURCE_ID,
            TileSet("2.2.0", MapSources.ORTHO_TILES).apply {
                maxZoom = MapSources.ORTHO_MAX_ZOOM
                attribution = MapSources.GUGIK_ATTRIBUTION
            },
            MapSources.RASTER_TILE_SIZE,
        ),
    )
    addSource(
        RasterSource(
            MapSources.PARCELS_SOURCE_ID,
            TileSet("2.2.0", MapSources.PARCELS_TILES).apply {
                attribution = MapSources.GUGIK_ATTRIBUTION
            },
            MapSources.RASTER_TILE_SIZE,
        ),
    )

    val ortho = RasterLayer(MapSources.ORTHO_LAYER_ID, MapSources.ORTHO_SOURCE_ID)
        .withProperties(visibility(Property.NONE))
    val parcels = RasterLayer(MapSources.PARCELS_LAYER_ID, MapSources.PARCELS_SOURCE_ID)
        .withProperties(visibility(Property.NONE))
        .apply { minZoom = MapSources.PARCELS_MIN_ZOOM.toFloat() }

    val firstLabelLayerId = layers.firstOrNull { it is SymbolLayer }?.id
    if (firstLabelLayerId != null) {
        addLayerBelow(ortho, firstLabelLayerId)
        addLayerAbove(parcels, MapSources.ORTHO_LAYER_ID)
    } else {
        addLayer(ortho)
        addLayer(parcels)
    }
}

/**
 * Styl bazowy podpisuje miejscowości po angielsku (name_en). Dla rolników: polska nazwa, a gdy jej brak –
 * nazwa lokalna z OSM. Warstwy z numerami dróg (ref) zostają bez zmian.
 */
private fun Style.usePolishLabels() {
    val polishName = Expression.coalesce(Expression.get("name:pl"), Expression.get("name"))
    layers.filterIsInstance<SymbolLayer>()
        .filter { it.textField.expression?.toString()?.contains("name") == true }
        .forEach { it.setProperties(textField(polishName)) }
}

private fun Style.applyVisibility(baseLayer: BaseLayer, showParcels: Boolean) {
    getLayer(MapSources.ORTHO_LAYER_ID)
        ?.setProperties(visibility(if (baseLayer == BaseLayer.PHOTO) Property.VISIBLE else Property.NONE))
    getLayer(MapSources.PARCELS_LAYER_ID)
        ?.setProperties(visibility(if (showParcels) Property.VISIBLE else Property.NONE))
}

/** Włącza kropkę „tu jestem". Zwraca `false`, gdy brak zgody na lokalizację. */
private fun MapLibreMap.showMyLocation(context: Context, style: Style): Boolean {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    val component = locationComponent
    if (!component.isLocationComponentActivated) {
        component.activateLocationComponent(LocationComponentActivationOptions.builder(context, style).build())
    }
    component.isLocationComponentEnabled = true
    component.renderMode = RenderMode.COMPASS
    return true
}
