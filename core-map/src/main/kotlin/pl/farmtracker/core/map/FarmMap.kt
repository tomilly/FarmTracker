package pl.farmtracker.core.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.rasterBrightnessMin
import org.maplibre.android.style.layers.PropertyFactory.rasterSaturation
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.geo.GeoArea
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.ui.FieldColorUi
import java.util.Locale

private val PolandCenter = LatLng(52.07, 19.48)
private const val MY_LOCATION_ZOOM = 16.0
private const val MY_LOCATION_TRANSITION_MS = 750L
private val SHOW_AREA_PADDING = 48.dp
private const val SHOW_AREA_DURATION_MS = 800

// Żółty jak zaznaczenie markerem – dobrze widoczny i na mapie, i na zdjęciu lotniczym.
private const val SELECTION_FILL_COLOR = "#FFD600"
private const val SELECTION_FILL_OPACITY = 0.35f
private const val SELECTION_LINE_COLOR = "#FF6F00"
private const val SELECTION_LINE_WIDTH = 3f

private const val FIELD_COLOR_PROPERTY = "color"
private const val FIELD_NAME_PROPERTY = "name"
private const val FIELD_FILL_OPACITY = 0.3f
private const val FIELD_LINE_WIDTH = 2.5f

// Czcionka z mapy bazowej (OpenFreeMap); biały tekst z czarną obwódką czytelny i na mapie, i na zdjęciu.
private const val FIELD_LABEL_FONT = "Noto Sans Bold"
private const val FIELD_LABEL_SIZE = 18f
private const val FIELD_LABEL_HALO = 1.5f

private const val DRAFT_POINT_RADIUS = 7f
private const val DRAFT_POINT_STROKE = 3f

private const val ENTRY_COLOR = "#1B7F2A"
private const val ENTRY_RADIUS = 11f
private const val ENTRY_STROKE = 3f
private const val ENTRY_LABEL_SIZE = 15f
private const val ENTRY_LABEL_OFFSET = 1.1f

// Baza większa i czarna – odróżnia się od zielonych wjazdów, kolorów pól i niebieskiej kropki „ja".
private const val BASE_COLOR = "#212121"
private const val BASE_RADIUS = 15f
private const val BASE_LABEL_SIZE = 17f

/** Pasy mapy (w pikselach) zasłonięte przez przyciski u góry i na dole. */
internal data class MapCovered(val top: Int = 0, val bottom: Int = 0) {
    /** Kamera celująca w środek odkrytej części mapy. */
    fun camera(target: LatLng, zoom: Double): CameraPosition = CameraPosition.Builder()
        .target(target)
        .zoom(zoom)
        .padding(0.0, top.toDouble(), 0.0, bottom.toDouble())
        .build()
}

/**
 * Mapa MapLibre w Compose. Stan (warstwy, prośby o wyśrodkowanie) przychodzi z góry;
 * MapView żyje tak długo jak ten composable i podąża za cyklem życia ekranu.
 */
