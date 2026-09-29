package pl.farmtracker.feature.roles.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.DriverState
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.work.WorkRepository
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewWatch
import pl.farmtracker.feature.roles.common.MyPosition
import pl.farmtracker.feature.roles.common.coworkers
import pl.farmtracker.feature.roles.common.myPosition
import javax.inject.Inject

data class DriverUiState(
    val state: DriverState = DriverState.IDLE,
    /** Stan sprzed ostatniej zmiany – pozwala cofnąć pomyłkowe kliknięcie (BRIEF §4). */
    val previousState: DriverState? = null,
    val isWorking: Boolean = false,
    val position: MyPosition = MyPosition.Searching,
    /** Gdzie są sieczkarnie – tam się jedzie po ładunek. */
    val harvesters: List<Coworker> = emptyList(),
) {
    val canUndo: Boolean get() = previousState != null
}

private data class DriverStatus(val state: DriverState = DriverState.IDLE, val previousState: DriverState? = null)

/** Status kursu na razie tylko na telefonie (w M5 pójdzie do bazy i sieczkarni); pozycja – od „Zaczynam pracę". */
@HiltViewModel
class DriverViewModel @Inject constructor(
    private val workRepository: WorkRepository,
    crewWatch: CrewWatch,
) : ViewModel() {

    private val status = MutableStateFlow(DriverStatus())

    val uiState: StateFlow<DriverUiState> =
        combine(status, workRepository.isWorking, crewWatch.snapshot) { status, working, crew ->
            DriverUiState(
                state = status.state,
                previousState = status.previousState,
                isWorking = working,
                position = myPosition(crew.locations, crew.fields, crew.nowMillis),
                harvesters = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.HARVESTER),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriverUiState())

    fun selectState(newState: DriverState) = status.update { current ->
        if (current.state == newState) current else DriverStatus(state = newState, previousState = current.state)
    }

    fun undo() = status.update { current ->
        current.previousState?.let { DriverStatus(state = it) } ?: current
    }

    /** Wołać po zgodzie na lokalizację (pyta o nią ekran). */
    fun startWork() {
        viewModelScope.launch { workRepository.setWorking(true) }
    }

    fun stopWork() {
        viewModelScope.launch { workRepository.setWorking(false) }
    }
}
