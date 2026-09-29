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
import pl.farmtracker.core.ui.component.RoleScaffold
import pl.farmtracker.core.ui.component.StatusPill
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
    DriverContent(
        uiState = uiState,
        onStartWork = viewModel::startWork,
        onStopWork = viewModel::stopWork,
        onOpenMap = onOpenMap,
        onSwitchRole = onSwitchRole,
        modifier = modifier,
    )
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
