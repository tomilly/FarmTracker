package pl.farmtracker.core.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class BaseLayer { MAP, PHOTO }

enum class LocationAccess { UNKNOWN, GRANTED, DENIED }

/** Stan wspólny dla każdego ekranu z mapą: warstwy, zoom i lokalizacja użytkownika. */
data class MapChromeState(
    val baseLayer: BaseLayer = BaseLayer.MAP,
    val showParcels: Boolean = false,
    val zoom: Double = INITIAL_ZOOM,
    val locationAccess: LocationAccess = LocationAccess.UNKNOWN,
    /** UI ma teraz zapytać system o zgodę na lokalizację (jednorazowo – potem [MapChromeController.onLocationPermissionAsked]). */
    val askForLocation: Boolean = false,
    /** Rośnie przy każdej prośbie „pokaż mnie na mapie"; mapa reaguje na zmianę wartości. */
    val centerOnMeRequest: Int = 0,
) {
    val parcelsVisible: Boolean get() = showParcels && zoom >= MapSources.PARCELS_MIN_ZOOM

    val showParcelsZoomHint: Boolean get() = showParcels && !parcelsVisible

    companion object {
        /** Cała Polska na ekranie. */
        const val INITIAL_ZOOM = 5.5
    }
}

/**
 * Logika „obudowy" mapy. Zwykła klasa (nie ViewModel) – każdy ViewModel ekranu z mapą trzyma
 * własną instancję, dzięki czemu przeżywa ona obrót ekranu razem z nim.
 */
class MapChromeController(initial: MapChromeState = MapChromeState()) {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<MapChromeState> = _state.asStateFlow()

    private var started = false

    /** Pierwsze otwarcie mapy: z pozwoleniem – od razu pokaż mnie; bez – zapytaj raz. */
    fun onStart(hasLocationPermission: Boolean) {
        if (started) return
        started = true
        _state.update {
            if (hasLocationPermission) {
                it.copy(locationAccess = LocationAccess.GRANTED, centerOnMeRequest = it.centerOnMeRequest + 1)
            } else {
                it.copy(askForLocation = true)
            }
        }
    }

    /** Powrót na ekran (np. z ustawień systemu) – zgoda mogła się zmienić. */
    fun onLocationPermissionRechecked(granted: Boolean) = _state.update {
        when {
            granted -> it.copy(locationAccess = LocationAccess.GRANTED)
            it.locationAccess == LocationAccess.GRANTED -> it.copy(locationAccess = LocationAccess.UNKNOWN)
            else -> it
        }
    }

    fun onLocationPermissionAsked() = _state.update { it.copy(askForLocation = false) }

    fun onLocationPermissionResult(granted: Boolean) = _state.update {
        if (granted) {
            it.copy(locationAccess = LocationAccess.GRANTED, centerOnMeRequest = it.centerOnMeRequest + 1)
        } else {
            it.copy(locationAccess = LocationAccess.DENIED)
        }
    }

    fun onWhereAmIClicked() = _state.update {
        if (it.locationAccess == LocationAccess.GRANTED) {
            it.copy(centerOnMeRequest = it.centerOnMeRequest + 1)
        } else {
            it.copy(askForLocation = true)
        }
    }

    fun selectBaseLayer(layer: BaseLayer) = _state.update { it.copy(baseLayer = layer) }

    fun toggleParcels() = _state.update { it.copy(showParcels = !it.showParcels) }

    fun onZoomChanged(zoom: Double) = _state.update { it.copy(zoom = zoom) }
}
