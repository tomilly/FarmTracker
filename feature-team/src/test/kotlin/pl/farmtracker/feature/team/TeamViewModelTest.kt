package pl.farmtracker.feature.team

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Harvest
import pl.farmtracker.core.domain.InviteCode
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.testing.FakeHarvestRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.harvest.Membership

class TeamViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val me = Member("me", "Tomek", "+48600000001", Role.ADMIN)
    private val marek = Member("u2", "Marek", "+48600000002", Role.DRIVER)
    private val ania = Member("u3", "Ania", "+48600000003", Role.BASE)

    private val harvests = FakeHarvestRepository(
        initial = Membership.Joined(Harvest("h-1", "Kukurydza 2026"), me),
        members = listOf(marek, me, ania),
    )
    private val viewModel by lazy { TeamViewModel(harvests) }
    private val state get() = viewModel.uiState.value

    private fun TestScope.watch() {
        viewModel.uiState.launchIn(backgroundScope)
    }

    private fun teamMember(userId: String) = state.members.first { it.member.userId == userId }

    @Test
    fun `admins first, then by name - I cannot be removed and the only admin cannot change`() =
        runTest(mainDispatcherRule.testDispatcher) {
            watch()

            assertEquals(listOf("Tomek", "Ania", "Marek"), state.members.map { it.member.name })
            assertEquals("Kukurydza 2026", state.harvestName)
            with(teamMember("me")) {
                assertTrue(isMe)
                assertFalse(canChange)
                assertFalse(canRemove)
            }
            assertTrue(teamMember("u2").canRemove)
        }

    @Test
    fun `an invite for a role shows its code`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()

        viewModel.invite(Role.HARVESTER)

        val invite = (state.step as TeamStep.ShowInvite).invite
        assertEquals(Role.HARVESTER, invite.role)
        assertEquals(InviteCode("482913"), invite.code)
    }

    @Test
    fun `without signal the invite is not shown and the problem is`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()
        harvests.available = false

        viewModel.invite(Role.DRIVER)

        assertEquals(TeamStep.List, state.step)
        assertTrue(state.inviteFailed)
    }

    @Test
    fun `the role changes right away`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()
        viewModel.openMember("u2")

        viewModel.changeRole(state.editedMember!!, Role.HARVESTER)

        assertEquals(Role.HARVESTER, teamMember("u2").member.role)
    }

    @Test
    fun `removing goes back to the list and can be undone`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()
        viewModel.openMember("u3")

        viewModel.remove(state.editedMember!!)
        assertEquals(TeamStep.List, state.step)
        assertEquals(listOf("Tomek", "Marek"), state.members.map { it.member.name })
        assertEquals(ania, viewModel.recentlyRemoved.value)

        viewModel.undoRemove()
        assertEquals(listOf("Tomek", "Ania", "Marek"), state.members.map { it.member.name })
        assertNull(viewModel.recentlyRemoved.value)
    }
}
