package pl.farmtracker.feature.roles.picker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.FarmTrackerScaffold
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R

/** Kolejność od ról najczęstszych w polu. */
private val PickerOrder = listOf(Role.HARVESTER, Role.DRIVER, Role.BASE, Role.ADMIN)

/** Tymczasowy wybór roli – w M3 zastąpi go logowanie i rola z zaproszenia. */
@Composable
fun RolePickerScreen(
    modifier: Modifier = Modifier,
    viewModel: RolePickerViewModel = hiltViewModel(),
) {
    RolePickerContent(onRoleSelected = viewModel::selectRole, modifier = modifier)
}

@Composable
internal fun RolePickerContent(
    onRoleSelected: (Role) -> Unit,
    modifier: Modifier = Modifier,
) {
    FarmTrackerScaffold(
        title = stringResource(R.string.roles_picker_title),
        icon = Icons.Filled.Person,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.roles_picker_hint), style = MaterialTheme.typography.bodyLarge)
        PickerOrder.forEach { role ->
            BigActionButton(
                text = stringResource(RoleUi.labelRes(role)),
                icon = RoleUi.icon(role),
                onClick = { onRoleSelected(role) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RolePickerPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        RolePickerContent(onRoleSelected = {})
    }
}
