package pl.farmtracker.feature.fields.view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.fieldAt
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.map.BaseLayer
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapChromeState
import pl.farmtracker.data.field.FieldRepository
import javax.inject.Inject

sealed interface FieldViewUiState {
    /** Pole jeszcze się wczytuje – UI nie pokazuje niczego. */
    data object Loading : FieldViewUiState

    /** @param allFields wszystkie pola – dla orientacji, co jest obok, i do przełączania dotykiem */
    data class Shown(val field: Field, val allFields: List<Field>) : FieldViewUiState

    /** Pola już nie ma (np. usunięte w edycji) – ekran się zamyka. */
    data object Gone : FieldViewUiState
}

/**
 * Pole na mapie (z listy pól): całe pole na zdjęciu lotniczym, a stąd „Edytuj pole".
 * Dotknięcie innego pola na mapie przełącza na nie – bez wracania do listy.
 * Zmiany z edycji widać od razu – ekran obserwuje zapisane pola.
 */
@HiltViewModel
class FieldViewModel @Inject constructor(
    fieldRepository: FieldRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val fieldId = MutableStateFlow<String>(checkNotNull(savedStateHandle[FIELD_ID_ARG]) { "Brak id pola" })

    /** Na zdjęciu widać, co rośnie na polu i gdzie jest wjazd. */
    val chrome = MapChromeController(MapChromeState(baseLayer = BaseLayer.PHOTO))

    private val _uiState = MutableStateFlow<FieldViewUiState>(FieldViewUiState.Loading)
    val uiState: StateFlow<FieldViewUiState> = _uiState.asStateFlow()

    private var framed = false

    init {
        viewModelScope.launch {
            combine(fieldRepository.fields, fieldId) { fields, id -> fields to fields.firstOrNull { it.id == id } }
                .collect { (fields, field) ->
                    _uiState.value = if (field == null) FieldViewUiState.Gone else FieldViewUiState.Shown(field, fields)
                    // Kadrujemy raz – po powrocie z edycji mapa zostaje tam, gdzie ją zostawiono.
                    if (field != null && !framed) {
                        framed = true
                        chrome.showArea(field.shape)
                    }
                }
        }
    }

    /** Dotknięte inne pole staje się bieżącym; mapa przesuwa się do niego, ale nie przybliża. */
    fun onMapTapped(point: GeoPoint) {
        val state = _uiState.value as? FieldViewUiState.Shown ?: return
        val tapped = state.allFields.fieldAt(point) ?: return
        if (tapped.id == state.field.id) return
        savedStateHandle[FIELD_ID_ARG] = tapped.id
        fieldId.value = tapped.id
        chrome.showArea(tapped.shape, zoomIn = false)
    }

    companion object {
        /** Nazwa argumentu nawigacji z id pola (pole `fieldId` trasy). */
        const val FIELD_ID_ARG = "fieldId"
    }
}
