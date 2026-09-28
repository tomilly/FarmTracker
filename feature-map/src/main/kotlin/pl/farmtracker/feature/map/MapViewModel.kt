package pl.farmtracker.feature.map

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class BaseLayer { MAP, PHOTO }

enum class LocationAccess { UNKNOWN, GRANTED, DENIED }

data class MapUiState(
    val baseLayer: BaseLayer = BaseLayer.MAP,
    val showParcels: Boolean = false,
    val zoom: Double = INITIAL_ZOOM,
    val locationAccess: LocationAccess = LocationAccess.UNKNOWN,
    /** UI ma teraz zapytać system o zgodę na lokalizację (jednorazowo – potem [MapViewModel.onLocationPermissionAsked]). */
    val askForLocation: Boolean = false,
    /** Rośnie przy każdej prośbie „pokaż mnie na mapie"; mapa reaguje na zmianę wartości. */
    val centerOnMeRequest: Int = 0,
) {
    val showParcelsZoomHint: Boolean get() = showParcels && zoom < MapSources.PARCELS_MIN_ZOOM

    companion object {
        /** Cała Polska na ekranie. */
        const val INITIAL_ZOOM = 5.5
    }
}

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var started = false

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

    fun toggleParcels() = _uiState.update { it.copy(showParcels = !it.showParcels) }

    fun onZoomChanged(zoom: Double) = _uiState.update { it.copy(zoom = zoom) }
}
