package pl.farmtracker.app.navigation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Harvest
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.testing.FakeAuthRepository
import pl.farmtracker.core.testing.FakeHarvestRepository
import pl.farmtracker.core.testing.FakeSessionRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.harvest.Membership

class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signedIn = AuthState.SignedIn("u1", "+48600000001")
    private val driver = Member("u1", "Marek", "+48600000001", Role.DRIVER)
    private val joinedAsDriver = Membership.Joined(Harvest("h-1", "Kukurydza 2026"), driver)

    private fun local(role: Role?) = AppViewModel(
        FakeSessionRepository(initialRole = role),
        FakeAuthRepository(),
        FakeHarvestRepository(),
        sharedHarvest = false,
    )

    private fun shared(
        auth: AuthState,
        membership: Membership = Membership.None,
        session: FakeSessionRepository = FakeSessionRepository(),
    ) = AppViewModel(session, FakeAuthRepository(auth), FakeHarvestRepository(initial = membership), sharedHarvest = true)

    @Test
    fun `without firebase and without a role the role picker is shown`() = runTest {
        local(role = null).uiState.test {
            assertEquals(AppUiState.Ready(RolePickerDestination), awaitReady())
        }
    }

    @Test
    fun `without firebase a chosen role opens its screen right away`() = runTest {
        local(role = Role.DRIVER).uiState.test {
            assertEquals(AppUiState.Ready(DriverDestination), awaitReady())
        }
    }

    @Test
    fun `with a shared harvest a signed out person logs in first`() = runTest {
        shared(AuthState.SignedOut).uiState.test {
            assertEquals(AppUiState.Ready(LoginDestination), awaitReady())
        }
    }

    @Test
    fun `signed in without a harvest - name and invite code or a new harvest`() = runTest {
        shared(signedIn, Membership.None).uiState.test {
            assertEquals(AppUiState.Ready(OnboardingDestination), awaitReady())
        }
    }

    @Test
    fun `a member goes straight to the screen of their role`() = runTest {
        shared(signedIn, joinedAsDriver).uiState.test {
            assertEquals(AppUiState.Ready(DriverDestination), awaitReady())
        }
    }

    @Test
    fun `debug role switch shows the picker and the picked role covers the harvest role`() = runTest {
        val session = FakeSessionRepository()
        val viewModel = shared(signedIn, joinedAsDriver, session)

        viewModel.uiState.test {
            assertEquals(AppUiState.Ready(DriverDestination), awaitReady())

            viewModel.switchRole()
            assertEquals(AppUiState.Ready(RolePickerDestination), awaitReady())

            session.setRole(Role.BASE)
            assertEquals(AppUiState.Ready(BaseDestination), awaitReady())
        }
    }
}

/** Pomija początkowy stan ładowania – zależy od kolejności startu `stateIn`. */
private suspend fun ReceiveTurbine<AppUiState>.awaitReady(): AppUiState {
    var item = awaitItem()
    while (item == AppUiState.Loading) item = awaitItem()
    return item
}
