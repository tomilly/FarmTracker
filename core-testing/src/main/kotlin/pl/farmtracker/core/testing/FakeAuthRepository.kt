package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import pl.farmtracker.data.auth.AuthRepository
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.auth.SendCodeResult
import pl.farmtracker.data.auth.VerifyCodeResult

/** Logowanie na niby: [correctCode] loguje numerem z ostatniego [sendCode]. */
class FakeAuthRepository(initial: AuthState = AuthState.SignedOut) : AuthRepository {

    override val state = MutableStateFlow(initial)

    var sendResult: SendCodeResult = SendCodeResult.CodeSent
    var correctCode: String = "123456"
    var verifyFailure: VerifyCodeResult? = null
    val sentTo = mutableListOf<String>()

    override suspend fun sendCode(phone: String): SendCodeResult {
        sentTo += phone
        if (sendResult == SendCodeResult.SignedIn) state.value = AuthState.SignedIn("user-$phone", phone)
        return sendResult
    }

    override suspend fun verifyCode(code: String): VerifyCodeResult {
        verifyFailure?.let { return it }
        if (code != correctCode) return VerifyCodeResult.WrongCode
        val phone = sentTo.last()
        state.value = AuthState.SignedIn("user-$phone", phone)
        return VerifyCodeResult.SignedIn
    }

    /** `false` – udaje brak zasięgu przy zakładaniu konta do kodu. */
    var inviteSignInWorks: Boolean = true

    override suspend fun startWithInviteCode(): Boolean {
        if (!inviteSignInWorks) return false
        state.value = AuthState.SignedIn("invited", phone = "", withInviteCode = true)
        return true
    }

    override suspend fun signOut() {
        state.value = AuthState.SignedOut
    }
}
