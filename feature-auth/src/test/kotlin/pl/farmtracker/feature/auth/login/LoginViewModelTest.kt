package pl.farmtracker.feature.auth.login

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.testing.FakeAuthRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.auth.SendCodeResult
import pl.farmtracker.data.auth.VerifyCodeResult

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel by lazy { LoginViewModel(auth) }
    private val state get() = viewModel.uiState.value

    private fun sendTo(phone: String) {
        viewModel.usePhoneNumber()
        viewModel.onPhoneChanged(phone)
        viewModel.sendCode()
    }

    @Test
    fun `a number typed the polish way gets the code`() {
        sendTo("600 123 456")

        assertEquals(listOf("+48600123456"), auth.sentTo)
        assertEquals(LoginStep.CODE, state.step)
        assertEquals("+48600123456", state.sentTo)
    }

    @Test
    fun `something that is not a number is not sent`() {
        sendTo("600 12")

        assertTrue(auth.sentTo.isEmpty())
        assertEquals(LoginProblem.INVALID_NUMBER, state.problem)
        assertEquals(LoginStep.PHONE, state.step)
    }

    @Test
    fun `the sixth digit signs in without pressing anything`() = runTest {
        sendTo("600123456")

        viewModel.onCodeChanged("12345")
        assertTrue(auth.state.first() is AuthState.SignedOut)

        viewModel.onCodeChanged("123456")
        assertEquals(AuthState.SignedIn("user-+48600123456", "+48600123456"), auth.state.first())
    }

    @Test
    fun `a wrong code is cleared so it can be typed again`() {
        sendTo("600123456")

        viewModel.onCodeChanged("111 111")

        assertEquals(LoginProblem.WRONG_CODE, state.problem)
        assertEquals("", state.code)
    }

    @Test
    fun `problems while sending or checking are reported`() {
        auth.sendResult = SendCodeResult.Unavailable
        sendTo("600123456")
        assertEquals(LoginProblem.UNAVAILABLE, state.problem)

        auth.sendResult = SendCodeResult.ServiceDown
        viewModel.sendCode()
        assertEquals(LoginProblem.SERVICE_DOWN, state.problem)

        auth.sendResult = SendCodeResult.CodeSent
        viewModel.sendCode()
        auth.verifyFailure = VerifyCodeResult.Expired
        viewModel.onCodeChanged("123456")
        assertEquals(LoginProblem.EXPIRED, state.problem)
    }

    @Test
    fun `resend goes to the same number, change number keeps what was typed`() {
        sendTo("600123456")

        viewModel.resendCode()
        assertEquals(listOf("+48600123456", "+48600123456"), auth.sentTo)

        viewModel.changeNumber()
        assertEquals(LoginStep.PHONE, state.step)
        assertEquals("600123456", state.phone)
        assertNull(state.problem)
    }

    @Test
    fun `first the choice - invite code or phone number, back from the number returns to it`() {
        assertEquals(LoginStep.START, state.step)

        viewModel.usePhoneNumber()
        assertEquals(LoginStep.PHONE, state.step)

        viewModel.backToStart()
        assertEquals(LoginStep.START, state.step)
    }

    @Test
    fun `I have an invite code - no number, no SMS, the app moves on by itself`() = runTest {
        viewModel.startWithInviteCode()

        assertTrue(auth.sentTo.isEmpty())
        assertEquals(AuthState.SignedIn("invited", phone = "", withInviteCode = true), auth.state.first())
    }

    @Test
    fun `I have an invite code without signal - say so, try again`() {
        auth.inviteSignInWorks = false

        viewModel.startWithInviteCode()

        assertEquals(LoginProblem.INVITE_START_FAILED, state.problem)
        assertEquals(AuthState.SignedOut, auth.state.value)
    }
}
