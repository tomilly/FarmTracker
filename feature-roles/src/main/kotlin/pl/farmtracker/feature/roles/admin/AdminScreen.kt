package pl.farmtracker.feature.roles.admin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.RoleScaffold
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R

/**
 * Menu admina – płaskie (BRIEF §4). Na razie bez ViewModelu. „Pola" i „Baza" – miejsca zbioru;
 * „Ludzie" – zaproszenia i role (feature-team); „Ustawienia" – konto i wylogowanie (feature-auth).
 */
@Composable
fun AdminScreen(
    onOpenFields: () -> Unit,
    onOpenBase: () -> Unit,
    /** `null` – „Ludzie" jeszcze niedostępne (bez wspólnego zbioru): pokazujemy „wkrótce". */
    onOpenPeople: (() -> Unit)?,
    /** `null` – bez wspólnego zbioru nie ma konta do pokazania ani wylogowania: „wkrótce". */
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
        AdminScreen(onOpenFields = {}, onOpenBase = {}, onOpenPeople = {}, onOpenSettings = {}, onSwitchRole = {})
    }
}
