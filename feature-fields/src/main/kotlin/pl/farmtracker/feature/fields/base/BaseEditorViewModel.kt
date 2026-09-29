package pl.farmtracker.feature.fields.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Base
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.map.BaseLayer
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapChromeState
import pl.farmtracker.data.base.BaseRepository
import pl.farmtracker.data.field.FieldRepository
import javax.inject.Inject

data class BaseEditorUiState(
    /** Czekamy na zapisaną bazę – UI nie pokazuje jeszcze mapy. */
    val loading: Boolean = true,
    val saved: GeoPoint? = null,
    /** Pinezka bazy na mapie (jeszcze niezapisana); `null` – bazy nie ma. */
    val location: GeoPoint? = null,
    /** Zapisano – ekran się zamyka. */
    val done: Boolean = false,
) {
    val canSave: Boolean get() = !loading && location != saved
}

/**
 * Baza (silos / pryzma) na mapie: dotknięcie stawia albo przesuwa bazę, „Zapisz bazę" ją zapamiętuje.
 * Wyjście bez zapisu niczego nie zmienia – dlatego bez pytań „czy na pewno?".
 */
@HiltViewModel
class BaseEditorViewModel @Inject constructor(
    private val baseRepository: BaseRepository,
    fieldRepository: FieldRepository,
) : ViewModel() {

    /** Na zdjęciu widać silosy i place – łatwiej trafić. */
    val chrome = MapChromeController(MapChromeState(baseLayer = BaseLayer.PHOTO))

    /** Pola – dla orientacji, gdzie baza jest względem nich. */
    val fields: StateFlow<List<Field>> = fieldRepository.fields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(BaseEditorUiState())
    val uiState: StateFlow<BaseEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = baseRepository.base.first()?.location
            // Zapisana baza na środku mapy; bez niej mapa pokaże „mnie" – admin zwykle ustawia bazę u siebie.
            saved?.let { chrome.showPlace(it, MapChromeController.CLOSE_ZOOM) }
            _uiState.update { it.copy(loading = false, saved = saved, location = saved) }
        }
    }

    fun onMapTapped(point: GeoPoint) {
        if (_uiState.value.loading) return
        _uiState.update { it.copy(location = point) }
    }

    fun removeBase() = _uiState.update { it.copy(location = null) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.done) return
        viewModelScope.launch {
            val location = state.location
            if (location == null) baseRepository.clear() else baseRepository.save(Base(location))
            _uiState.update { it.copy(done = true) }
        }
    }
}
