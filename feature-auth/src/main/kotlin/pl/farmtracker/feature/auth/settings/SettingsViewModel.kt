package pl.farmtracker.feature.auth.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.auth.AuthRepository
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.session.SessionRepository
import pl.farmtracker.data.work.WorkRepository
import javax.inject.Inject

data class SettingsUiState(
    val name: String = "",
    val phone: String = "",
    val harvestName: String = "",
    val role: Role? = null,
    val signingOut: Boolean = false,
)

/** „Ustawienia": kim jestem, w którym zbiorze – i „Wyloguj się". */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    harvestRepository: HarvestRepository,
    private val workRepository: WorkRepository,
    private val liveLocationRepository: LiveLocationRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val signingOut = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(harvestRepository.membership, signingOut) { membership, busy ->
        val joined = membership as? Membership.Joined
        SettingsUiState(
            name = joined?.me?.name.orEmpty(),
            phone = joined?.me?.phone.orEmpty(),
            harvestName = joined?.harvest?.name.orEmpty(),
            role = joined?.me?.role,
            signingOut = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /**
     * Najpierw kończy pracę i znika z mapy innych (póki jeszcze wolno zapisać swoją pozycję), potem wylogowuje.
     * Aplikacja sama wraca do logowania – obserwuje stan logowania.
     */
    fun signOut() {
        if (signingOut.value) return
        signingOut.value = true
        viewModelScope.launch {
            workRepository.setWorking(false)
            liveLocationRepository.stopSharing()
            // Rola wybrana w wersji testowej („Zmień rolę") nie może przejść na następną osobę.
            sessionRepository.clearRole()
            authRepository.signOut()
        }
    }
}
