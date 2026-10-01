package pl.farmtracker.feature.roles.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.session.PeopleRepository
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewWatch
import pl.farmtracker.feature.roles.common.coworkers
import javax.inject.Inject

data class AdminUiState(
    val harvesters: List<Coworker> = emptyList(),
    /** Kierowcy z tym, co robią (ładuje, wraca do bazy…) i od kiedy stoją – wszystko z ich lokalizacji. */
    val drivers: List<Coworker> = emptyList(),
    /** Sieczkarnie i kierowcy ze zbioru, którzy nie zaczęli pracy (albo ich telefon zgasł dawno temu), po imieniu. */
    val notWorking: List<String> = emptyList(),
)

/** Admin – podgląd całej pracy na górze ekranu, pod nim menu zbioru. */
@HiltViewModel
class AdminViewModel @Inject constructor(crewWatch: CrewWatch, peopleRepository: PeopleRepository) : ViewModel() {

    val uiState: StateFlow<AdminUiState> = combine(crewWatch.snapshot, peopleRepository.members) { crew, members ->
        val working = crew.locations.filterNot { it.isGoneAt(crew.nowMillis) }.map { it.userId }.toSet()
        AdminUiState(
            harvesters = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.HARVESTER),
            drivers = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.DRIVER),
            notWorking = members
                .filter { it.role in WORKING_ROLES && it.userId !in working }
                .map { it.name }
                .sorted(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUiState())

    private companion object {
        /** Kto w ogóle „zaczyna pracę" – baza stoi w miejscu, a admin tylko patrzy. */
        val WORKING_ROLES = setOf(Role.HARVESTER, Role.DRIVER)
    }
}
