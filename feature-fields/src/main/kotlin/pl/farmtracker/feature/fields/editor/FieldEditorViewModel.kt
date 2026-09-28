package pl.farmtracker.feature.fields.editor

import androidx.lifecycle.SavedStateHandle
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
import pl.farmtracker.core.domain.geo.GeoArea
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
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
import pl.farmtracker.feature.fields.common.DeletedFieldBin
import java.util.UUID
import javax.inject.Inject

enum class EditorStep { SHAPE, SEARCH, DETAILS, ENTRY }

/** Jak powstaje kształt pola: z działek ewidencyjnych albo narysowany po rogach. */
enum class ShapeMode { PARCELS, DRAW }

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
    val shapeMode: ShapeMode = ShapeMode.PARCELS,
    /** Działki wybrane do pola, w kolejności dotykania (ostatnia = do „Cofnij"). */
    val parcels: List<Parcel> = emptyList(),
    /** Rogi rysowanego pola, w kolejności dotykania. */
    val drawnPoints: List<GeoPoint> = emptyList(),
    val pendingLookups: Int = 0,
    val lastProblem: LookupProblem? = null,
    val searchQuery: String = "",
    val search: SearchState = SearchState.Idle,
    val name: String = "",
    val color: FieldColor = FieldColor.BLUE,
    val entryPoint: GeoPoint? = null,
    /** Domyślna nazwa i kolor ustawiane tylko przy pierwszym wejściu do formularza. */
    val detailsPrefilled: Boolean = false,
    /** Edytowane istniejące pole (`null` przy tworzeniu nowego). */
    val editing: Field? = null,
    /** Ekran skończył pracę (zapisano, usunięto albo pola już nie ma) – wracamy do listy. */
    val done: Boolean = false,
    /** Czekamy na wczytanie edytowanego pola – UI nie pokazuje jeszcze niczego. */
    val loadingField: Boolean = false,
) {
    val isEditing: Boolean get() = editing != null

    /** Kształt pola, który zostanie zapisany. Rysunek liczy się od trzech rogów. */
    val fieldShape: List<GeoPolygon>
        get() = editing?.shape ?: when (shapeMode) {
            ShapeMode.PARCELS -> parcels.flatMap { it.shape }
            ShapeMode.DRAW -> if (drawnPoints.size >= MIN_CORNERS) listOf(GeoPolygon(drawnPoints)) else emptyList()
        }

    val areaHectares: Double get() = GeoArea.hectares(fieldShape)
    val canContinue: Boolean get() = fieldShape.isNotEmpty()
    val canUndo: Boolean get() = if (shapeMode == ShapeMode.DRAW) drawnPoints.isNotEmpty() else parcels.isNotEmpty()
    val canSave: Boolean get() = canContinue && name.isNotBlank()

    companion object {
        const val MIN_CORNERS = 3
    }
}

/**
 * Tworzenie pola: 1) na mapie wybiera się działki (dotykiem albo po numerze) albo rysuje rogi,
 * 2) nazwa, kolor i opcjonalnie wjazd. Nic nie jest zapisywane przed „Zapisz pole".
 *
 * Edycja istniejącego pola (argument [FIELD_ID_ARG]) zaczyna od razu od kroku 2; kształt zostaje.
 */
