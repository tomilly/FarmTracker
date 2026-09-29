package pl.farmtracker.feature.roles.harvester

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

data class HarvesterUiState(
    val isWorking: Boolean = false,
    /** Na którym polu jestem – wykrywa telefon, bez przycisków „Jestem na polu" / „Przejeżdżam". */
    val position: MyPosition = MyPosition.Searching,
    val drivers: List<Coworker> = emptyList(),
)

/** „Zaczynam pracę" włącza udostępnianie lokalizacji (usługa `WorkService`), a pole wykrywa geofencing. */
@HiltViewModel
class HarvesterViewModel @Inject constructor(
    private val workRepository: WorkRepository,
    crewWatch: CrewWatch,
) : ViewModel() {

    val uiState: StateFlow<HarvesterUiState> = combine(workRepository.isWorking, crewWatch.snapshot) { working, crew ->
        HarvesterUiState(
            isWorking = working,
            position = myPosition(crew.locations, crew.fields, crew.nowMillis),
            drivers = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.DRIVER),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HarvesterUiState())

    /** Wołać po zgodzie na lokalizację (pyta o nią ekran). */
    fun startWork() {
        viewModelScope.launch { workRepository.setWorking(true) }
    }

    fun stopWork() {
        viewModelScope.launch { workRepository.setWorking(false) }
    }
}
