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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.app.di.DebugBuild
import pl.farmtracker.app.di.SharedHarvest
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.auth.AuthRepository
import pl.farmtracker.data.auth.AuthState
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import pl.farmtracker.data.session.SessionRepository
import pl.farmtracker.data.work.WorkRepository
import javax.inject.Inject

sealed interface AppUiState {
    data object Loading : AppUiState

    /** @param backToAdmin admin pracuje jako sieczkarnia / kierowca – na ekranie roli „Admin" wraca do panelu */
    data class Ready(val destination: AppDestination, val backToAdmin: Boolean = false) : AppUiState
}

/**
 * Dokąd trafia osoba po otwarciu aplikacji:
 * - wspólny zbiór (Firebase): logowanie → pierwsze wejście (imię, kod / nowy zbiór) → ekran roli,
 * - bez Firebase: jak przed M3 – wybór roli zapisany na telefonie.
 *
 * Admin, który sam pracuje („Pracuję jako sieczkarnia / kierowca"), dostaje ekran tej roli – rola na tym telefonie
 * przykrywa rolę ze zbioru. W wersji testowej (debug) tak samo działa „Zmień rolę" – dla każdego, na każdą rolę.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val harvestRepository: HarvestRepository,
    private val workRepository: WorkRepository,
    @SharedHarvest private val sharedHarvest: Boolean,
    @DebugBuild private val debugBuild: Boolean,
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
        // Rola na telefonie (np. wybrana przed M3) przykrywa rolę ze zbioru tylko adminowi, który sam pracuje –
        // poza wersją testową.
        if (sharedHarvest && !debugBuild) {
            viewModelScope.launch {
                val membership = harvestRepository.membership.first { it !is Membership.Loading }
                if ((membership as? Membership.Joined)?.me?.role != Role.ADMIN) sessionRepository.clearRole()
            }
        }
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
                    is Membership.Joined -> {
                        val myRole = membership.me.role
                        val worksAs = roleOverride?.takeIf { debugBuild || (myRole == Role.ADMIN && it in ADMIN_WORKS_AS) }
                        AppUiState.Ready(
                            destination = when {
                                picking && roleOverride == null -> RolePickerDestination
                                else -> (worksAs ?: myRole).toDestination()
                            },
                            backToAdmin = myRole == Role.ADMIN && worksAs != null && worksAs != Role.ADMIN,
                        )
                    }
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

    /** Admin sam pracuje – ekran sieczkarni / kierowcy; inni widzą go w tej roli. */
    fun workAs(role: Role) {
        if (role !in ADMIN_WORKS_AS) return
        pickingRole.value = false
        viewModelScope.launch { sessionRepository.setRole(role) }
    }

    /** „Admin" na ekranie roli – z powrotem panel admina. */
    fun backToAdmin() {
        pickingRole.value = false
        viewModelScope.launch { sessionRepository.clearRole() }
    }

    /** Debug: wraca do wyboru roli. */
    fun switchRole() {
        viewModelScope.launch {
            sessionRepository.clearRole()
            pickingRole.value = true
        }
    }

    private companion object {
        /** Jako kto admin może sam pracować – role, które udostępniają lokalizację. */
        val ADMIN_WORKS_AS = setOf(Role.HARVESTER, Role.DRIVER)
    }
}
