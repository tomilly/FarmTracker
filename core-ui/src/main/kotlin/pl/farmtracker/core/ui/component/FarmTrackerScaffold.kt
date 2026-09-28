package pl.farmtracker.core.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.R
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

/**
 * Szkielet ekranu: górny pasek z ikoną i tytułem („gdzie jestem") oraz przewijana kolumna treści
 * z dużymi odstępami.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmTrackerScaffold(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(text = title, style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(FarmTrackerDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(FarmTrackerDimens.ItemSpacing),
            content = content,
        )
    }
}

/**
 * Ekran roli: w pasku zawsze widać, kim jestem (ikona + nazwa roli) – test „trunk test" (BRIEF §4).
 *
 * @param onSwitchRole gdy nie `null`, w pasku pojawia się „Zmień rolę" (tymczasowo, tylko w wersji debug).
 */
@Composable
fun RoleScaffold(
    role: Role,
    modifier: Modifier = Modifier,
    onSwitchRole: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable ColumnScope.() -> Unit,
) {
    FarmTrackerScaffold(
        title = stringResource(RoleUi.labelRes(role)),
        icon = RoleUi.icon(role),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        actions = {
            if (onSwitchRole != null) {
                TextButton(
                    onClick = onSwitchRole,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.core_ui_switch_role), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        content = content,
    )
}

@PreviewLightDark
@Composable
private fun RoleScaffoldPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        RoleScaffold(role = Role.DRIVER, onSwitchRole = {}) {
            Text("Treść ekranu", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
