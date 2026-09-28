package pl.farmtracker.feature.roles.base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
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
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R
import pl.farmtracker.feature.roles.common.OpenMapButton

@Composable
fun BaseScreen(
    onOpenMap: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: BaseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BaseContent(uiState = uiState, onOpenMap = onOpenMap, onSwitchRole = onSwitchRole, modifier = modifier)
}

@Composable
internal fun BaseContent(
    uiState: BaseUiState,
    onOpenMap: () -> Unit,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RoleScaffold(role = Role.BASE, onSwitchRole = onSwitchRole, modifier = modifier) {
        Text(stringResource(R.string.roles_base_incoming_title), style = MaterialTheme.typography.titleMedium)
        if (uiState.incomingDrivers.isEmpty()) {
            StatusPill(text = stringResource(R.string.roles_base_nobody_incoming), icon = Icons.Filled.Info)
        } else {
            uiState.incomingDrivers.forEach { driver ->
                StatusPill(text = driver, icon = Icons.Filled.LocalShipping, tone = Tone.Go)
            }
        }

        OpenMapButton(onClick = onOpenMap)
    }
}

@PreviewLightDark
@Composable
private fun BaseContentPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        BaseContent(
            uiState = BaseUiState(incomingDrivers = listOf("Marek", "Janek")),
            onOpenMap = {},
            onSwitchRole = {},
        )
    }
}
