package pl.farmtracker.feature.auth.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.InviteCode
import pl.farmtracker.data.auth.AuthRepository
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.JoinResult
import java.time.Year
import javax.inject.Inject

enum class OnboardingStep { CHOOSE, JOIN, CREATE }

enum class OnboardingProblem { INVALID_CODE, UNAVAILABLE }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CHOOSE,
    /** Imię widoczne dla innych („Następny: Marek"). */
    val name: String = "",
    val code: String = "",
    val harvestName: String = "",
    val busy: Boolean = false,
    val problem: OnboardingProblem? = null,
    /** Konto z samego kodu zaproszenia – tylko kod: imię i rola są w zaproszeniu, zbioru się nie zakłada. */
    val inviteOnly: Boolean = false,
) {
    val canContinue: Boolean get() = name.isNotBlank()
    val canCreate: Boolean get() = harvestName.isNotBlank() && !busy
}

/**
 * Pierwsze wejście, gdy osoba nie należy jeszcze do zbioru. Po „Mam kod zaproszenia" (bez numeru) – od razu kod.
 * Po logowaniu numerem: imię, a potem kod zaproszenia albo założenie zbioru (admin). Po dołączeniu aplikacja
 * sama przechodzi na ekran roli.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val harvestRepository: HarvestRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(harvestName = defaultHarvestName()))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val auth = authRepository.state.first { it !is AuthState.Loading }
            if (auth is AuthState.SignedIn && auth.withInviteCode) {
                _uiState.update { it.copy(step = OnboardingStep.JOIN, inviteOnly = true) }
            }
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun startJoining() = moveTo(OnboardingStep.JOIN)

    fun startCreating() = moveTo(OnboardingStep.CREATE)

    /** Konto z samego kodu nie ma kroku wcześniej – wraca do pierwszego ekranu („Mam kod" / numer telefonu). */
    fun back() {
        if (_uiState.value.inviteOnly) {
            viewModelScope.launch { authRepository.signOut() }
        } else {
            _uiState.update { it.copy(step = OnboardingStep.CHOOSE, problem = null) }
        }
    }

    private fun moveTo(step: OnboardingStep) = _uiState.update { if (it.canContinue) it.copy(step = step) else it }

    /** Tylko cyfry, najwyżej 6; szóstą cyfrą dołącza się samo – jak kod z SMS-a. */
    fun onCodeChanged(text: String) {
        val digits = text.filter { it.isDigit() }.take(InviteCode.LENGTH)
        _uiState.update { it.copy(code = digits, problem = null) }
        InviteCode.parse(digits)?.let(::join)
    }

    private fun join(code: InviteCode) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            // Samym kodem – imię z zaproszenia (wpisał je admin).
            val typed = _uiState.value
            val result = harvestRepository.join(code, myName = if (typed.inviteOnly) null else typed.name.trim())
            _uiState.update { state ->
                when (result) {
                    JoinResult.Joined -> state.copy(busy = false)
                    JoinResult.InvalidCode -> state.copy(busy = false, code = "", problem = OnboardingProblem.INVALID_CODE)
                    JoinResult.Unavailable -> state.copy(busy = false, problem = OnboardingProblem.UNAVAILABLE)
                }
            }
        }
    }

    /** Po braku zasięgu – ten sam kod jeszcze raz, bez przepisywania. */
    fun retryJoin() {
        InviteCode.parse(_uiState.value.code)?.let(::join)
    }

    fun onHarvestNameChanged(name: String) = _uiState.update { it.copy(harvestName = name) }

    fun createHarvest() {
        val state = _uiState.value
        if (!state.canCreate) return
        _uiState.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            val created = harvestRepository.createHarvest(state.harvestName.trim(), state.name.trim())
            _uiState.update { it.copy(busy = false, problem = if (created) null else OnboardingProblem.UNAVAILABLE) }
        }
    }

    private companion object {
        /** „Kukurydza 2026" – najczęstsza nazwa; łatwo zmienić. */
        fun defaultHarvestName(): String = "Kukurydza ${Year.now().value}"
    }
}
