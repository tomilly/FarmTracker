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

    private fun inviteDriver(name: String, phone: String) {
        viewModel.startInvite(Role.DRIVER)
        viewModel.onInviteNameChanged(name)
        viewModel.onInvitePhoneChanged(phone)
        viewModel.createInvite()
    }

    @Test
    fun `inviting a person - name and number, then the code to send by SMS`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()

        inviteDriver(" Janek ", "600 000 003")

        val invite = (state.step as TeamStep.ShowInvite).invite
        assertEquals(Role.DRIVER, invite.role)
        assertEquals(InviteCode("482913"), invite.code)
        assertEquals("Janek", invite.name)
        assertEquals("+48600000003", invite.phone)
        assertEquals(listOf(invite), state.invites)
    }

    @Test
    fun `a name is needed, the number can be left out but not mistyped`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()

        viewModel.startInvite(Role.DRIVER)
        assertFalse(state.draft.canCreate)

        inviteDriver("Janek", "600 12")
        assertTrue(state.draft.invalidPhone)
        assertTrue(state.step is TeamStep.NewInvite)

        viewModel.onInvitePhoneChanged("")
        viewModel.createInvite()
        assertEquals("", (state.step as TeamStep.ShowInvite).invite.phone)
    }

    @Test
    fun `without signal the invite is not made and the problem is shown`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()
        harvests.available = false

        inviteDriver("Janek", "")

        assertTrue(state.step is TeamStep.NewInvite)
        assertTrue(state.inviteFailed)
    }

    @Test
    fun `a pending invite can be opened again or withdrawn`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()
        inviteDriver("Janek", "")
        val invite = state.invites.single()
        viewModel.backToList()

        viewModel.openInvite(invite)
        assertEquals(TeamStep.ShowInvite(invite), state.step)

        viewModel.cancelInvite(invite)
        assertEquals(TeamStep.List, state.step)
        assertTrue(state.invites.isEmpty())
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
