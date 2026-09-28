package pl.farmtracker.feature.roles.driver

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.DriverState
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.RoleScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R
import pl.farmtracker.feature.roles.common.OpenMapButton

private data class DriverAction(val state: DriverState, @StringRes val label: Int, val icon: ImageVector)

/** Przyciski w kolejności kursu: pole → załadunek → baza → rozładunek. */
private val DriverActions = listOf(
    DriverAction(DriverState.TO_FIELD, R.string.roles_driver_to_field, Icons.Filled.LocationOn),
    DriverAction(DriverState.LOADING, R.string.roles_driver_loading, Icons.Filled.Download),
    DriverAction(DriverState.TO_BASE, R.string.roles_driver_to_base, Icons.Filled.LocalShipping),
    DriverAction(DriverState.UNLOADING, R.string.roles_driver_unloading, Icons.Filled.Upload),
)

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
        onSelectState = viewModel::selectState,
        onUndo = viewModel::undo,
        onOpenMap = onOpenMap,
        onSwitchRole = onSwitchRole,
        modifier = modifier,
    )
}

@Composable
internal fun DriverContent(
    uiState: DriverUiState,
    onSelectState: (DriverState) -> Unit,
    onUndo: () -> Unit,
    onOpenMap: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RoleScaffold(role = Role.DRIVER, onSwitchRole = onSwitchRole, modifier = modifier) {
        val current = DriverActions.firstOrNull { it.state == uiState.state }
        if (current == null) {
            StatusPill(text = stringResource(R.string.roles_driver_pick_status), icon = Icons.Filled.Info)
        } else {
            StatusPill(
                text = stringResource(R.string.roles_driver_current_status, stringResource(current.label)),
                icon = current.icon,
                tone = Tone.Go,
            )
        }

        DriverActions.forEach { action ->
            val isSelected = action.state == uiState.state
            BigActionButton(
                text = stringResource(action.label),
                icon = action.icon,
                onClick = { onSelectState(action.state) },
                tone = if (isSelected) Tone.Go else Tone.Neutral,
                selected = isSelected,
            )
        }

        if (uiState.canUndo) {
            TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.roles_undo), style = MaterialTheme.typography.labelLarge)
            }
        }

        OpenMapButton(onClick = onOpenMap)
    }
}

@PreviewLightDark
@Composable
private fun DriverContentPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        DriverContent(
            uiState = DriverUiState(state = DriverState.LOADING, previousState = DriverState.TO_FIELD),
            onSelectState = {},
            onUndo = {},
            onOpenMap = {},
            onSwitchRole = {},
        )
    }
}
