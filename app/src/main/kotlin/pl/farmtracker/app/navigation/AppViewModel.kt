package pl.farmtracker.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.data.session.SessionRepository
import javax.inject.Inject

sealed interface AppUiState {
    data object Loading : AppUiState

    data class Ready(val destination: AppDestination) : AppUiState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = sessionRepository.currentRole
        .map<_, AppUiState> { role -> AppUiState.Ready(role.toDestination()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    /** Tymczasowe (debug): wraca do wyboru roli. */
    fun switchRole() {
        viewModelScope.launch { sessionRepository.clearRole() }
    }
}
