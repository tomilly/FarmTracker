package pl.farmtracker.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.parcel.ParcelLookup
import pl.farmtracker.data.parcel.ParcelRepository
import javax.inject.Inject

/** Co się dzieje z dotkniętą działką. */
sealed interface ParcelSelection {
    data object None : ParcelSelection
    data object Searching : ParcelSelection
    data class Selected(val parcel: Parcel) : ParcelSelection
    data object NotFound : ParcelSelection
    data object Unavailable : ParcelSelection
}

val ParcelSelection.selectedParcel: Parcel? get() = (this as? ParcelSelection.Selected)?.parcel

@HiltViewModel
class MapViewModel @Inject constructor(
    private val parcelRepository: ParcelRepository,
    fieldRepository: FieldRepository,
) : ViewModel() {

    val chrome = MapChromeController()

    /** Pola zbioru – widoczne na mapie dla każdej roli. */
    val fields: StateFlow<List<Field>> = fieldRepository.fields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _parcelSelection = MutableStateFlow<ParcelSelection>(ParcelSelection.None)
    val parcelSelection: StateFlow<ParcelSelection> = _parcelSelection.asStateFlow()

    private var parcelLookup: Job? = null

    init {
        // Ukrycie warstwy działek chowa też zaznaczenie – inaczej żółta plama zostaje bez kontekstu.
        viewModelScope.launch {
            chrome.state.map { it.showParcels }.distinctUntilChanged().filter { !it }.collect {
                clearParcelSelection()
            }
        }
    }

    /** Dotknięcie mapy zaznacza działkę – tylko gdy granice działek są widoczne. */
    fun onMapTapped(point: GeoPoint) {
        if (!chrome.state.value.parcelsVisible) return
        parcelLookup?.cancel()
        _parcelSelection.value = ParcelSelection.Searching
        parcelLookup = viewModelScope.launch {
            _parcelSelection.value = when (val result = parcelRepository.parcelAt(point)) {
                is ParcelLookup.Found -> {
                    // Cała działka na ekranie (nad kartą z opisem) – bez przybliżania widoku.
                    chrome.showArea(result.parcel.shape, zoomIn = false)
                    ParcelSelection.Selected(result.parcel)
                }
                ParcelLookup.NotFound -> ParcelSelection.NotFound
                ParcelLookup.Unavailable -> ParcelSelection.Unavailable
            }
        }
    }

    fun clearParcelSelection() {
        parcelLookup?.cancel()
        _parcelSelection.value = ParcelSelection.None
    }
}
