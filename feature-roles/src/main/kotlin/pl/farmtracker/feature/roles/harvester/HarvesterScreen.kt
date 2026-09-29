package pl.farmtracker.feature.roles.harvester

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
fun HarvesterScreen(
    onOpenMap: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: HarvesterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HarvesterContent(
        uiState = uiState,
        onStartWork = viewModel::startWork,
        onStopWork = viewModel::stopWork,
        onOpenMap = onOpenMap,
        onSwitchRole = onSwitchRole,
        modifier = modifier,
    )
}

@Composable
internal fun HarvesterContent(
    uiState: HarvesterUiState,
    onStartWork: () -> Unit,
    onStopWork: () -> Unit,
    onOpenMap: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RoleScaffold(
        role = Role.HARVESTER,
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

        OpenMapButton(onClick = onOpenMap)

        Text(stringResource(R.string.roles_drivers_title), style = MaterialTheme.typography.titleMedium)
        CoworkerPills(role = Role.DRIVER, coworkers = uiState.drivers)
    }
}

@PreviewLightDark
@Composable
private fun HarvesterWorkingPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        HarvesterContent(
            uiState = HarvesterUiState(
                isWorking = true,
                position = MyPosition.OnField("Za lasem"),
                drivers = listOf(Coworker("Marek", Role.DRIVER, fieldName = null, isStale = false)),
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
private fun HarvesterIdlePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        HarvesterContent(
            uiState = HarvesterUiState(),
            onStartWork = {},
            onStopWork = {},
            onOpenMap = {},
            onSwitchRole = null,
        )
    }
}
