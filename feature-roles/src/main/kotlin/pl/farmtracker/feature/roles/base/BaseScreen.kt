package pl.farmtracker.feature.roles.base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import pl.farmtracker.feature.roles.common.CoworkerRow
import pl.farmtracker.feature.roles.common.RoleMap
import pl.farmtracker.feature.roles.common.crewLines

/** Baza nic nie klika – patrzy, kto przyjeżdża: mapa całego zbioru, w rogu sieczkarnia, pod mapą lista „Do bazy". */
@Composable
fun BaseScreen(
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: BaseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overlays by viewModel.overlays.collectAsStateWithLifecycle()
    RoleMap(
        chrome = viewModel.chrome,
        overlays = overlays,
        onSwitchRole = onSwitchRole,
        modifier = modifier,
        status = { BaseStatusCard(uiState) },
        sheet = { IncomingDrivers(uiState.drivers) },
    )
}

@Composable
internal fun BaseStatusCard(uiState: BaseUiState, modifier: Modifier = Modifier) {
    MapStatusCard(
        title = stringResource(RoleUi.labelRes(Role.BASE)),
        icon = RoleUi.icon(Role.BASE),
        details = crewLines(Role.HARVESTER, uiState.harvesters),
        modifier = modifier,
    )
}

/** „Do bazy – najbliżej na górze", kierowca w wierszu; nikt nie jedzie – „Kierowca nie pracuje". */
@Composable
internal fun ColumnScope.IncomingDrivers(drivers: List<Coworker>) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            stringResource(R.string.roles_base_incoming_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(R.string.roles_base_incoming_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (drivers.isEmpty()) {
        Text(crewLines(Role.DRIVER, drivers).first(), style = MaterialTheme.typography.bodyLarge)
    }
    drivers.forEach { CoworkerRow(it) }
}

@PreviewLightDark
@Composable
private fun BasePreview() {
    val uiState = BaseUiState(
        harvesters = listOf(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false)),
        drivers = listOf(
            Coworker("Janek", Role.DRIVER, fieldName = null, isStale = false, trip = Trip.TO_BASE, distanceMeters = 800.0),
            Coworker("Marek", Role.DRIVER, fieldName = "Za lasem", isStale = false, trip = Trip.LOADING),
            Coworker("Wojtek", Role.DRIVER, fieldName = null, isStale = true),
        ),
    )
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BaseStatusCard(uiState)
                IncomingDrivers(uiState.drivers)
            }
        }
    }
}