@Composable
internal fun FarmMap(
    chrome: MapChromeState,
    overlays: MapOverlays,
    covered: MapCovered,
    onCameraIdle: (zoom: Double, center: GeoPoint) -> Unit,
    onMapTap: (GeoPoint) -> Unit,
    onCameraRequestHandled: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseLayer = chrome.baseLayer
    val showParcels = chrome.showParcels
    val locationEnabled = chrome.locationAccess == LocationAccess.GRANTED
    val cameraRequest = chrome.cameraRequest
    val context = LocalContext.current
    val areaPaddingPx = with(LocalDensity.current) { SHOW_AREA_PADDING.roundToPx() }
    val entryLabel = stringResource(R.string.core_map_entry_label)
    val baseLabel = stringResource(R.string.core_map_base_label)
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val currentCovered by rememberUpdatedState(covered)
    val currentChrome by rememberUpdatedState(chrome)
    val currentOnCameraRequestHandled by rememberUpdatedState(onCameraRequestHandled)

    MapViewLifecycle(mapView)

    AndroidView(factory = { mapView }, modifier = modifier)

    LaunchedEffect(mapView) {
        mapView.getMapAsync { mapLibreMap ->
            // Mapa odtwarzana (powrót na ekran) startuje od ostatniego widoku, nowa – od całej Polski.
            // Ważne też dlatego, że pierwszy „bezruch" kamery zapisuje widok jako ostatni.
            val last = currentChrome.center
            mapLibreMap.cameraPosition = CameraPosition.Builder()
                .target(last?.let { LatLng(it.latitude, it.longitude) } ?: PolandCenter)
                .zoom(if (last != null) currentChrome.zoom else MapChromeState.INITIAL_ZOOM)
                .build()
            // Zawsze północ u góry i widok z góry – obrócona/pochylona mapa dezorientuje (BRIEF §4).
            mapLibreMap.uiSettings.isRotateGesturesEnabled = false
            mapLibreMap.uiSettings.isTiltGesturesEnabled = false
            mapLibreMap.addOnCameraIdleListener {
                val position = mapLibreMap.cameraPosition
                val target = position.target ?: return@addOnCameraIdleListener
                currentOnCameraIdle(position.zoom, GeoPoint(latitude = target.latitude, longitude = target.longitude))
            }
            mapLibreMap.addOnMapClickListener { latLng ->
                currentOnMapTap(GeoPoint(latitude = latLng.latitude, longitude = latLng.longitude))
                true
            }
            mapLibreMap.setStyle(Style.Builder().fromUri(MapSources.BASE_STYLE_URL)) { loaded ->
                loaded.usePolishLabels()
                loaded.addFarmLayers(entryLabel, baseLabel)
                style = loaded
            }
            map = mapLibreMap
        }
    }

    LaunchedEffect(style, baseLayer, showParcels) {
        style?.applyVisibility(baseLayer, showParcels)
    }

    LaunchedEffect(style, overlays.fields) {
        val loadedStyle = style ?: return@LaunchedEffect
        loadedStyle.getSourceAs<GeoJsonSource>(MapSources.FIELDS_SOURCE_ID)
            ?.setGeoJson(overlays.fields.toFieldsFeatureCollection())
        loadedStyle.getSourceAs<GeoJsonSource>(MapSources.FIELD_LABELS_SOURCE_ID)
            ?.setGeoJson(overlays.fields.toLabelsFeatureCollection())
    }

    LaunchedEffect(style, overlays.draft) {
        val loadedStyle = style ?: return@LaunchedEffect
        loadedStyle.getSourceAs<GeoJsonSource>(MapSources.DRAFT_SOURCE_ID)
            ?.setGeoJson(overlays.draft.toDraftFeatureCollection())
        loadedStyle.getSourceAs<GeoJsonSource>(MapSources.DRAFT_POINTS_SOURCE_ID)
            ?.setGeoJson(overlays.draft.toPointsFeatureCollection())
    }

    LaunchedEffect(style, overlays.entryPoints) {
        style?.getSourceAs<GeoJsonSource>(MapSources.ENTRIES_SOURCE_ID)
            ?.setGeoJson(overlays.entryPoints.toPointsFeatureCollection())
    }

    LaunchedEffect(style, overlays.base) {
        style?.getSourceAs<GeoJsonSource>(MapSources.BASE_SOURCE_ID)
            ?.setGeoJson(listOfNotNull(overlays.base).toPointsFeatureCollection())
    }

    LaunchedEffect(style, overlays.highlight) {
        style?.getSourceAs<GeoJsonSource>(MapSources.SELECTION_SOURCE_ID)
            ?.setGeoJson(overlays.highlight.toFeatureCollection())
    }

    // Logo i atrybucja (wymagane licencją map) – u góry, pod przyciskami warstw; na dole zasłaniałby je panel.
    LaunchedEffect(map, covered.top) {
        val ui = map?.uiSettings ?: return@LaunchedEffect
        ui.logoGravity = Gravity.TOP or Gravity.START
        ui.setLogoMargins(ui.logoMarginLeft, covered.top, ui.logoMarginRight, 0)
        ui.attributionGravity = Gravity.TOP or Gravity.START
        ui.setAttributionMargins(ui.attributionMarginLeft, covered.top, ui.attributionMarginRight, 0)
    }

    LaunchedEffect(map, style, locationEnabled, cameraRequest) {
        val mapLibreMap = map ?: return@LaunchedEffect
        val loadedStyle = style ?: return@LaunchedEffect
        val showsMe = locationEnabled && mapLibreMap.showMyLocation(context, loadedStyle)
        // Klatka na ułożenie panelu – np. karta działki pojawia się razem z prośbą o jej pokazanie.
        withFrameNanos { }
        val visible = currentCovered
        val request = currentChrome.pendingCameraRequest
        if (request == null) {
            // Prośba już wykonana, a mapa powstała od nowa (powrót z edycji): zostaje ostatni widok –
            // z tym samym paddingiem, żeby nic nie przesunęło się względem przycisków.
            val last = currentChrome.center ?: return@LaunchedEffect
            val target = LatLng(last.latitude, last.longitude)
            mapLibreMap.moveCamera(CameraUpdateFactory.newCameraPosition(visible.camera(target, currentChrome.zoom)))
            return@LaunchedEffect
        }
        when (request) {
            is CameraRequest.CenterOnMe -> if (showsMe) {
                // Śledzenie trzyma pozycję na środku odkrytej części mapy (padding kamery = zasłonięte pasy).
                mapLibreMap.moveCamera(CameraUpdateFactory.paddingTo(0.0, visible.top.toDouble(), 0.0, visible.bottom.toDouble()))
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
                currentOnCameraRequestHandled(request.id)
            }
            is CameraRequest.ShowPlace -> {
                mapLibreMap.stopFollowingMe()
                val target = LatLng(request.point.latitude, request.point.longitude)
                mapLibreMap.easeCamera(
                    CameraUpdateFactory.newCameraPosition(visible.camera(target, request.zoom)),
                    SHOW_AREA_DURATION_MS,
                )
                currentOnCameraRequestHandled(request.id)
            }
            is CameraRequest.ShowArea -> {
                mapLibreMap.stopFollowingMe()
                val bounds = request.bounds
                val latLngBounds = LatLngBounds.from(bounds.north, bounds.east, bounds.south, bounds.west)
                // Zoom, przy którym obszar mieści się w odkrytej części mapy (z marginesem).
                val padding = intArrayOf(areaPaddingPx, visible.top + areaPaddingPx, areaPaddingPx, visible.bottom + areaPaddingPx)
                val fittedZoom = mapLibreMap.getCameraForLatLngBounds(latLngBounds, padding)?.zoom
                    ?: return@LaunchedEffect
                val zoom = if (request.zoomIn) fittedZoom else minOf(fittedZoom, mapLibreMap.cameraPosition.zoom)
                val center = LatLng(bounds.center.latitude, bounds.center.longitude)
                mapLibreMap.easeCamera(
                    CameraUpdateFactory.newCameraPosition(visible.camera(center, zoom)),
                    SHOW_AREA_DURATION_MS,
                )
                currentOnCameraRequestHandled(request.id)
            }
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
private fun Style.addFarmLayers(entryLabel: String, baseLabel: String) {
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

    // Pola: kolor z właściwości obiektu, półprzezroczyste (widać zdjęcie i granice działek), nazwa na środku.
    addSource(GeoJsonSource(MapSources.FIELDS_SOURCE_ID))
    addSource(GeoJsonSource(MapSources.FIELD_LABELS_SOURCE_ID))
    val fieldColor = Expression.toColor(Expression.get(FIELD_COLOR_PROPERTY))
    val fieldsFill = FillLayer(MapSources.FIELDS_FILL_LAYER_ID, MapSources.FIELDS_SOURCE_ID)
        .withProperties(fillColor(fieldColor), fillOpacity(FIELD_FILL_OPACITY))
    val fieldsLine = LineLayer(MapSources.FIELDS_LINE_LAYER_ID, MapSources.FIELDS_SOURCE_ID)
        .withProperties(lineColor(fieldColor), lineWidth(FIELD_LINE_WIDTH))
    val fieldsLabel = SymbolLayer(MapSources.FIELDS_LABEL_LAYER_ID, MapSources.FIELD_LABELS_SOURCE_ID)
        .withProperties(
            textField(Expression.get(FIELD_NAME_PROPERTY)),
            textFont(arrayOf(FIELD_LABEL_FONT)),
            textSize(FIELD_LABEL_SIZE),
            textColor(Color.WHITE),
            textHaloColor(Color.BLACK),
            textHaloWidth(FIELD_LABEL_HALO),
        )
    addLayerAbove(fieldsFill, MapSources.PARCELS_LAYER_ID)
    addLayerAbove(fieldsLine, MapSources.FIELDS_FILL_LAYER_ID)

    // Zaznaczona działka: półprzezroczyste wypełnienie (widać zdjęcie pod spodem) + wyraźny obrys.
    addSource(GeoJsonSource(MapSources.SELECTION_SOURCE_ID))
    val selectionFill = FillLayer(MapSources.SELECTION_FILL_LAYER_ID, MapSources.SELECTION_SOURCE_ID)
        .withProperties(fillColor(SELECTION_FILL_COLOR), fillOpacity(SELECTION_FILL_OPACITY))
    val selectionLine = LineLayer(MapSources.SELECTION_LINE_LAYER_ID, MapSources.SELECTION_SOURCE_ID)
        .withProperties(lineColor(SELECTION_LINE_COLOR), lineWidth(SELECTION_LINE_WIDTH))
    addLayerAbove(selectionFill, MapSources.FIELDS_LINE_LAYER_ID)
    addLayerAbove(selectionLine, MapSources.SELECTION_FILL_LAYER_ID)
    // Rysowany kształt: to samo żółte wypełnienie co zaznaczenie, do tego linia i kropki w rogach.
    addSource(GeoJsonSource(MapSources.DRAFT_SOURCE_ID))
    addSource(GeoJsonSource(MapSources.DRAFT_POINTS_SOURCE_ID))
    val draftFill = FillLayer(MapSources.DRAFT_FILL_LAYER_ID, MapSources.DRAFT_SOURCE_ID)
        .withProperties(fillColor(SELECTION_FILL_COLOR), fillOpacity(SELECTION_FILL_OPACITY))
        .apply { setFilter(Expression.eq(Expression.geometryType(), Expression.literal("Polygon"))) }
    val draftLine = LineLayer(MapSources.DRAFT_LINE_LAYER_ID, MapSources.DRAFT_SOURCE_ID)
        .withProperties(lineColor(SELECTION_LINE_COLOR), lineWidth(SELECTION_LINE_WIDTH))
    val draftPoints = CircleLayer(MapSources.DRAFT_POINTS_LAYER_ID, MapSources.DRAFT_POINTS_SOURCE_ID)
        .withProperties(
            circleRadius(DRAFT_POINT_RADIUS),
            circleColor(Color.WHITE),
            circleStrokeColor(SELECTION_LINE_COLOR),
            circleStrokeWidth(DRAFT_POINT_STROKE),
        )
    addLayerAbove(draftFill, MapSources.SELECTION_LINE_LAYER_ID)
    addLayerAbove(draftLine, MapSources.DRAFT_FILL_LAYER_ID)
    addLayerAbove(draftPoints, MapSources.DRAFT_LINE_LAYER_ID)

    // Wjazdy: zielona kropka (zielony = „jedź tu") z podpisem pod spodem.
    addSource(GeoJsonSource(MapSources.ENTRIES_SOURCE_ID))
    val entryCircles = CircleLayer(MapSources.ENTRIES_CIRCLE_LAYER_ID, MapSources.ENTRIES_SOURCE_ID)
        .withProperties(
            circleRadius(ENTRY_RADIUS),
            circleColor(ENTRY_COLOR),
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(ENTRY_STROKE),
        )
    val entryLabels = SymbolLayer(MapSources.ENTRIES_LABEL_LAYER_ID, MapSources.ENTRIES_SOURCE_ID)
        .withProperties(
            textField(entryLabel),
            textFont(arrayOf(FIELD_LABEL_FONT)),
            textSize(ENTRY_LABEL_SIZE),
            textColor(Color.WHITE),
            textHaloColor(Color.BLACK),
            textHaloWidth(FIELD_LABEL_HALO),
            textOffset(arrayOf(0f, ENTRY_LABEL_OFFSET)),
            textAnchor(Property.TEXT_ANCHOR_TOP),
        )
    addLayerAbove(entryCircles, MapSources.DRAFT_POINTS_LAYER_ID)

    addSource(GeoJsonSource(MapSources.BASE_SOURCE_ID))
    val baseCircle = CircleLayer(MapSources.BASE_CIRCLE_LAYER_ID, MapSources.BASE_SOURCE_ID)
        .withProperties(
            circleRadius(BASE_RADIUS),
            circleColor(BASE_COLOR),
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(ENTRY_STROKE),
        )
    val baseLabels = SymbolLayer(MapSources.BASE_LABEL_LAYER_ID, MapSources.BASE_SOURCE_ID)
        .withProperties(
            textField(baseLabel),
            textFont(arrayOf(FIELD_LABEL_FONT)),
            textSize(BASE_LABEL_SIZE),
            textColor(Color.WHITE),
            textHaloColor(Color.BLACK),
            textHaloWidth(FIELD_LABEL_HALO),
            textOffset(arrayOf(0f, ENTRY_LABEL_OFFSET)),
            textAnchor(Property.TEXT_ANCHOR_TOP),
            // Napis bazy zawsze widoczny – nawet gdy nachodzi na nazwę pola.
            textAllowOverlap(true),
        )
    addLayerAbove(baseCircle, MapSources.ENTRIES_CIRCLE_LAYER_ID)

    // Napisy (nazwy pól, „Wjazd", „Baza") na samej górze – nad zaznaczeniem i etykietami mapy bazowej.
    addLayer(fieldsLabel)
    addLayer(entryLabels)
    addLayer(baseLabels)
}

private fun List<GeoPoint>.toDraftFeatureCollection(): FeatureCollection {
    val points = map { Point.fromLngLat(it.longitude, it.latitude) }
    val features = when {
        points.size >= 3 -> listOf(Feature.fromGeometry(Polygon.fromLngLats(listOf(points + points.first()))))
        points.size == 2 -> listOf(Feature.fromGeometry(LineString.fromLngLats(points)))
        else -> emptyList()
    }
    return FeatureCollection.fromFeatures(features)
}

private fun List<GeoPoint>.toPointsFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(map { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) })

private fun List<Field>.toFieldsFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(
    map { field ->
        Feature.fromGeometry(field.shape.toMultiPolygon()).apply {
            addStringProperty(FIELD_COLOR_PROPERTY, FieldColorUi.color(field.color).toHex())
        }
    },
)

private fun List<Field>.toLabelsFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(
    mapNotNull { field ->
        field.labelPoint()?.let { point ->
            Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                addStringProperty(FIELD_NAME_PROPERTY, field.name)
            }
        }
    },
)

/** Środek największej części pola (średnia wierzchołków obrysu) – wystarczy dla zwykłych pól. */
private fun Field.labelPoint(): GeoPoint? {
    val ring = shape.maxByOrNull { GeoArea.squareMeters(it) }?.outer?.takeIf { it.isNotEmpty() } ?: return null
    return GeoPoint(latitude = ring.map { it.latitude }.average(), longitude = ring.map { it.longitude }.average())
}

private fun androidx.compose.ui.graphics.Color.toHex(): String =
    String.format(Locale.ROOT, "#%06X", 0xFFFFFF and toArgb())

private fun List<GeoPolygon>.toFeatureCollection(): FeatureCollection {
    if (isEmpty()) return FeatureCollection.fromFeatures(emptyList<Feature>())
    return FeatureCollection.fromFeature(Feature.fromGeometry(toMultiPolygon()))
}

private fun List<GeoPolygon>.toMultiPolygon(): MultiPolygon = MultiPolygon.fromLngLats(
    map { polygon ->
        (listOf(polygon.outer) + polygon.holes).map { ring ->
            ring.map { Point.fromLngLat(it.longitude, it.latitude) }
        }
    },
)

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
    val photo = baseLayer == BaseLayer.PHOTO
    getLayer(MapSources.ORTHO_LAYER_ID)
        ?.setProperties(visibility(if (photo) Property.VISIBLE else Property.NONE))
    getLayer(MapSources.PARCELS_LAYER_ID)?.setProperties(
        visibility(if (showParcels) Property.VISIBLE else Property.NONE),
        // KIEG rysuje cienkie niebieskie linie – na ciemnym zdjęciu (las, pole) giną. Na zdjęciu
        // przebarwiamy je na białe (bez nasycenia, pełna jasność); na zwykłej mapie zostają niebieskie.
        rasterSaturation(if (photo) -1f else 0f),
        rasterBrightnessMin(if (photo) 1f else 0f),
    )
}

/** Bez tego śledzenie pozycji od razu przeciągnęłoby mapę z powrotem do użytkownika. */
private fun MapLibreMap.stopFollowingMe() {
    if (locationComponent.isLocationComponentActivated) {
        locationComponent.cameraMode = CameraMode.NONE
    }
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
