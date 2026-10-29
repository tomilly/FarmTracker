package pl.farmtracker.feature.roles.harvester

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.MapStatusCard
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.MyPosition
import pl.farmtracker.feature.roles.common.RoleMap
import pl.farmtracker.feature.roles.common.StartWorkButton
import pl.farmtracker.feature.roles.common.StopWorkButton
import pl.farmtracker.feature.roles.common.crewLines
import pl.farmtracker.feature.roles.common.icon
import pl.farmtracker.feature.roles.common.title

/**
 * Sieczkarnia: mapa na cały ekran, w pracy jedzie za nią i pokazuje nadjeżdżające przyczepy. W rogu mała karta:
 * na którym polu jestem, kto ładuje, kto jedzie i jak daleko. W pracy żadnego dużego przycisku – sieczkarnia tylko
 * jedzie; mały „Kończę" w lewym dolnym rogu.
 */
@Composable
fun HarvesterScreen(
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** Admin, który sam pracuje – „Admin" wraca do panelu (gdy nie pracuje; w pracy najpierw „Kończę"). */
    onBackToAdmin: (() -> Unit)? = null,
    viewModel: HarvesterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overlays by viewModel.overlays.collectAsStateWithLifecycle()
    RoleMap(
        chrome = viewModel.chrome,
        overlays = overlays,
        onSwitchRole = onSwitchRole,
        onBackToAdmin = onBackToAdmin?.takeUnless { uiState.isWorking },
        modifier = modifier,
        status = { HarvesterStatusCard(uiState) },
        bottom = { HarvesterActions(uiState, onStartWork = viewModel::startWork, onStopWork = viewModel::stopWork) },
    )
}

@Composable
internal fun HarvesterStatusCard(uiState: HarvesterUiState, modifier: Modifier = Modifier) {
    MapStatusCard(
        title = if (uiState.isWorking) uiState.position.title() else stringResource(RoleUi.labelRes(Role.HARVESTER)),
        icon = if (uiState.isWorking) uiState.position.icon else RoleUi.icon(Role.HARVESTER),
        live = uiState.isWorking,
        liveText = stringResource(if (uiState.isWorking) R.string.roles_live else R.string.roles_not_live),
        details = crewLines(Role.DRIVER, uiState.drivers),
        modifier = modifier,
    )
}

@Composable
internal fun ColumnScope.HarvesterActions(uiState: HarvesterUiState, onStartWork: () -> Unit, onStopWork: () -> Unit) {
    if (uiState.isWorking) {
        StopWorkButton(onStopWork = onStopWork)
    } else {
        StartWorkButton(onStartWork = onStartWork)
    }
}

@PreviewLightDark
@Composable
private fun HarvesterWorkingPreview() {
    val uiState = HarvesterUiState(
        isWorking = true,
        position = MyPosition.OnField("Za lasem"),
        drivers = listOf(
            Coworker("Marek", Role.DRIVER, fieldName = "Za lasem", isStale = false, trip = Trip.LOADING),
            Coworker("Janek", Role.DRIVER, fieldName = null, isStale = false, trip = Trip.TO_FIELD, distanceMeters = 1_200.0),
        ),
    )
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HarvesterStatusCard(uiState)
            HarvesterActions(uiState, onStartWork = {}, onStopWork = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun HarvesterIdlePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HarvesterStatusCard(HarvesterUiState())
            HarvesterActions(HarvesterUiState(), onStartWork = {}, onStopWork = {})
        }
    }
}
