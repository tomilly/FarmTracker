package pl.farmtracker.core.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.farmtracker.core.domain.geo.GeoBounds
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.domain.geo.bounds

enum class BaseLayer { MAP, PHOTO }

enum class LocationAccess { UNKNOWN, GRANTED, DENIED }

/**
 * Prośba o ustawienie kamery. Obowiązuje najnowsza ([id] rośnie) – także gdy mapa powstaje od nowa
 * (np. po powrocie z innego kroku), dzięki czemu nie „skacze" do starszej prośby.
 */
sealed interface CameraRequest {
    val id: Int

    /** Pokaż mnie i podążaj za mną. */
    data class CenterOnMe(override val id: Int) : CameraRequest

    /** Pokaż cały obszar, np. znalezioną działkę. */
    data class ShowArea(override val id: Int, val bounds: GeoBounds) : CameraRequest
}

/** Stan wspólny dla każdego ekranu z mapą: warstwy, kamera i lokalizacja użytkownika. */
data class MapChromeState(
    val baseLayer: BaseLayer = BaseLayer.MAP,
    val showParcels: Boolean = false,
    val zoom: Double = INITIAL_ZOOM,
    /** Środek widoku po ostatnim ruchu mapy – np. do sortowania wyników „najbliżej". */
    val center: GeoPoint? = null,
    val locationAccess: LocationAccess = LocationAccess.UNKNOWN,
    /** UI ma teraz zapytać system o zgodę na lokalizację (jednorazowo – potem [MapChromeController.onLocationPermissionAsked]). */
    val askForLocation: Boolean = false,
    val cameraRequest: CameraRequest? = null,
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
                it.copy(locationAccess = LocationAccess.GRANTED, cameraRequest = it.nextCenterOnMe())
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
            it.copy(locationAccess = LocationAccess.GRANTED, cameraRequest = it.nextCenterOnMe())
        } else {
            it.copy(locationAccess = LocationAccess.DENIED)
        }
    }

    fun onWhereAmIClicked() = _state.update {
        if (it.locationAccess == LocationAccess.GRANTED) {
            it.copy(cameraRequest = it.nextCenterOnMe())
        } else {
            it.copy(askForLocation = true)
        }
    }

    /** Pokaż cały kształt na ekranie (np. działkę znalezioną po numerze). */
    fun showArea(shape: List<GeoPolygon>) {
        val bounds = shape.bounds() ?: return
        _state.update { it.copy(cameraRequest = CameraRequest.ShowArea(it.nextCameraId(), bounds)) }
    }

    fun selectBaseLayer(layer: BaseLayer) = _state.update { it.copy(baseLayer = layer) }

    fun toggleParcels() = _state.update { it.copy(showParcels = !it.showParcels) }

    fun onCameraIdle(zoom: Double, center: GeoPoint? = null) =
        _state.update { it.copy(zoom = zoom, center = center ?: it.center) }

    private fun MapChromeState.nextCameraId() = (cameraRequest?.id ?: 0) + 1

    private fun MapChromeState.nextCenterOnMe() = CameraRequest.CenterOnMe(nextCameraId())
}
