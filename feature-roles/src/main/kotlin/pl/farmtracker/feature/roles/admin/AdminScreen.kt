package pl.farmtracker.feature.roles.admin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.RoleScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CoworkerPills
import pl.farmtracker.feature.roles.common.OpenMapButton

/**
 * Admin: na górze podgląd pracy – kto gdzie jest i co robi (ładuje, wraca do bazy, stoi od…), kto nie pracuje,
 * „Mapa" ze wszystkimi polami. Pod spodem płaskie menu zbioru (BRIEF §4): „Pola" i „Baza" – miejsca zbioru;
 * „Ludzie" – zaproszenia i role (feature-team); „Ustawienia" – konto i wylogowanie (feature-auth).
 */
@Composable
fun AdminScreen(
    onOpenMap: () -> Unit,
    onOpenFields: () -> Unit,
    onOpenBase: () -> Unit,
    /** `null` – „Ludzie" jeszcze niedostępne (bez wspólnego zbioru): pokazujemy „wkrótce". */
    onOpenPeople: (() -> Unit)?,
    /** `null` – bez wspólnego zbioru nie ma konta do pokazania ani wylogowania: „wkrótce". */
    onOpenSettings: (() -> Unit)?,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AdminContent(
        uiState = uiState,
        onOpenMap = onOpenMap,
        onOpenFields = onOpenFields,
        onOpenBase = onOpenBase,
        onOpenPeople = onOpenPeople,
        onOpenSettings = onOpenSettings,
        onSwitchRole = onSwitchRole,
        modifier = modifier,
    )
}

@Composable
internal fun AdminContent(
    uiState: AdminUiState,
    onOpenMap: () -> Unit,
    onOpenFields: () -> Unit,
    onOpenBase: () -> Unit,
    onOpenPeople: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.roles_admin_coming_soon)
    val showComingSoon: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }

    RoleScaffold(
        role = Role.ADMIN,
        onSwitchRole = onSwitchRole,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.roles_admin_overview_title), style = MaterialTheme.typography.titleMedium)
        CoworkerPills(role = Role.HARVESTER, coworkers = uiState.harvesters)
        CoworkerPills(role = Role.DRIVER, coworkers = uiState.drivers)
        if (uiState.notWorking.isNotEmpty()) {
            StatusPill(
                text = stringResource(R.string.roles_admin_not_working, uiState.notWorking.joinToString(", ")),
                icon = Icons.Filled.PersonOff,
            )
        }
        OpenMapButton(onClick = onOpenMap)

        Text(stringResource(R.string.roles_admin_manage_title), style = MaterialTheme.typography.titleMedium)
        BigActionButton(
            text = stringResource(R.string.roles_admin_fields),
            icon = Icons.Filled.Map,
            onClick = onOpenFields,
        )
        BigActionButton(
            text = stringResource(R.string.roles_admin_base),
            icon = Icons.Filled.Warehouse,
            onClick = onOpenBase,
        )
        BigActionButton(
            text = stringResource(R.string.roles_admin_people),
            icon = Icons.Filled.Groups,
            onClick = onOpenPeople ?: showComingSoon,
        )
        BigActionButton(
            text = stringResource(R.string.roles_admin_settings),
            icon = Icons.Filled.Settings,
            onClick = onOpenSettings ?: showComingSoon,
        )
    }
}

@PreviewLightDark
@Composable
private fun AdminScreenPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        AdminContent(
            uiState = AdminUiState(
                harvesters = listOf(
                    Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false, standingMinutes = 7),
                ),
                drivers = listOf(
                    Coworker("Marek", Role.DRIVER, fieldName = "Za lasem", isStale = false, trip = Trip.LOADING),
                    Coworker("Janek", Role.DRIVER, fieldName = null, isStale = false, trip = Trip.TO_BASE),
                    Coworker("Wojtek", Role.DRIVER, fieldName = null, isStale = false, trip = Trip.AT_BASE, standingMinutes = 12),
                ),
                notWorking = listOf("Staszek"),
            ),
            onOpenMap = {},
            onOpenFields = {},
            onOpenBase = {},
            onOpenPeople = {},
            onOpenSettings = {},
            onSwitchRole = {},
        )
    }
}
