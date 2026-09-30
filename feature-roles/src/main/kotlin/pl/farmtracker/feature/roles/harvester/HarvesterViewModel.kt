package pl.farmtracker.feature.roles.harvester

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
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.data.base.BaseRepository
import pl.farmtracker.data.work.WorkRepository
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewWatch
import pl.farmtracker.feature.roles.common.MyPosition
import pl.farmtracker.feature.roles.common.coworkers
import pl.farmtracker.feature.roles.common.myPosition
import pl.farmtracker.feature.roles.common.toWorkMapOverlays
import javax.inject.Inject

data class HarvesterUiState(
    val isWorking: Boolean = false,
    /** Na którym polu jestem – wykrywa telefon, bez przycisków „Jestem na polu" / „Przejeżdżam". */
    val position: MyPosition = MyPosition.Searching,
    val drivers: List<Coworker> = emptyList(),
)

/**
 * „Zaczynam pracę" włącza udostępnianie lokalizacji (usługa `WorkService`), a pole wykrywa geofencing.
 * W pracy – jak u kierowcy – mapa na cały ekran jedzie za sieczkarnią i pokazuje nadjeżdżające przyczepy.
 */
@HiltViewModel
class HarvesterViewModel @Inject constructor(
    private val workRepository: WorkRepository,
    crewWatch: CrewWatch,
    baseRepository: BaseRepository,
) : ViewModel() {

    /** Mapa w czasie pracy. */
    val chrome = MapChromeController()

    val uiState: StateFlow<HarvesterUiState> = combine(workRepository.isWorking, crewWatch.snapshot) { working, crew ->
        HarvesterUiState(
            isWorking = working,
            position = myPosition(crew.locations, crew.fields, crew.nowMillis),
            drivers = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.DRIVER),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HarvesterUiState())

    /** Pola, wjazdy, baza i inni pracujący – na mapie w czasie pracy. */
    val overlays: StateFlow<MapOverlays> = combine(crewWatch.snapshot, baseRepository.base) { crew, base ->
        // Na ekranie przyczepy, które jadą do mnie albo już są na polu – nie te w drodze do bazy czy w bazie.
        crew.toWorkMapOverlays(base?.location) { it.role == Role.DRIVER && it.trip !in AWAY_FROM_HARVESTER }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapOverlays())

    init {
        // Każdy początek pracy (także sprzed zamknięcia aplikacji): mapa pokazuje sieczkarnię i jedzie za nią.
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

    private companion object {
        val AWAY_FROM_HARVESTER = setOf(Trip.TO_BASE, Trip.AT_BASE)
    }
}
