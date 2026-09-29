package pl.farmtracker.core.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.FarmTrackerTopBar
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private val OverlayPadding = 12.dp
private val OverlaySpacing = 12.dp

/**
 * Wspólny ekran z mapą: górny pasek z „Wróć", mapa na całą resztę ekranu, a na niej: warstwy u góry,
 * „Gdzie jestem" i treść ekranu ([panel]) na dole. Kamera wie, ile mapy zasłaniają przyciski,
 * więc pokazywane pola i działki trafiają w odkrytą część.
 */
@Composable
fun MapScaffold(
    title: String,
    icon: ImageVector,
    onBack: () -> Unit,
    chrome: MapChromeController,
    onMapTap: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    overlays: MapOverlays = MapOverlays(),
    showParcelsToggle: Boolean = true,
    panel: @Composable ColumnScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val chromeState by chrome.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> chrome.onLocationPermissionResult(granted = result.values.any { it }) }

    LaunchedEffect(Unit) { chrome.onStart(context.hasLocationPermission()) }
    LifecycleResumeEffect(Unit) {
        chrome.onLocationPermissionRechecked(context.hasLocationPermission())
        onPauseOrDispose { }
    }
    LaunchedEffect(chromeState.askForLocation) {
        if (chromeState.askForLocation) {
            chrome.onLocationPermissionAsked()
            permissionLauncher.launch(LocationPermissions)
        }
    }

    // Ile pikseli mapy zasłaniają przyciski u góry i na dole (razem z „Gdzie jestem").
    var coveredTop by remember { mutableIntStateOf(0) }
    var coveredBottom by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = { FarmTrackerTopBar(title = title, icon = icon, onBack = onBack) },
    ) { innerPadding ->
        // Mapa sięga do dołu ekranu (pod pasek nawigacji systemu); przyciski go omijają.
        Box(Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            FarmMap(
                chrome = chromeState,
                overlays = overlays,
                covered = MapCovered(top = coveredTop, bottom = coveredBottom),
                onCameraIdle = chrome::onCameraIdle,
                onMapTap = onMapTap,
                modifier = Modifier.fillMaxSize(),
            )
            MapLayerBar(
                chrome = chromeState,
                onSelectBaseLayer = chrome::selectBaseLayer,
                onToggleParcels = chrome::toggleParcels,
                showParcelsToggle = showParcelsToggle,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { coveredTop = it.height }
                    .padding(OverlayPadding),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { coveredBottom = it.height }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(OverlayPadding),
                verticalArrangement = Arrangement.spacedBy(OverlaySpacing),
            ) {
                WhereAmIButton(onClick = chrome::onWhereAmIClicked, modifier = Modifier.align(Alignment.End))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OverlaySpacing),
                ) {
                    MapHints(chrome = chromeState, onOpenSettings = { context.openAppSettings() })
                    panel()
                }
            }
        }
    }
}

/** Komunikaty mapy (brak zgody na lokalizację, za daleko na działki) – nad treścią ekranu. */
@Composable
private fun ColumnScope.MapHints(chrome: MapChromeState, onOpenSettings: () -> Unit) {
    if (chrome.locationAccess == LocationAccess.DENIED) {
        StatusPill(
            text = stringResource(R.string.core_map_location_denied),
            icon = Icons.Filled.LocationOff,
            tone = Tone.Warning,
        )
        BigActionButton(
            text = stringResource(R.string.core_map_open_settings),
            icon = Icons.Filled.Settings,
            onClick = onOpenSettings,
            tone = Tone.Neutral,
        )
    }
    if (chrome.showParcelsZoomHint) {
        StatusPill(
            text = stringResource(R.string.core_map_parcels_zoom_hint),
            icon = Icons.Filled.ZoomIn,
            tone = Tone.Warning,
        )
    }
}

/** Warstwy w jednym rzędzie u góry mapy: „Mapa" / „Zdjęcie" i włącznik „Działki". */
@Composable
internal fun MapLayerBar(
    chrome: MapChromeState,
    onSelectBaseLayer: (BaseLayer) -> Unit,
    onToggleParcels: () -> Unit,
    modifier: Modifier = Modifier,
    showParcelsToggle: Boolean = true,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MapChip(
            text = stringResource(R.string.core_map_layer_map),
            icon = Icons.Filled.Map,
            isSelected = chrome.baseLayer == BaseLayer.MAP,
            onClick = { onSelectBaseLayer(BaseLayer.MAP) },
            modifier = Modifier.weight(1f),
        )
        MapChip(
            text = stringResource(R.string.core_map_layer_photo),
            icon = Icons.Filled.Satellite,
            isSelected = chrome.baseLayer == BaseLayer.PHOTO,
            onClick = { onSelectBaseLayer(BaseLayer.PHOTO) },
            modifier = Modifier.weight(1f),
        )
        if (showParcelsToggle) {
            MapChip(
                text = stringResource(R.string.core_map_parcels),
                icon = if (chrome.showParcels) Icons.Filled.CheckCircle else Icons.Filled.GridOn,
                isSelected = chrome.showParcels,
                onClick = onToggleParcels,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Przycisk na mapie: nieprzezroczysty i z cieniem – czytelny na zdjęciu lotniczym.
 * Włączony = wypełniony kolorem, wyłączony = biały z obrysem.
 */
@Composable
private fun MapChip(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget).semantics { selected = isSelected },
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) colors.primary else colors.surface,
        contentColor = if (isSelected) colors.onPrimary else colors.onSurface,
        border = if (isSelected) null else BorderStroke(2.dp, colors.outline),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Konwencja z Google Maps: przycisk „moja pozycja" w prawym dolnym rogu mapy – ale z podpisem. */
@Composable
private fun WhereAmIButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(Icons.Filled.MyLocation, contentDescription = null, Modifier.size(28.dp)) },
        text = { Text(stringResource(R.string.core_map_where_am_i), style = MaterialTheme.typography.labelLarge) },
    )
}

private fun Context.hasLocationPermission(): Boolean = LocationPermissions.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@PreviewLightDark
@Composable
private fun MapLayerBarPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Box(Modifier.padding(OverlayPadding)) {
            MapLayerBar(
                chrome = MapChromeState(baseLayer = BaseLayer.PHOTO, showParcels = true, zoom = 12.0),
                onSelectBaseLayer = {},
                onToggleParcels = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MapHintsPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Column(Modifier.padding(OverlayPadding), verticalArrangement = Arrangement.spacedBy(OverlaySpacing)) {
            MapHints(
                chrome = MapChromeState(locationAccess = LocationAccess.DENIED, showParcels = true, zoom = 10.0),
                onOpenSettings = {},
            )
        }
    }
}
