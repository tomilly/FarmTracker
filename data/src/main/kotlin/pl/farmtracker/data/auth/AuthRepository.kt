package pl.farmtracker.data.auth

import kotlinx.coroutines.flow.Flow

/** Kto jest zalogowany na tym telefonie. */
sealed interface AuthState {
    /** Jeszcze nie wiadomo (start aplikacji) – UI nie pokazuje logowania, żeby nie mignęło. */
    data object Loading : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val userId: String, val phone: String) : AuthState
}

sealed interface SendCodeResult {
    data object CodeSent : SendCodeResult

    /** Telefon sam odczytał SMS – nie trzeba wpisywać kodu. */
    data object SignedIn : SendCodeResult

    data object InvalidNumber : SendCodeResult

    /** Za dużo prób z tego numeru / telefonu – trzeba chwilę odczekać. */
    data object TooManyAttempts : SendCodeResult

    data object Unavailable : SendCodeResult
}

sealed interface VerifyCodeResult {
    data object SignedIn : VerifyCodeResult

    data object WrongCode : VerifyCodeResult

    /** Kod wygasł – trzeba wysłać nowy. */
    data object Expired : VerifyCodeResult

    data object Unavailable : VerifyCodeResult
}

/** Logowanie numerem telefonu kodem z SMS-a (BRIEF §5). */
interface AuthRepository {
    val state: Flow<AuthState>

    /** @param phone numer w formacie międzynarodowym, np. „+48600123456" */
    suspend fun sendCode(phone: String): SendCodeResult

    suspend fun verifyCode(code: String): VerifyCodeResult

    suspend fun signOut()
}
