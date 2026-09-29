package pl.farmtracker.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pl.farmtracker.app.BuildConfig
import pl.farmtracker.feature.fields.editor.FieldEditorScreen
import pl.farmtracker.feature.fields.list.FieldsListScreen
import pl.farmtracker.feature.fields.view.FieldScreen
import pl.farmtracker.feature.map.MapScreen
import pl.farmtracker.feature.roles.admin.AdminScreen
import pl.farmtracker.feature.roles.base.BaseScreen
import pl.farmtracker.feature.roles.driver.DriverScreen
import pl.farmtracker.feature.roles.harvester.HarvesterScreen
import pl.farmtracker.feature.roles.picker.RolePickerScreen

@Composable
fun FarmTrackerApp(viewModel: AppViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        // Odczyt roli z dysku trwa chwilę – pusty ekran w kolorze tła zamiast migania wyboru roli.
        AppUiState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        is AppUiState.Ready -> FarmTrackerNavHost(
            destination = state.destination,
            onSwitchRole = if (BuildConfig.DEBUG) viewModel::switchRole else null,
        )
    }
}

@Composable
private fun FarmTrackerNavHost(
    destination: AppDestination,
    onSwitchRole: (() -> Unit)?,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = remember { destination }

    val openMap = { navController.navigate(MapDestination) { launchSingleTop = true } }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<RolePickerDestination> { RolePickerScreen() }
        composable<HarvesterDestination> { HarvesterScreen(onOpenMap = openMap, onSwitchRole = onSwitchRole) }
        composable<DriverDestination> { DriverScreen(onOpenMap = openMap, onSwitchRole = onSwitchRole) }
        composable<BaseDestination> { BaseScreen(onOpenMap = openMap, onSwitchRole = onSwitchRole) }
        composable<AdminDestination> {
            AdminScreen(
                onOpenFields = { navController.navigate(FieldsListDestination) { launchSingleTop = true } },
                onSwitchRole = onSwitchRole,
            )
        }
        composable<MapDestination> {
            MapScreen(
                onBack = { navController.popBackStack() },
                onEditField = { id -> navController.navigate(FieldEditorDestination(id)) { launchSingleTop = true } },
            )
        }
        composable<FieldsListDestination> {
            FieldsListScreen(
                onBack = { navController.popBackStack() },
                onAddField = { navController.navigate(FieldEditorDestination()) { launchSingleTop = true } },
                onOpenField = { id -> navController.navigate(FieldDestination(id)) { launchSingleTop = true } },
                onShowMap = openMap,
            )
        }
        composable<FieldDestination> {
            FieldScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(FieldEditorDestination(id)) { launchSingleTop = true } },
            )
        }
        composable<FieldEditorDestination> {
            FieldEditorScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
    }

    // Zmiana roli = nowy ekran startowy; czyścimy stos, żeby „wstecz" nie wracało do starej roli.
    LaunchedEffect(destination) {
        if (navController.currentDestination?.hasRoute(destination::class) != true) {
            navController.navigate(destination) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
