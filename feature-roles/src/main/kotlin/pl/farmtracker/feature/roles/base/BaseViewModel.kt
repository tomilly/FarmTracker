package pl.farmtracker.feature.roles.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.data.base.BaseRepository
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewWatch
import pl.farmtracker.feature.roles.common.coworkers
import pl.farmtracker.feature.roles.common.showWholeHarvest
import pl.farmtracker.feature.roles.common.toWorkMapOverlays
import javax.inject.Inject

data class BaseUiState(
    val harvesters: List<Coworker> = emptyList(),
    /** Kierowcy w kolejności „kto zaraz przyjedzie": z ładunkiem najbliżej bazy na górze, bez pozycji na końcu. */
    val drivers: List<Coworker> = emptyList(),
)

/**
 * Baza stoi w miejscu – nie udostępnia lokalizacji, tylko patrzy, kto przyjeżdża: mapa z całym zbiorem, a pod nią
 * kierowcy, z ładunkiem najbliżej na górze.
 */
@HiltViewModel
class BaseViewModel @Inject constructor(crewWatch: CrewWatch, baseRepository: BaseRepository) : ViewModel() {

    val chrome = MapChromeController()

    private val basePoint = baseRepository.base.map { it?.location }

    val uiState: StateFlow<BaseUiState> = combine(crewWatch.snapshot, basePoint) { crew, base ->
        BaseUiState(
            harvesters = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.HARVESTER),
            drivers = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.DRIVER, from = base)
                .sortedWith(ArrivingFirst),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BaseUiState())

    val overlays: StateFlow<MapOverlays> = combine(crewWatch.snapshot, basePoint) { crew, base ->
        crew.toWorkMapOverlays(base) { false }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapOverlays())

    init {
        viewModelScope.launch { chrome.showWholeHarvest(crewWatch.snapshot.map { it.fields }, basePoint) }
    }

    private companion object {
        /** Wraca z ładunkiem przed resztą, potem kto bliżej; bez świeżej pozycji – na końcu. */
        val ArrivingFirst = compareBy<Coworker>(
            { it.isStale },
            { it.trip != Trip.TO_BASE },
            { it.distanceMeters ?: Double.MAX_VALUE },
            { it.name },
        )
    }
}
