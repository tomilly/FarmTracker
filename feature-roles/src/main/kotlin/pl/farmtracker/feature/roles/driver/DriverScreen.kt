package pl.farmtracker.feature.roles.driver

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.DriveTo
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.MapStatusCard
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.DriveToButton
import pl.farmtracker.feature.roles.common.MyPosition
import pl.farmtracker.feature.roles.common.RoleMap
import pl.farmtracker.feature.roles.common.StartWorkButton
import pl.farmtracker.feature.roles.common.StopWorkButton
import pl.farmtracker.feature.roles.common.crewLines
import pl.farmtracker.feature.roles.common.icon
import pl.farmtracker.feature.roles.common.title

/**
 * Kierowca: mapa na cały ekran, w pracy jedzie za nim (niebieska kropka „ja") i trzyma na ekranie sieczkarnię.
 * W rogu mała karta: co robię („Ładujesz") i gdzie sieczkarnia. Na dole jeden duży przycisk – przed pracą
 * „Zaczynam pracę", w pracy „Prowadź do…" (nawigacja) – i mały „Kończę".
 */
@Composable
fun DriverScreen(
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** Admin, który sam pracuje – „Admin" wraca do panelu (gdy nie pracuje; w pracy najpierw „Kończę"). */
    onBackToAdmin: (() -> Unit)? = null,
    viewModel: DriverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overlays by viewModel.overlays.collectAsStateWithLifecycle()
    RoleMap(
        chrome = viewModel.chrome,
        overlays = overlays,
        onSwitchRole = onSwitchRole,
        onBackToAdmin = onBackToAdmin?.takeUnless { uiState.isWorking },
        modifier = modifier,
        status = { DriverStatusCard(uiState) },
        bottom = { DriverActions(uiState, onStartWork = viewModel::startWork, onStopWork = viewModel::stopWork) },
    )
}

/** Co robię jednym słowem, pod spodem drobniej – gdzie sieczkarnia. Przed pracą: kim jestem i że nikt mnie nie widzi. */
@Composable
internal fun DriverStatusCard(uiState: DriverUiState, modifier: Modifier = Modifier) {
    MapStatusCard(
        title = if (uiState.isWorking) uiState.position.title() else stringResource(RoleUi.labelRes(Role.DRIVER)),
        icon = if (uiState.isWorking) uiState.position.icon else RoleUi.icon(Role.DRIVER),
        live = uiState.isWorking,
        liveText = stringResource(if (uiState.isWorking) R.string.roles_live else R.string.roles_not_live),
        details = crewLines(Role.HARVESTER, uiState.harvesters),
        modifier = modifier,
    )
}

/** W pracy: mały „Kończę" po lewej, „Prowadź do…" (gdy jest dokąd) obok – pod kciukiem. */
@Composable
internal fun ColumnScope.DriverActions(uiState: DriverUiState, onStartWork: () -> Unit, onStopWork: () -> Unit) {
    if (!uiState.isWorking) {
        StartWorkButton(onStartWork = onStartWork)
        return
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StopWorkButton(onStopWork = onStopWork, modifier = Modifier.fillMaxHeight())
        uiState.driveTo?.let { target ->
            DriveToButton(driveTo = target, distanceMeters = uiState.driveToMeters, modifier = Modifier.weight(1f))
        }
    }
}

@PreviewLightDark
@Composable
private fun DriverWorkingPreview() {
    val uiState = DriverUiState(
        isWorking = true,
        position = MyPosition.Loading,
        harvesters = listOf(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false)),
        driveTo = DriveTo.ToBase(GeoPoint(50.0, 17.0)),
        driveToMeters = 4_180.0,
    )
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DriverStatusCard(uiState)
            DriverActions(uiState, onStartWork = {}, onStopWork = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun DriverIdlePreview() {
    val uiState = DriverUiState(harvesters = listOf(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false)))
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DriverStatusCard(uiState)
            DriverActions(uiState, onStartWork = {}, onStopWork = {})
        }
    }
}
