package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.farmtracker.data.auth.AuthRepository
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.auth.SendCodeResult
import pl.farmtracker.data.auth.VerifyCodeResult

/** Logowanie na niby: [correctCode] loguje numerem z ostatniego [sendCode]. */
class FakeAuthRepository(initial: AuthState = AuthState.SignedOut) : AuthRepository {

    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<AuthState> = _state

    var sendResult: SendCodeResult = SendCodeResult.CodeSent
    var correctCode: String = "123456"
    var verifyFailure: VerifyCodeResult? = null
    val sentTo = mutableListOf<String>()

    override suspend fun sendCode(phone: String): SendCodeResult {
        sentTo += phone
        if (sendResult == SendCodeResult.SignedIn) _state.value = AuthState.SignedIn("user-$phone", phone)
        return sendResult
    }

    override suspend fun verifyCode(code: String): VerifyCodeResult {
        verifyFailure?.let { return it }
        if (code != correctCode) return VerifyCodeResult.WrongCode
        val phone = sentTo.last()
        _state.value = AuthState.SignedIn("user-$phone", phone)
        return VerifyCodeResult.SignedIn
    }

    override suspend fun signOut() {
        _state.value = AuthState.SignedOut
    }
}
