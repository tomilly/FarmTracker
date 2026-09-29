package pl.farmtracker.feature.auth.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.FarmTrackerScaffold
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.auth.R

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(uiState = uiState, onBack = onBack, onSignOut = viewModel::signOut, modifier = modifier)
}

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    FarmTrackerScaffold(
        title = stringResource(R.string.settings_title),
        icon = Icons.Filled.Settings,
        onBack = onBack,
        modifier = modifier,
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(uiState.name, style = MaterialTheme.typography.headlineSmall)
                Text(uiState.phone, style = MaterialTheme.typography.bodyLarge)
                if (uiState.harvestName.isNotEmpty()) {
                    Text(
                        stringResource(R.string.settings_harvest, uiState.harvestName),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                uiState.role?.let { role ->
                    Text(
                        stringResource(R.string.settings_role, stringResource(RoleUi.labelRes(role))),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        BigActionButton(
            text = stringResource(if (uiState.signingOut) R.string.settings_signing_out else R.string.settings_sign_out),
            icon = Icons.AutoMirrored.Filled.Logout,
            onClick = { confirming = true },
            tone = Tone.Neutral,
            enabled = !uiState.signingOut,
        )
    }
    if (confirming) {
        SignOutDialog(
            onConfirm = {
                confirming = false
                onSignOut()
            },
            onDismiss = { confirming = false },
        )
    }
}

/** Po wylogowaniu trzeba znów dostać SMS – więc pytamy, czy na pewno. */
@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_sign_out_question)) },
        text = { Text(stringResource(R.string.settings_sign_out_explanation), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget)) {
                    Text(stringResource(R.string.settings_sign_out_confirm), style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget)) {
                    Text(stringResource(R.string.settings_sign_out_cancel), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        SettingsContent(
            uiState = SettingsUiState(name = "Tomek", phone = "+48600000001", harvestName = "Kukurydza 2026", role = Role.ADMIN),
            onBack = {},
            onSignOut = {},
        )
    }
}
