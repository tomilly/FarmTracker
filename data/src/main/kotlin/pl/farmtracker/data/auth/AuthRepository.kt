package pl.farmtracker.data.auth

import kotlinx.coroutines.flow.Flow

/** Kto jest zalogowany na tym telefonie. */
sealed interface AuthState {
    /** Jeszcze nie wiadomo (start aplikacji) – UI nie pokazuje logowania, żeby nie mignęło. */
    data object Loading : AuthState

    data object SignedOut : AuthState

    /** @param withInviteCode konto z samego kodu zaproszenia (bez numeru) – tylko na tym telefonie */
    data class SignedIn(val userId: String, val phone: String, val withInviteCode: Boolean = false) : AuthState
}

sealed interface SendCodeResult {
    data object CodeSent : SendCodeResult

    /** Telefon sam odczytał SMS – nie trzeba wpisywać kodu. */
    data object SignedIn : SendCodeResult

    data object InvalidNumber : SendCodeResult

    /** Za dużo prób z tego numeru / telefonu – trzeba chwilę odczekać. */
    data object TooManyAttempts : SendCodeResult

    /** Brak sieci – można spróbować od razu po odzyskaniu zasięgu. */
    data object Unavailable : SendCodeResult

    /** Sieć jest, ale logowanie po stronie serwera nie działa (np. źle ustawiony projekt) – nie „brak zasięgu". */
    data object ServiceDown : SendCodeResult
}

sealed interface VerifyCodeResult {
    data object SignedIn : VerifyCodeResult

    data object WrongCode : VerifyCodeResult

    /** Kod wygasł – trzeba wysłać nowy. */
    data object Expired : VerifyCodeResult

    data object Unavailable : VerifyCodeResult
}

/**
 * Logowanie numerem telefonu kodem z SMS-a (BRIEF §5) – admin; albo samym kodem zaproszenia ([startWithInviteCode]) –
 * kierowcy, sieczkarnia, baza: bez SMS-a, konto zostaje na tym telefonie.
 */
interface AuthRepository {
    val state: Flow<AuthState>

    /** @param phone numer w formacie międzynarodowym, np. „+48600123456" */
    suspend fun sendCode(phone: String): SendCodeResult

    suspend fun verifyCode(code: String): VerifyCodeResult

    /**
     * Konto bez numeru – do wpisania kodu zaproszenia (reguły serwera wpuszczają tylko zalogowanych).
     * @return `false` – brak zasięgu albo serwer odmówił
     */
    suspend fun startWithInviteCode(): Boolean

    suspend fun signOut()
}
