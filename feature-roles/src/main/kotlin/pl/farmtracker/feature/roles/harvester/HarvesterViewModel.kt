package pl.farmtracker.feature.roles.harvester

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HarvesterUiState(
    val isWorking: Boolean = false,
    val isMoving: Boolean = false,
)

/**
 * M0: stan trzymany tylko lokalnie. W M4 „Zaczynam pracę" włączy udostępnianie lokalizacji,
 * a pole wykryje geofencing.
 */
@HiltViewModel
class HarvesterViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HarvesterUiState())
    val uiState: StateFlow<HarvesterUiState> = _uiState.asStateFlow()

    fun startWork() = _uiState.update { it.copy(isWorking = true) }

    fun stopWork() = _uiState.update { HarvesterUiState() }

    fun startMoving() = _uiState.update { if (it.isWorking) it.copy(isMoving = true) else it }

    fun arrivedAtField() = _uiState.update { it.copy(isMoving = false) }
}
