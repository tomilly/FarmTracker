package pl.farmtracker.feature.roles.driver

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.core.map.MapScaffold
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.RoleScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.SwitchRoleButton
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CoworkerPills
import pl.farmtracker.feature.roles.common.MyPosition
import pl.farmtracker.feature.roles.common.MyPositionPills
import pl.farmtracker.feature.roles.common.OpenMapButton
import pl.farmtracker.feature.roles.common.StartWorkButton
import pl.farmtracker.feature.roles.common.StopWorkButton

@Composable
fun DriverScreen(
    onOpenMap: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: DriverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.isWorking) {
        val overlays by viewModel.overlays.collectAsStateWithLifecycle()
        DriverWorkMap(
            uiState = uiState,
            chrome = viewModel.chrome,
            overlays = overlays,
            onStopWork = viewModel::stopWork,
            onSwitchRole = onSwitchRole,
            modifier = modifier,
        )
    } else {
        DriverContent(
            uiState = uiState,
            onStartWork = viewModel::startWork,
            onStopWork = viewModel::stopWork,
            onOpenMap = onOpenMap,
            onSwitchRole = onSwitchRole,
            modifier = modifier,
        )
    }
}

/**
 * Kierowca w pracy: mapa na cały ekran, jedzie za nim (niebieska kropka „ja"), z sieczkarnią i innymi.
 * Na dole: gdzie jestem, gdzie sieczkarnia i mały „Kończę pracę".
 */
@Composable
private fun DriverWorkMap(
    uiState: DriverUiState,
    chrome: MapChromeController,
    overlays: MapOverlays,
    onStopWork: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MapScaffold(
        title = stringResource(RoleUi.labelRes(Role.DRIVER)),
        icon = RoleUi.icon(Role.DRIVER),
        onBack = null,
        chrome = chrome,
        onMapTap = {},
        modifier = modifier,
        overlays = overlays,
        showParcelsToggle = false,
        actions = { if (onSwitchRole != null) SwitchRoleButton(onClick = onSwitchRole) },
    ) {
        MyPositionPills(uiState.position)
        CoworkerPills(role = Role.HARVESTER, coworkers = uiState.harvesters)
        StopWorkButton(onStopWork = onStopWork)
    }
}

/** Ekran kierowcy – celowo prosty: kierowca prowadzi, więc tylko „Zaczynam pracę", gdzie sieczkarnia i mapa. */
@Composable
internal fun DriverContent(
    uiState: DriverUiState,
    onStartWork: () -> Unit,
    onStopWork: () -> Unit,
    onOpenMap: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // W pracy kierowca widzi mapę ([DriverWorkMap]); tu – przed pracą (i w podglądach).
    RoleScaffold(
        role = Role.DRIVER,
        onSwitchRole = onSwitchRole,
        modifier = modifier,
        bottomAction = if (uiState.isWorking) {
            { StopWorkButton(onStopWork = onStopWork) }
        } else {
            null
        },
    ) {
        if (uiState.isWorking) {
            MyPositionPills(uiState.position)
        } else {
            StatusPill(text = stringResource(R.string.roles_not_working), icon = Icons.Filled.Info)
            StartWorkButton(onStartWork = onStartWork)
        }

        Text(stringResource(R.string.roles_harvesters_title), style = MaterialTheme.typography.titleMedium)
        CoworkerPills(role = Role.HARVESTER, coworkers = uiState.harvesters)

        OpenMapButton(onClick = onOpenMap)
    }
}

@PreviewLightDark
@Composable
private fun DriverWorkingPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        DriverContent(
            uiState = DriverUiState(
                isWorking = true,
                position = MyPosition.OffField,
                harvesters = listOf(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false)),
            ),
            onStartWork = {},
            onStopWork = {},
            onOpenMap = {},
            onSwitchRole = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DriverIdlePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        DriverContent(
            uiState = DriverUiState(),
            onStartWork = {},
            onStopWork = {},
            onOpenMap = {},
            onSwitchRole = null,
        )
    }
}
