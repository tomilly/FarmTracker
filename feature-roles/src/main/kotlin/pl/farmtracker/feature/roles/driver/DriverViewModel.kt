package pl.farmtracker.feature.roles.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.core.map.toMapPeople
import pl.farmtracker.data.base.BaseRepository
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
 * Kierowca prowadzi – nie klika statusów kursu. „Zaczynam pracę" pokazuje mapę, która jedzie za kierowcą,
 * z sieczkarnią i innymi pracującymi; resztę wie telefon.
 */
@HiltViewModel
class DriverViewModel @Inject constructor(
    private val workRepository: WorkRepository,
    crewWatch: CrewWatch,
    baseRepository: BaseRepository,
) : ViewModel() {

    /** Mapa w czasie pracy. */
    val chrome = MapChromeController()

    val uiState: StateFlow<DriverUiState> = combine(workRepository.isWorking, crewWatch.snapshot) { working, crew ->
        DriverUiState(
            isWorking = working,
            position = myPosition(crew.locations, crew.fields, crew.nowMillis, withTrip = true),
            harvesters = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.HARVESTER),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriverUiState())

    /** Pola, wjazdy, baza i inni pracujący – na mapie w czasie pracy. */
    val overlays: StateFlow<MapOverlays> = combine(crewWatch.snapshot, baseRepository.base) { crew, base ->
        MapOverlays(
            fields = crew.fields,
            entryPoints = crew.fields.flatMap { it.entryPoints },
            base = base?.location,
            people = crew.locations.toMapPeople(crew.nowMillis),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapOverlays())

    init {
        // Każdy początek pracy (także sprzed zamknięcia aplikacji): mapa pokazuje kierowcę i jedzie za nim.
        viewModelScope.launch {
            workRepository.isWorking.distinctUntilChanged().filter { it }.collect { chrome.followMe() }
        }
    }

    /** Wołać po zgodzie na lokalizację (pyta o nią ekran). */
    fun startWork() {
        viewModelScope.launch { workRepository.setWorking(true) }
    }

    fun stopWork() {
        viewModelScope.launch { workRepository.setWorking(false) }
    }
}
