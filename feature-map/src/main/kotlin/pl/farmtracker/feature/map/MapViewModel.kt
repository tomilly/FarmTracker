package pl.farmtracker.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.parcel.ParcelLookup
import pl.farmtracker.data.parcel.ParcelRepository
import javax.inject.Inject

enum class BaseLayer { MAP, PHOTO }

enum class LocationAccess { UNKNOWN, GRANTED, DENIED }

/** Co się dzieje z dotkniętą działką. */
sealed interface ParcelSelection {
    data object None : ParcelSelection
    data object Searching : ParcelSelection
    data class Selected(val parcel: Parcel) : ParcelSelection
    data object NotFound : ParcelSelection
    data object Unavailable : ParcelSelection
}

data class MapUiState(
    val baseLayer: BaseLayer = BaseLayer.MAP,
    val showParcels: Boolean = false,
    val zoom: Double = INITIAL_ZOOM,
    val locationAccess: LocationAccess = LocationAccess.UNKNOWN,
    /** UI ma teraz zapytać system o zgodę na lokalizację (jednorazowo – potem [MapViewModel.onLocationPermissionAsked]). */
    val askForLocation: Boolean = false,
    /** Rośnie przy każdej prośbie „pokaż mnie na mapie"; mapa reaguje na zmianę wartości. */
    val centerOnMeRequest: Int = 0,
    val parcelSelection: ParcelSelection = ParcelSelection.None,
) {
    val parcelsVisible: Boolean get() = showParcels && zoom >= MapSources.PARCELS_MIN_ZOOM

    val showParcelsZoomHint: Boolean get() = showParcels && !parcelsVisible

    val selectedParcel: Parcel? get() = (parcelSelection as? ParcelSelection.Selected)?.parcel

    companion object {
        /** Cała Polska na ekranie. */
        const val INITIAL_ZOOM = 5.5
    }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val parcelRepository: ParcelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var started = false
    private var parcelLookup: Job? = null

    /** Pierwsze otwarcie mapy: z pozwoleniem – od razu pokaż mnie; bez – zapytaj raz. */
    fun onStart(hasLocationPermission: Boolean) {
        if (started) return
        started = true
        _uiState.update {
            if (hasLocationPermission) {
                it.copy(locationAccess = LocationAccess.GRANTED, centerOnMeRequest = it.centerOnMeRequest + 1)
            } else {
                it.copy(askForLocation = true)
            }
        }
    }

    /** Powrót na ekran (np. z ustawień systemu) – zgoda mogła się zmienić. */
    fun onLocationPermissionRechecked(granted: Boolean) = _uiState.update {
        when {
            granted -> it.copy(locationAccess = LocationAccess.GRANTED)
            it.locationAccess == LocationAccess.GRANTED -> it.copy(locationAccess = LocationAccess.UNKNOWN)
            else -> it
        }
    }

    fun onLocationPermissionAsked() = _uiState.update { it.copy(askForLocation = false) }

    fun onLocationPermissionResult(granted: Boolean) = _uiState.update {
        if (granted) {
            it.copy(locationAccess = LocationAccess.GRANTED, centerOnMeRequest = it.centerOnMeRequest + 1)
        } else {
            it.copy(locationAccess = LocationAccess.DENIED)
        }
    }

    fun onWhereAmIClicked() = _uiState.update {
        if (it.locationAccess == LocationAccess.GRANTED) {
            it.copy(centerOnMeRequest = it.centerOnMeRequest + 1)
        } else {
            it.copy(askForLocation = true)
        }
    }

    fun selectBaseLayer(layer: BaseLayer) = _uiState.update { it.copy(baseLayer = layer) }

    fun toggleParcels() {
        val turningOff = _uiState.value.showParcels
        if (turningOff) parcelLookup?.cancel()
        _uiState.update {
            it.copy(
                showParcels = !it.showParcels,
                parcelSelection = if (turningOff) ParcelSelection.None else it.parcelSelection,
            )
        }
    }

    fun onZoomChanged(zoom: Double) = _uiState.update { it.copy(zoom = zoom) }

    /** Dotknięcie mapy zaznacza działkę – tylko gdy granice działek są widoczne. */
    fun onMapTapped(point: GeoPoint) {
        if (!_uiState.value.parcelsVisible) return
        parcelLookup?.cancel()
        _uiState.update { it.copy(parcelSelection = ParcelSelection.Searching) }
        parcelLookup = viewModelScope.launch {
            val selection = when (val result = parcelRepository.parcelAt(point)) {
                is ParcelLookup.Found -> ParcelSelection.Selected(result.parcel)
                ParcelLookup.NotFound -> ParcelSelection.NotFound
                ParcelLookup.Unavailable -> ParcelSelection.Unavailable
            }
            _uiState.update { it.copy(parcelSelection = selection) }
        }
    }

    fun clearParcelSelection() {
        parcelLookup?.cancel()
        _uiState.update { it.copy(parcelSelection = ParcelSelection.None) }
    }
}
