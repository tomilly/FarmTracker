package pl.farmtracker.feature.auth.settings

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Harvest
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.testing.FakeAuthRepository
import pl.farmtracker.core.testing.FakeHarvestRepository
import pl.farmtracker.core.testing.FakeLiveLocationRepository
import pl.farmtracker.core.testing.FakeSessionRepository
import pl.farmtracker.core.testing.FakeWorkRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.harvest.Membership

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val me = Member("u1", "Tomek", "+48600000001", Role.ADMIN)
    private val auth = FakeAuthRepository(AuthState.SignedIn("u1", "+48600000001"))
    private val harvest = FakeHarvestRepository(initial = Membership.Joined(Harvest("h-1", "Kukurydza 2026"), me))
    private val work = FakeWorkRepository(working = true)
    private val locations = FakeLiveLocationRepository(myRole = Role.ADMIN)
    private val session = FakeSessionRepository(Role.DRIVER)
    private val viewModel by lazy { SettingsViewModel(auth, harvest, work, locations, session) }

    @Test
    fun `shows who I am and in which harvest`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.uiState.launchIn(backgroundScope)

        assertEquals(
            SettingsUiState(name = "Tomek", phone = "+48600000001", harvestName = "Kukurydza 2026", role = Role.ADMIN),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `signing out ends work, hides me from the map and forgets the test role`() = runTest(mainDispatcherRule.testDispatcher) {
        locations.publish(PositionReport(GeoPoint(50.0, 17.0), fieldId = null, timeMillis = 0))

        viewModel.signOut()

        assertEquals(AuthState.SignedOut, auth.state.value)
        assertFalse(work.isWorking.value)
        assertTrue(locations.locations.value.none { it.isMe })
        assertNull(session.currentRole.value)
    }
}
