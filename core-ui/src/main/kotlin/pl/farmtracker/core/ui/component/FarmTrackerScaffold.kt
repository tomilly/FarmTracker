package pl.farmtracker.core.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.R
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

/**
 * Górny pasek aplikacji: ikona + tytuł („gdzie jestem").
 *
 * @param onBack gdy nie `null`, po lewej pojawia się „Wróć" (ikona + podpis, nie sama strzałka).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmTrackerTopBar(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                // Długa nazwa (np. pola) nie może rozepchnąć paska – kończy się wielokropkiem.
                Text(text = title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.core_ui_back), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

/**
 * Szkielet ekranu: [FarmTrackerTopBar] oraz przewijana kolumna treści z dużymi odstępami.
 *
 * @param bottomAction główny przycisk ekranu przyklejony do dołu – widoczny także, gdy treść się przewija
 */
@Composable
fun FarmTrackerScaffold(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    actions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { FarmTrackerTopBar(title = title, icon = icon, onBack = onBack, actions = actions) },
        bottomBar = {
            if (bottomAction != null) {
                Surface(shadowElevation = 8.dp) {
                    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(FarmTrackerDimens.ScreenPadding)) {
                        bottomAction()
                    }
                }
            }
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
