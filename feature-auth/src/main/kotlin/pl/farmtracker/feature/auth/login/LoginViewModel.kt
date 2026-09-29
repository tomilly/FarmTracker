package pl.farmtracker.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.normalizePhoneNumber
import pl.farmtracker.data.auth.AuthRepository
import pl.farmtracker.data.auth.SendCodeResult
import pl.farmtracker.data.auth.VerifyCodeResult
import javax.inject.Inject

enum class LoginStep { PHONE, CODE }

enum class LoginProblem { INVALID_NUMBER, TOO_MANY_ATTEMPTS, UNAVAILABLE, SERVICE_DOWN, WRONG_CODE, EXPIRED }

data class LoginUiState(
    val step: LoginStep = LoginStep.PHONE,
    val phone: String = "",
    /** Numer, na który poszedł SMS (już w formacie „+48…") – pokazujemy go przy wpisywaniu kodu. */
    val sentTo: String? = null,
    val code: String = "",
    val busy: Boolean = false,
    val problem: LoginProblem? = null,
) {
    val canSend: Boolean get() = phone.isNotBlank() && !busy
}

/**
 * Logowanie: numer telefonu → kod z SMS-a. Kod sprawdza się sam po wpisaniu 6 cyfr – bez przycisku
 * „Zatwierdź". Po zalogowaniu aplikacja sama przechodzi dalej (obserwuje stan logowania).
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onPhoneChanged(phone: String) = _uiState.update { it.copy(phone = phone, problem = null) }

    fun sendCode() {
        val state = _uiState.value
        if (!state.canSend) return
        val phone = normalizePhoneNumber(state.phone)
        if (phone == null) {
            _uiState.update { it.copy(problem = LoginProblem.INVALID_NUMBER) }
            return
        }
        _uiState.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            val result = authRepository.sendCode(phone)
            _uiState.update { current ->
                when (result) {
                    SendCodeResult.CodeSent ->
                        current.copy(busy = false, step = LoginStep.CODE, sentTo = phone, code = "")
                    SendCodeResult.SignedIn -> current.copy(busy = false)
                    SendCodeResult.InvalidNumber -> current.copy(busy = false, problem = LoginProblem.INVALID_NUMBER)
                    SendCodeResult.TooManyAttempts -> current.copy(busy = false, problem = LoginProblem.TOO_MANY_ATTEMPTS)
                    SendCodeResult.Unavailable -> current.copy(busy = false, problem = LoginProblem.UNAVAILABLE)
                    SendCodeResult.ServiceDown -> current.copy(busy = false, problem = LoginProblem.SERVICE_DOWN)
                }
            }
        }
    }

    /** Tylko cyfry, najwyżej 6; szóstą cyfrą kod wysyła się sam. */
    fun onCodeChanged(text: String) {
        val code = text.filter { it.isDigit() }.take(CODE_LENGTH)
        _uiState.update { it.copy(code = code, problem = null) }
        if (code.length == CODE_LENGTH) verify(code)
    }

    private fun verify(code: String) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = authRepository.verifyCode(code)
            _uiState.update { current ->
                when (result) {
                    VerifyCodeResult.SignedIn -> current.copy(busy = false)
                    // Zły kod czyścimy – wpisuje się od nowa, bez kasowania cyfra po cyfrze.
                    VerifyCodeResult.WrongCode -> current.copy(busy = false, code = "", problem = LoginProblem.WRONG_CODE)
                    VerifyCodeResult.Expired -> current.copy(busy = false, code = "", problem = LoginProblem.EXPIRED)
                    VerifyCodeResult.Unavailable -> current.copy(busy = false, problem = LoginProblem.UNAVAILABLE)
                }
            }
        }
    }

    /** „Wyślij kod jeszcze raz" – na ten sam numer. */
    fun resendCode() = sendCode()

    /** „Zmień numer" – powrót do wpisywania numeru, z tym co było wpisane. */
    fun changeNumber() = _uiState.update { it.copy(step = LoginStep.PHONE, code = "", problem = null) }

    private companion object {
        const val CODE_LENGTH = 6
    }
}
