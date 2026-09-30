package pl.farmtracker.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.app.BuildConfig
import pl.farmtracker.app.di.SharedHarvest
import pl.farmtracker.data.auth.AuthRepository
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import pl.farmtracker.data.session.SessionRepository
import pl.farmtracker.data.work.WorkRepository
import javax.inject.Inject

sealed interface AppUiState {
    data object Loading : AppUiState

    data class Ready(val destination: AppDestination) : AppUiState
}

/**
 * Dokąd trafia osoba po otwarciu aplikacji:
 * - wspólny zbiór (Firebase): logowanie → pierwsze wejście (imię, kod / nowy zbiór) → ekran roli,
 * - bez Firebase: jak przed M3 – wybór roli zapisany na telefonie.
 *
 * W wersji testowej (debug) „Zmień rolę" pozwala na jednym telefonie obejrzeć ekrany innych ról –
 * wybrana rola przykrywa rolę ze zbioru tylko na tym telefonie.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val harvestRepository: HarvestRepository,
    private val workRepository: WorkRepository,
    @SharedHarvest private val sharedHarvest: Boolean,
) : ViewModel() {

    /** „Zmień rolę" (debug) w trybie zbioru – pokaż wybór roli zamiast ekranu roli ze zbioru. */
    private val pickingRole = MutableStateFlow(false)

    private val askingToStopWork = MutableStateFlow(false)

    val uiState: StateFlow<AppUiState> =
        (if (sharedHarvest) sharedHarvestState() else localState())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    /** Pytanie „Skończyć pracę?" nad każdym ekranem – po „Kończę pracę" w powiadomieniu. */
    val confirmStopWork: StateFlow<Boolean> =
        combine(askingToStopWork, workRepository.isWorking) { asking, working -> asking && working }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // Rola wybrana na telefonie przed M3 nie może przykryć roli ze zbioru – poza wersją testową.
        if (sharedHarvest && !BuildConfig.DEBUG) viewModelScope.launch { sessionRepository.clearRole() }
        // Rola z „Zmień rolę" należy do tej osoby – po wylogowaniu następna trafia na ekran swojej roli.
        if (sharedHarvest) {
            viewModelScope.launch {
                authRepository.state.collect { auth -> if (auth == AuthState.SignedOut) sessionRepository.clearRole() }
            }
        }
    }

    private fun localState(): Flow<AppUiState> =
        sessionRepository.currentRole.map { role -> AppUiState.Ready(role.toDestination()) }

    private fun sharedHarvestState(): Flow<AppUiState> = authRepository.state.flatMapLatest { auth ->
        when (auth) {
            AuthState.Loading -> flowOf(AppUiState.Loading)
            AuthState.SignedOut -> flowOf(AppUiState.Ready(LoginDestination))
            is AuthState.SignedIn -> combine(
                harvestRepository.membership,
                sessionRepository.currentRole,
                pickingRole,
            ) { membership, roleOverride, picking ->
                when (membership) {
                    Membership.Loading -> AppUiState.Loading
                    Membership.None -> AppUiState.Ready(OnboardingDestination)
                    is Membership.Joined -> AppUiState.Ready(
                        when {
                            picking && roleOverride == null -> RolePickerDestination
                            else -> (roleOverride ?: membership.me.role).toDestination()
                        },
                    )
                }
            }
        }
    }

    /** „Kończę pracę" w powiadomieniu – jak mały przycisk na ekranie roli, najpierw pytamy. */
    fun askToStopWork() {
        askingToStopWork.value = true
    }

    fun stopWork() {
        askingToStopWork.value = false
        viewModelScope.launch { workRepository.setWorking(false) }
    }

    fun keepWorking() {
        askingToStopWork.value = false
    }

    /** Debug: wraca do wyboru roli. */
    fun switchRole() {
        viewModelScope.launch {
            sessionRepository.clearRole()
            pickingRole.value = true
        }
    }
}