@HiltViewModel
class FieldEditorViewModel @Inject constructor(
    private val parcelRepository: ParcelRepository,
    private val fieldRepository: FieldRepository,
    private val deletedFieldBin: DeletedFieldBin,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private fun loadForEditing(fieldId: String) {
        viewModelScope.launch {
            val field = fieldRepository.fields.first().firstOrNull { it.id == fieldId }
            _uiState.update { state ->
                if (field == null) {
                    state.copy(done = true, loadingField = false)
                } else {
                    state.copy(
                        step = EditorStep.DETAILS,
                        editing = field,
                        name = field.name,
                        color = field.color,
                        entryPoint = field.entryPoint,
                        detailsPrefilled = true,
                        loadingField = false,
                    )
                }
            }
        }
    }

    /** Do wyznaczania pola od razu zdjęcie lotnicze i granice działek. */
    val chrome = MapChromeController(MapChromeState(baseLayer = BaseLayer.PHOTO, showParcels = true))

    /** Istniejące pola – dla orientacji na mapie i do wyboru wolnego koloru. */
    val existingFields: StateFlow<List<Field>> = fieldRepository.fields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(
        FieldEditorUiState(loadingField = savedStateHandle.get<String>(FIELD_ID_ARG) != null),
    )
    val uiState: StateFlow<FieldEditorUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    // Po deklaracji stanu – wczytanie może zaktualizować stan od razu, bez zawieszania.
    init {
        savedStateHandle.get<String>(FIELD_ID_ARG)?.let(::loadForEditing)
    }

    fun onMapTapped(point: GeoPoint) {
        val state = _uiState.value
        when {
            state.step == EditorStep.ENTRY -> _uiState.update { it.copy(entryPoint = point) }
            state.step != EditorStep.SHAPE -> Unit
            state.shapeMode == ShapeMode.DRAW -> _uiState.update { it.copy(drawnPoints = it.drawnPoints + point) }
            chrome.state.value.parcelsVisible -> toggleParcelAt(point)
        }
    }

    /** Dotknięcie działki dodaje ją do pola; dotknięcie już wybranej – usuwa (bez pytania serwera). */
    private fun toggleParcelAt(point: GeoPoint) {
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
                    is ParcelLookup.Found -> state.copy(pendingLookups = pending, parcels = state.parcels.plusUnique(result.parcel))
                    ParcelLookup.NotFound -> state.copy(pendingLookups = pending, lastProblem = LookupProblem.NOT_FOUND)
                    ParcelLookup.Unavailable -> state.copy(pendingLookups = pending, lastProblem = LookupProblem.UNAVAILABLE)
                }
            }
        }
    }

    /** Rysowanie ręczne – gdy pole nie pokrywa się z działkami (np. część działki). */
    fun startDrawing() = _uiState.update {
        if (it.parcels.isEmpty()) it.copy(shapeMode = ShapeMode.DRAW, lastProblem = null) else it
    }

    /** Powrót do wybierania działek; narysowane rogi są porzucane. */
    fun stopDrawing() = _uiState.update { it.copy(shapeMode = ShapeMode.PARCELS, drawnPoints = emptyList()) }

    /** „Cofnij" – usuwa ostatnio dodaną działkę albo ostatni róg. */
    fun undoLast() = _uiState.update {
        when (it.shapeMode) {
            ShapeMode.PARCELS -> it.copy(parcels = it.parcels.dropLast(1), lastProblem = null)
            ShapeMode.DRAW -> it.copy(drawnPoints = it.drawnPoints.dropLast(1))
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
                shapeMode = ShapeMode.PARCELS,
                drawnPoints = emptyList(),
                parcels = state.parcels.plusUnique(parcel),
                lastProblem = null,
            )
        }
        chrome.showArea(parcel.shape)
    }

    fun goToDetails() {
        if (!_uiState.value.canContinue) return
        viewModelScope.launch {
            val usedColors = fieldRepository.fields.first().map { it.color }.toSet()
            _uiState.update { state ->
                if (state.detailsPrefilled) {
                    state.copy(step = EditorStep.DETAILS)
                } else {
                    state.copy(
                        step = EditorStep.DETAILS,
                        name = state.name.ifBlank { suggestedName(state) },
                        color = firstFreeColor(usedColors),
                        detailsPrefilled = true,
                    )
                }
            }
        }
    }

    fun backToShape() = _uiState.update { it.copy(step = EditorStep.SHAPE) }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun onColorSelected(color: FieldColor) = _uiState.update { it.copy(color = color) }

    /** Zaznaczanie wjazdu na mapie – mapa pokazuje całe nowe pole. */
    fun openEntry() {
        _uiState.update { it.copy(step = EditorStep.ENTRY) }
        chrome.showArea(_uiState.value.fieldShape)
    }

    fun closeEntry() = _uiState.update { it.copy(step = EditorStep.DETAILS) }

    fun clearEntry() = _uiState.update { it.copy(entryPoint = null) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.done) return
        viewModelScope.launch {
            val field = state.editing?.copy(
                name = state.name.trim(),
                color = state.color,
                entryPoint = state.entryPoint,
            ) ?: Field(
                id = UUID.randomUUID().toString(),
                name = state.name.trim(),
                color = state.color,
                shape = state.fieldShape,
                parcelIds = if (state.shapeMode == ShapeMode.PARCELS) state.parcels.map { it.id } else emptyList(),
                entryPoint = state.entryPoint,
                order = (fieldRepository.fields.first().maxOfOrNull { it.order } ?: -1) + 1,
            )
            fieldRepository.save(field)
            _uiState.update { it.copy(done = true) }
        }
    }

    /** Usuwa od razu – bez „czy na pewno?"; lista pól pozwoli to cofnąć. */
    fun delete() {
        val field = _uiState.value.editing ?: return
        if (_uiState.value.done) return
        viewModelScope.launch {
            deletedFieldBin.put(field)
            fieldRepository.delete(field.id)
            _uiState.update { it.copy(done = true) }
        }
    }

    companion object {
        /** Nazwa argumentu nawigacji z id edytowanego pola (pole `fieldId` trasy). */
        const val FIELD_ID_ARG = "fieldId"

        private fun List<Parcel>.plusUnique(parcel: Parcel): List<Parcel> =
            if (any { it.id == parcel.id }) this else this + parcel

        private fun Parcel.distanceKmFrom(point: GeoPoint): Double? =
            shape.bounds()?.center?.distanceMetersTo(point)?.div(1000.0)

        /** Podpowiedź nazwy: obręb i numer pierwszej działki, np. „Bystrzyca 2285" – łatwo zmienić. */
        private fun suggestedName(state: FieldEditorUiState): String = when (state.shapeMode) {
            ShapeMode.PARCELS -> state.parcels.firstOrNull()?.let { "${it.precinct} ${it.number}" }.orEmpty()
            ShapeMode.DRAW -> ""
        }

        private fun firstFreeColor(used: Set<FieldColor>): FieldColor =
            FieldColor.entries.firstOrNull { it !in used } ?: FieldColor.entries.first()
    }
}
