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

    NavHost(navController = navController, startDestination = startDestination) {
        composable<RolePickerDestination> { RolePickerScreen() }
        composable<HarvesterDestination> { HarvesterScreen(onSwitchRole = onSwitchRole) }
        composable<DriverDestination> { DriverScreen(onSwitchRole = onSwitchRole) }
        composable<BaseDestination> { BaseScreen(onSwitchRole = onSwitchRole) }
        composable<AdminDestination> { AdminScreen(onSwitchRole = onSwitchRole) }
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
