package pl.farmtracker.feature.auth.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Invite
import pl.farmtracker.core.domain.InviteCode
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.testing.FakeAuthRepository
import pl.farmtracker.core.testing.FakeHarvestRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.harvest.Membership

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harvests = FakeHarvestRepository()
    private val auth = FakeAuthRepository(AuthState.SignedIn("u1", "+48600000002"))
    private val viewModel by lazy { OnboardingViewModel(harvests, auth) }
    private val state get() = viewModel.uiState.value
    private val membership get() = harvests.membership.value

    @Test
    fun `a name is needed before joining or creating`() {
        viewModel.startJoining()
        assertEquals(OnboardingStep.CHOOSE, state.step)

        viewModel.onNameChanged("Marek")
        viewModel.startJoining()
        assertEquals(OnboardingStep.JOIN, state.step)
    }

    @Test
    fun `the sixth digit of an invite joins with the invited role and my name`() {
        harvests.addInvite(Invite(InviteCode("482913"), "h-1", Role.DRIVER, expiresAtMillis = 1_000))
        viewModel.onNameChanged(" Marek ")
        viewModel.startJoining()

        viewModel.onCodeChanged("482 91")
        assertEquals(Membership.None, membership)

        viewModel.onCodeChanged("482 913")
        val me = (membership as Membership.Joined).me
        assertEquals("Marek", me.name)
        assertEquals(Role.DRIVER, me.role)
    }

    @Test
    fun `a wrong or expired code is cleared with an explanation`() {
        harvests.addInvite(Invite(InviteCode("482913"), "h-1", Role.DRIVER, expiresAtMillis = 1_000))
        harvests.now = 2_000 // wygasło
        viewModel.onNameChanged("Marek")
        viewModel.startJoining()

        viewModel.onCodeChanged("482913")

        assertEquals(OnboardingProblem.INVALID_CODE, state.problem)
        assertEquals("", state.code)
    }

    @Test
    fun `without signal the typed code stays and can be retried`() {
        harvests.addInvite(Invite(InviteCode("482913"), "h-1", Role.BASE, expiresAtMillis = 1_000))
        harvests.available = false
        viewModel.onNameChanged("Ania")
        viewModel.startJoining()

        viewModel.onCodeChanged("482913")
        assertEquals(OnboardingProblem.UNAVAILABLE, state.problem)
        assertEquals("482913", state.code)

        harvests.available = true
        viewModel.retryJoin()
        assertEquals(Role.BASE, (membership as Membership.Joined).me.role)
    }

    @Test
    fun `creating a harvest makes me its admin, the name is suggested`() {
        assertTrue(state.harvestName.startsWith("Kukurydza "))
        viewModel.onNameChanged("Tomek")
        viewModel.startCreating()
        viewModel.onHarvestNameChanged("Kukurydza u Tomka")

        viewModel.createHarvest()

        val joined = membership as Membership.Joined
        assertEquals("Kukurydza u Tomka", joined.harvest.name)
        assertEquals(Role.ADMIN, joined.me.role)
    }

    @Test
    fun `with only an invite code - straight to the code, name and role from the invite, used once`() {
        auth.state.value = AuthState.SignedIn("invited", phone = "", withInviteCode = true)
        harvests.addInvite(Invite(InviteCode("482913"), "h-1", Role.DRIVER, expiresAtMillis = 1_000, name = "Marek"))
        assertEquals(OnboardingStep.JOIN, state.step)
        assertTrue(state.inviteOnly)

        viewModel.onCodeChanged("482913")

        val me = (membership as Membership.Joined).me
        assertEquals("Marek", me.name)
        assertEquals(Role.DRIVER, me.role)
        assertTrue(harvests.invites.value.isEmpty())
    }

    @Test
    fun `with only an invite code, back returns to the first screen`() {
        auth.state.value = AuthState.SignedIn("invited", phone = "", withInviteCode = true)
        assertEquals(OnboardingStep.JOIN, state.step)

        viewModel.back()

        assertEquals(AuthState.SignedOut, auth.state.value)
    }
}
