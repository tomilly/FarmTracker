package pl.farmtracker.feature.roles.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.farmtracker.core.domain.Role
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewWatch
import pl.farmtracker.feature.roles.common.coworkers
import javax.inject.Inject

data class BaseUiState(
    val harvesters: List<Coworker> = emptyList(),
    /** Gdzie są kierowcy; kto wiezie ładunek – w M5 (statusy kierowców). */
    val drivers: List<Coworker> = emptyList(),
)

/** Baza stoi w miejscu – nie udostępnia lokalizacji, tylko widzi sieczkarnię i kierowców. */
@HiltViewModel
class BaseViewModel @Inject constructor(crewWatch: CrewWatch) : ViewModel() {

    val uiState: StateFlow<BaseUiState> = crewWatch.snapshot
        .map { crew ->
            BaseUiState(
                harvesters = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.HARVESTER),
                drivers = coworkers(crew.locations, crew.fields, crew.nowMillis, Role.DRIVER),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BaseUiState())
}
