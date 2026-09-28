package pl.farmtracker.app.navigation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.testing.FakeSessionRepository
import pl.farmtracker.core.testing.MainDispatcherRule

class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `without a role the role picker is shown`() = runTest {
        val viewModel = AppViewModel(FakeSessionRepository(initialRole = null))

        viewModel.uiState.test {
            assertEquals(AppUiState.Ready(RolePickerDestination), awaitReady())
        }
    }

    @Test
    fun `with a role its screen is shown right away`() = runTest {
        val viewModel = AppViewModel(FakeSessionRepository(initialRole = Role.DRIVER))

        viewModel.uiState.test {
            assertEquals(AppUiState.Ready(DriverDestination), awaitReady())
        }
    }

    @Test
    fun `switching role goes back to role picker`() = runTest {
        val viewModel = AppViewModel(FakeSessionRepository(initialRole = Role.BASE))

        viewModel.uiState.test {
            assertEquals(AppUiState.Ready(BaseDestination), awaitReady())

            viewModel.switchRole()

            assertEquals(AppUiState.Ready(RolePickerDestination), awaitReady())
        }
    }
}

/** Pomija początkowy stan ładowania – zależy od kolejności startu `stateIn`. */
private suspend fun ReceiveTurbine<AppUiState>.awaitReady(): AppUiState {
    var item = awaitItem()
    while (item == AppUiState.Loading) item = awaitItem()
    return item
}
