package pl.farmtracker.feature.roles.admin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.map.MapIconButton
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CoworkerRow
import pl.farmtracker.feature.roles.common.NotWorkingRow
import pl.farmtracker.feature.roles.common.RoleMap

/**
 * Admin: mapa całego zbioru (wszystkie pola i baza), pod nią arkusz – kto co robi (ładuje, wraca do bazy, stoi…),
 * kto nie pracuje, menu zbioru jako 4 kafelki (BRIEF §4: „Pola", „Baza", „Ludzie", „Ustawienia") i na końcu
 * „Sam też pracujesz?" – w małym gospodarstwie admin często sam jedzie. Ikona w rogu mapy otwiera dużą mapę
 * (pola do edycji, działki).
 */
@Composable
fun AdminScreen(
    onOpenMap: () -> Unit,
    /** Admin sam pracuje – ekran tej roli („Zaczynam pracę"); inni widzą go jako sieczkarnię / kierowcę. */
    onWorkAs: (Role) -> Unit,
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
    val overlays by viewModel.overlays.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.roles_admin_coming_soon)
    val showComingSoon: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }

    Box(modifier) {
        RoleMap(
            chrome = viewModel.chrome,
            overlays = overlays,
            onSwitchRole = onSwitchRole,
            mapButtons = {
                MapIconButton(
                    icon = Icons.Filled.OpenInFull,
                    contentDescription = stringResource(R.string.roles_admin_big_map),
                    onClick = onOpenMap,
                )
            },
            sheet = {
                AdminSheet(
                    uiState = uiState,
                    onWorkAs = onWorkAs,
                    onOpenFields = onOpenFields,
                    onOpenBase = onOpenBase,
                    onOpenPeople = onOpenPeople ?: showComingSoon,
                    onOpenSettings = onOpenSettings ?: showComingSoon,
                )
            },
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

@Composable
internal fun ColumnScope.AdminSheet(
    uiState: AdminUiState,
    onWorkAs: (Role) -> Unit,
    onOpenFields: () -> Unit,
    onOpenBase: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Text(stringResource(R.string.roles_admin_overview_title), style = MaterialTheme.typography.titleMedium)
    val working = uiState.harvesters + uiState.drivers
    if (working.isEmpty() && uiState.notWorking.isEmpty()) {
        Text(stringResource(R.string.roles_admin_nobody), style = MaterialTheme.typography.bodyLarge)
    }
    working.forEach { CoworkerRow(it) }
    uiState.notWorking.forEach { NotWorkingRow(it) }

    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuTile(stringResource(R.string.roles_admin_fields), Icons.Filled.Map, onOpenFields, Modifier.weight(1f))
        MenuTile(stringResource(R.string.roles_admin_base), Icons.Filled.Warehouse, onOpenBase, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuTile(stringResource(R.string.roles_admin_people), Icons.Filled.Groups, onOpenPeople, Modifier.weight(1f))
        MenuTile(stringResource(R.string.roles_admin_settings), Icons.Filled.Settings, onOpenSettings, Modifier.weight(1f))
    }

    Text(
        stringResource(R.string.roles_admin_work_title),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Role.HARVESTER, Role.DRIVER).forEach { role ->
            OutlinedButton(
                onClick = { onWorkAs(role) },
                modifier = Modifier.weight(1f).heightIn(min = FarmTrackerDimens.MinTouchTarget),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(RoleUi.icon(role), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(RoleUi.labelRes(role)), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

/** Kafelek menu zbioru: ikona + podpis, mniejszy niż duże przyciski – to nie praca, tylko ustawienia zbioru. */
@Composable
private fun MenuTile(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@PreviewLightDark
@Composable
private fun AdminSheetPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminSheet(
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
                    onWorkAs = {},
                    onOpenFields = {},
                    onOpenBase = {},
                    onOpenPeople = {},
                    onOpenSettings = {},
                )
            }
        }
    }
}
