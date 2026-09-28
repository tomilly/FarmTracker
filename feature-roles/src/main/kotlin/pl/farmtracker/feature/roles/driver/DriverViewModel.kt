package pl.farmtracker.feature.roles.driver

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.farmtracker.core.domain.DriverState
import javax.inject.Inject

data class DriverUiState(
    val state: DriverState = DriverState.IDLE,
    /** Stan sprzed ostatniej zmiany – pozwala cofnąć pomyłkowe kliknięcie (BRIEF §4). */
    val previousState: DriverState? = null,
) {
    val canUndo: Boolean get() = previousState != null
}

/** M0: status tylko lokalnie. W M5 będzie wysyłany do bazy i sieczkarni. */
@HiltViewModel
class DriverViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DriverUiState())
    val uiState: StateFlow<DriverUiState> = _uiState.asStateFlow()

    fun selectState(newState: DriverState) = _uiState.update { current ->
        if (current.state == newState) current else DriverUiState(state = newState, previousState = current.state)
    }

    fun undo() = _uiState.update { current ->
        current.previousState?.let { DriverUiState(state = it) } ?: current
    }
}
