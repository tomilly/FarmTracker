package pl.farmtracker.feature.roles.base

import androidx.compose.foundation.isSystemInDarkTheme
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
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CoworkerPills
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
        Text(stringResource(R.string.roles_harvesters_title), style = MaterialTheme.typography.titleMedium)
        CoworkerPills(role = Role.HARVESTER, coworkers = uiState.harvesters)

        Text(stringResource(R.string.roles_drivers_title), style = MaterialTheme.typography.titleMedium)
        CoworkerPills(role = Role.DRIVER, coworkers = uiState.drivers)

        OpenMapButton(onClick = onOpenMap)
    }
}

@PreviewLightDark
@Composable
private fun BaseContentPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        BaseContent(
            uiState = BaseUiState(
                harvesters = listOf(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false)),
                drivers = listOf(
                    Coworker("Marek", Role.DRIVER, fieldName = null, isStale = false),
                    Coworker("Janek", Role.DRIVER, fieldName = "Za lasem", isStale = false),
                ),
            ),
            onOpenMap = {},
            onSwitchRole = {},
        )
    }
}
