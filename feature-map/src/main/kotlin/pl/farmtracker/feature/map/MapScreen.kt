package pl.farmtracker.feature.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun MapScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> viewModel.onLocationPermissionResult(granted = result.values.any { it }) }

    LaunchedEffect(Unit) { viewModel.onStart(context.hasLocationPermission()) }
    LifecycleResumeEffect(Unit) {
        viewModel.onLocationPermissionRechecked(context.hasLocationPermission())
        onPauseOrDispose { }
    }
    LaunchedEffect(uiState.askForLocation) {
        if (uiState.askForLocation) {
            viewModel.onLocationPermissionAsked()
            permissionLauncher.launch(LocationPermissions)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FarmTrackerTopBar(title = stringResource(R.string.map_title), icon = Icons.Filled.Map, onBack = onBack)
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                FarmMap(
                    baseLayer = uiState.baseLayer,
                    showParcels = uiState.showParcels,
                    locationEnabled = uiState.locationAccess == LocationAccess.GRANTED,
                    centerOnMeRequest = uiState.centerOnMeRequest,
                    onZoomChanged = viewModel::onZoomChanged,
                    modifier = Modifier.fillMaxSize(),
                )
                WhereAmIButton(
                    onClick = viewModel::onWhereAmIClicked,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(FarmTrackerDimens.ScreenPadding),
                )
            }
            MapControls(
                uiState = uiState,
                onSelectBaseLayer = viewModel::selectBaseLayer,
                onToggleParcels = viewModel::toggleParcels,
                onOpenSettings = { context.openAppSettings() },
            )
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
        text = { Text(stringResource(R.string.map_where_am_i), style = MaterialTheme.typography.labelLarge) },
    )
}

@Composable
internal fun MapControls(
    uiState: MapUiState,
    onSelectBaseLayer: (BaseLayer) -> Unit,
    onToggleParcels: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.padding(FarmTrackerDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.locationAccess == LocationAccess.DENIED) {
                StatusPill(
                    text = stringResource(R.string.map_location_denied),
                    icon = Icons.Filled.LocationOff,
                    tone = Tone.Warning,
                )
                BigActionButton(
                    text = stringResource(R.string.map_open_settings),
                    icon = Icons.Filled.Settings,
                    onClick = onOpenSettings,
                    tone = Tone.Neutral,
                )
            }
            if (uiState.showParcelsZoomHint) {
                StatusPill(
                    text = stringResource(R.string.map_parcels_zoom_hint),
                    icon = Icons.Filled.ZoomIn,
                    tone = Tone.Warning,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LayerButton(
                    text = stringResource(R.string.map_layer_map),
                    icon = Icons.Filled.Map,
                    isSelected = uiState.baseLayer == BaseLayer.MAP,
                    onClick = { onSelectBaseLayer(BaseLayer.MAP) },
                    modifier = Modifier.weight(1f),
                )
                LayerButton(
                    text = stringResource(R.string.map_layer_photo),
                    icon = Icons.Filled.Satellite,
                    isSelected = uiState.baseLayer == BaseLayer.PHOTO,
                    onClick = { onSelectBaseLayer(BaseLayer.PHOTO) },
                    modifier = Modifier.weight(1f),
                )
            }
            BigActionButton(
                text = stringResource(R.string.map_parcels),
                icon = Icons.Filled.GridOn,
                onClick = onToggleParcels,
                tone = if (uiState.showParcels) Tone.Primary else Tone.Neutral,
                selected = uiState.showParcels,
            )
        }
    }
}

/** Przycisk wyboru warstwy: wybrana = wypełniona, pozostałe = obrys (bez „ptaszka" – za wąsko). */
@Composable
private fun LayerButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BigActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        tone = if (isSelected) Tone.Primary else Tone.Neutral,
        modifier = modifier.semantics { selected = isSelected },
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
private fun MapControlsPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        MapControls(
            uiState = MapUiState(baseLayer = BaseLayer.PHOTO, showParcels = true, zoom = 14.0),
            onSelectBaseLayer = {},
            onToggleParcels = {},
            onOpenSettings = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MapControlsDeniedPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        MapControls(
            uiState = MapUiState(locationAccess = LocationAccess.DENIED),
            onSelectBaseLayer = {},
            onToggleParcels = {},
            onOpenSettings = {},
        )
    }
}
