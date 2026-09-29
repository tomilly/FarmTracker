package pl.farmtracker.feature.roles.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.work.WorkRepository
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewWatch
import pl.farmtracker.feature.roles.common.MyPosition
import pl.farmtracker.feature.roles.common.coworkers
import pl.farmtracker.feature.roles.common.myPosition
import javax.inject.Inject

data class DriverUiState(
    val isWorking: Boolean = false,
    val position: MyPosition = MyPosition.Searching,
    /** Gdzie są sieczkarnie – tam się jedzie po ładunek. */
    val harvesters: List<Coworker> = emptyList(),
)

/**
 * Kierowca prowadzi – nie klika statusów kursu. Wystarczy „Zaczynam pracę": resztę (gdzie jest, na którym polu)
 * wie telefon.
 */
@HiltViewModel
class DriverViewModel @Inject constructor(
    private val workRepository: WorkRepository,
    crewWatch: CrewWatch,
) : ViewModel() {

    val uiState: StateFlow<DriverUiState> = combine(workRepository.isWorking, crewWatch.snapshot) { working, crew ->
        DriverUiState(
            isWorking = working,
            position = myPosition(crew.locations, crew.fields, crew.nowMillis),
            harvesters = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.HARVESTER),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriverUiState())

    /** Wołać po zgodzie na lokalizację (pyta o nią ekran). */
    fun startWork() {
        viewModelScope.launch { workRepository.setWorking(true) }
    }

    fun stopWork() {
        viewModelScope.launch { workRepository.setWorking(false) }
    }
}
