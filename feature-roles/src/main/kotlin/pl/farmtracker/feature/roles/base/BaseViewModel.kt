package pl.farmtracker.feature.roles.base

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class BaseUiState(
    /** Kierowcy wiozący ładunek do bazy (M0: zawsze pusto; dane na żywo od M4–M5). */
    val incomingDrivers: List<String> = emptyList(),
)

@HiltViewModel
class BaseViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(BaseUiState())
    val uiState: StateFlow<BaseUiState> = _uiState.asStateFlow()
}
