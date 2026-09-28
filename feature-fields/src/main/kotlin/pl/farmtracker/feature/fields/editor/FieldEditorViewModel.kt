package pl.farmtracker.feature.fields.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.bounds
import pl.farmtracker.core.domain.geo.containsPoint
import pl.farmtracker.core.domain.geo.distanceMetersTo
import pl.farmtracker.core.map.BaseLayer
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapChromeState
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.parcel.ParcelLookup
import pl.farmtracker.data.parcel.ParcelRepository
import pl.farmtracker.data.parcel.ParcelSearch
import java.util.UUID
import javax.inject.Inject

enum class EditorStep { SHAPE, SEARCH, DETAILS }

enum class LookupProblem { NOT_FOUND, UNAVAILABLE }

/** Wynik wyszukiwania z odległością od środka mapy (gdy znany) – najbliższe na górze. */
data class SearchHit(val parcel: Parcel, val distanceKm: Double?)

sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Results(val hits: List<SearchHit>) : SearchState
    data object NotFound : SearchState
    data object Unavailable : SearchState
}

data class FieldEditorUiState(
    val step: EditorStep = EditorStep.SHAPE,
    /** Działki wybrane do pola, w kolejności dotykania (ostatnia = do „Cofnij"). */
    val parcels: List<Parcel> = emptyList(),
    val pendingLookups: Int = 0,
    val lastProblem: LookupProblem? = null,
    val searchQuery: String = "",
    val search: SearchState = SearchState.Idle,
    val name: String = "",
    val color: FieldColor = FieldColor.BLUE,
    val saved: Boolean = false,
) {
    val areaHectares: Double get() = parcels.sumOf { it.areaHectares }
    val canContinue: Boolean get() = parcels.isNotEmpty()
    val canSave: Boolean get() = parcels.isNotEmpty() && name.isNotBlank()
}

/**
 * Tworzenie pola w dwóch krokach: 1) na mapie dotyka się działek, które tworzą pole,
 * 2) nazwa i kolor. Nic nie jest zapisywane przed „Zapisz pole".
 */
@HiltViewModel
class FieldEditorViewModel @Inject constructor(
    private val parcelRepository: ParcelRepository,
    private val fieldRepository: FieldRepository,
) : ViewModel() {

    /** Do wyznaczania pola od razu zdjęcie lotnicze i granice działek. */
    val chrome = MapChromeController(MapChromeState(baseLayer = BaseLayer.PHOTO, showParcels = true))

    /** Istniejące pola – dla orientacji na mapie i do wyboru wolnego koloru. */
    val existingFields: StateFlow<List<Field>> = fieldRepository.fields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(FieldEditorUiState())
    val uiState: StateFlow<FieldEditorUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Dotknięcie działki dodaje ją do pola; dotknięcie już wybranej – usuwa. */
    fun onMapTapped(point: GeoPoint) {
        if (_uiState.value.step != EditorStep.SHAPE || !chrome.state.value.parcelsVisible) return

        val tappedSelected = _uiState.value.parcels.firstOrNull { it.shape.containsPoint(point) }
        if (tappedSelected != null) {
            _uiState.update { state -> state.copy(parcels = state.parcels - tappedSelected, lastProblem = null) }
            return
        }

        _uiState.update { it.copy(pendingLookups = it.pendingLookups + 1, lastProblem = null) }
        viewModelScope.launch {
            val result = parcelRepository.parcelAt(point)
            _uiState.update { state ->
                val pending = state.pendingLookups - 1
                when (result) {
                    is ParcelLookup.Found -> state.copy(
                        pendingLookups = pending,
                        parcels = if (state.parcels.any { it.id == result.parcel.id }) {
                            state.parcels
                        } else {
                            state.parcels + result.parcel
                        },
                    )
                    ParcelLookup.NotFound -> state.copy(pendingLookups = pending, lastProblem = LookupProblem.NOT_FOUND)
                    ParcelLookup.Unavailable -> state.copy(pendingLookups = pending, lastProblem = LookupProblem.UNAVAILABLE)
                }
            }
        }
    }

    fun openSearch() = _uiState.update { it.copy(step = EditorStep.SEARCH) }

    fun closeSearch() = _uiState.update { it.copy(step = EditorStep.SHAPE) }

    fun onSearchQueryChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun runSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        _uiState.update { it.copy(search = SearchState.Searching) }
        searchJob = viewModelScope.launch {
            val center = chrome.state.value.center
            val search = when (val result = parcelRepository.search(query)) {
                is ParcelSearch.Found -> SearchState.Results(
                    result.parcels
                        .map { parcel -> SearchHit(parcel, center?.let { parcel.distanceKmFrom(it) }) }
                        .sortedBy { it.distanceKm ?: Double.MAX_VALUE },
                )
                ParcelSearch.NotFound -> SearchState.NotFound
                ParcelSearch.Unavailable -> SearchState.Unavailable
            }
            _uiState.update { it.copy(search = search) }
        }
    }

    /** Wybrana z wyników działka trafia do pola, a mapa pokazuje ją w całości. */
    fun pickSearchResult(parcel: Parcel) {
        _uiState.update { state ->
            state.copy(
                step = EditorStep.SHAPE,
                parcels = if (state.parcels.any { it.id == parcel.id }) state.parcels else state.parcels + parcel,
                lastProblem = null,
            )
        }
        chrome.showArea(parcel.shape)
    }

    /** „Cofnij" – usuwa ostatnio dodaną działkę. */
    fun removeLastParcel() = _uiState.update { it.copy(parcels = it.parcels.dropLast(1), lastProblem = null) }

    fun goToDetails() {
        if (!_uiState.value.canContinue) return
        viewModelScope.launch {
            val usedColors = fieldRepository.fields.first().map { it.color }.toSet()
            _uiState.update { state ->
                state.copy(
                    step = EditorStep.DETAILS,
                    name = state.name.ifBlank { suggestedName(state.parcels) },
                    color = if (state.name.isBlank()) firstFreeColor(usedColors) else state.color,
                )
            }
        }
    }

    fun backToShape() = _uiState.update { it.copy(step = EditorStep.SHAPE) }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun onColorSelected(color: FieldColor) = _uiState.update { it.copy(color = color) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.saved) return
        viewModelScope.launch {
            val existing = fieldRepository.fields.first()
            fieldRepository.save(
                Field(
                    id = UUID.randomUUID().toString(),
                    name = state.name.trim(),
                    color = state.color,
                    shape = state.parcels.flatMap { it.shape },
                    parcelIds = state.parcels.map { it.id },
                    order = (existing.maxOfOrNull { it.order } ?: -1) + 1,
                ),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

    private companion object {
        fun Parcel.distanceKmFrom(point: GeoPoint): Double? =
            shape.bounds()?.center?.distanceMetersTo(point)?.div(1000.0)

        /** Podpowiedź nazwy: obręb i numer pierwszej działki, np. „Bystrzyca 2285" – łatwo zmienić. */
        fun suggestedName(parcels: List<Parcel>): String =
            parcels.firstOrNull()?.let { "${it.precinct} ${it.number}" }.orEmpty()

        fun firstFreeColor(used: Set<FieldColor>): FieldColor =
            FieldColor.entries.firstOrNull { it !in used } ?: FieldColor.entries.first()
    }
}
