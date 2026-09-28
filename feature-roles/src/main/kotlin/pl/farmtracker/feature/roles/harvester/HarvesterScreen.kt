package pl.farmtracker.feature.roles.harvester

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.RoleScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R

@Composable
fun HarvesterScreen(
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: HarvesterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HarvesterContent(
        uiState = uiState,
        onStartWork = viewModel::startWork,
        onStopWork = viewModel::stopWork,
        onStartMoving = viewModel::startMoving,
        onArrived = viewModel::arrivedAtField,
        onSwitchRole = onSwitchRole,
        modifier = modifier,
    )
}

@Composable
internal fun HarvesterContent(
    uiState: HarvesterUiState,
    onStartWork: () -> Unit,
    onStopWork: () -> Unit,
    onStartMoving: () -> Unit,
    onArrived: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RoleScaffold(role = Role.HARVESTER, onSwitchRole = onSwitchRole, modifier = modifier) {
        when {
            !uiState.isWorking -> StatusPill(
                text = stringResource(R.string.roles_harvester_not_working),
                icon = Icons.Filled.Info,
            )
            uiState.isMoving -> StatusPill(
                text = stringResource(R.string.roles_harvester_moving),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                tone = Tone.Warning,
            )
            else -> StatusPill(
                text = stringResource(R.string.roles_harvester_working),
                icon = Icons.Filled.Agriculture,
                tone = Tone.Go,
            )
        }

        if (!uiState.isWorking) {
            BigActionButton(
                text = stringResource(R.string.roles_harvester_start_work),
                icon = Icons.Filled.PlayArrow,
                onClick = onStartWork,
                tone = Tone.Go,
            )
        } else {
            if (uiState.isMoving) {
                BigActionButton(
                    text = stringResource(R.string.roles_harvester_arrived),
                    icon = Icons.Filled.LocationOn,
                    onClick = onArrived,
                    tone = Tone.Go,
                )
            } else {
                BigActionButton(
                    text = stringResource(R.string.roles_harvester_start_moving),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onStartMoving,
                    tone = Tone.Warning,
                )
            }
            BigActionButton(
                text = stringResource(R.string.roles_harvester_stop_work),
                icon = Icons.Filled.Stop,
                onClick = onStopWork,
                tone = Tone.Stop,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HarvesterWorkingPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        HarvesterContent(
            uiState = HarvesterUiState(isWorking = true),
            onStartWork = {},
            onStopWork = {},
            onStartMoving = {},
            onArrived = {},
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
            onStartMoving = {},
            onArrived = {},
            onSwitchRole = null,
        )
    }
}
