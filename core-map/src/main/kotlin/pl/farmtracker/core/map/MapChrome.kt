package pl.farmtracker.core.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.farmtracker.core.domain.geo.GeoBounds
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.domain.geo.bounds
import kotlin.math.cos
import kotlin.math.pow

enum class BaseLayer { MAP, PHOTO }

enum class LocationAccess { UNKNOWN, GRANTED, DENIED }

/**
 * Prośba o ustawienie kamery. Obowiązuje najnowsza ([id] rośnie) i wykonuje się raz: gdy mapa powstaje od nowa
 * (np. po powrocie z edycji), wraca do ostatniego widoku – nie „skacze" do dawnej prośby.
 */
sealed interface CameraRequest {
    val id: Int

    /** Pokaż mnie i podążaj za mną. */
    data class CenterOnMe(override val id: Int) : CameraRequest

    /**
     * Pokaż cały obszar, np. znalezioną działkę. Bez [zoomIn] mapa tylko się przesuwa (albo oddala,
     * gdy obszar się nie mieści) – dotknięcie działki nie może nagle przybliżyć widoku.
     */
    data class ShowArea(override val id: Int, val bounds: GeoBounds, val zoomIn: Boolean = true) : CameraRequest

    /** Pokaż miejsce z bliska, np. znalezioną wieś – z widocznymi granicami działek. */
    data class ShowPlace(override val id: Int, val point: GeoPoint, val zoom: Double) : CameraRequest
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
    /** [CameraRequest.id] ostatniej prośby, którą mapa już wykonała. */
    val handledCameraRequestId: Int = 0,
) {
    val parcelsVisible: Boolean get() = showParcels && zoom >= MapSources.PARCELS_MIN_ZOOM

    val showParcelsZoomHint: Boolean get() = showParcels && !parcelsVisible

    /** Prośba o kamerę jeszcze niewykonana; `null` – mapa ma zostać tam, gdzie ją zostawiono. */
    val pendingCameraRequest: CameraRequest? get() = cameraRequest?.takeIf { it.id > handledCameraRequestId }

    /**
     * Ile metrów w terenie przykrywa opuszek palca przy obecnym zoomie – żeby dotknięcie trafiało
     * w mały znak na mapie (np. wjazd) tak samo przy bliskim, jak i oddalonym widoku.
     */
    fun fingerMeters(latitude: Double): Double =
        FINGER_DP * METERS_PER_DP_AT_ZOOM_0 * cos(Math.toRadians(latitude)) / 2.0.pow(zoom)

    companion object {
        /** Cała Polska na ekranie. */
        const val INITIAL_ZOOM = 5.5

        private const val FINGER_DP = 32.0

        /** Równik przy zoomie 0 w MapLibre (kafle 512 px): obwód Ziemi / 512. */
        private const val METERS_PER_DP_AT_ZOOM_0 = 78_271.517
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

    /**
     * Pierwsze otwarcie mapy: z pozwoleniem – od razu pokaż mnie; bez – zapytaj raz.
     * Gdy ekran już poprosił o konkretny widok (np. edytowane pole), zostaje on, a nie „ja".
     */
    fun onStart(hasLocationPermission: Boolean) {
        if (started) return
        started = true
        _state.update {
            when {
                !hasLocationPermission -> it.copy(askForLocation = true)
                it.cameraRequest != null -> it.copy(locationAccess = LocationAccess.GRANTED)
                else -> it.copy(locationAccess = LocationAccess.GRANTED, cameraRequest = it.nextCenterOnMe())
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

    /**
     * Mapa ma pokazywać mnie i jechać za mną (np. kierowca w czasie pracy) – także zanim wiadomo, czy jest zgoda
     * na lokalizację: prośba czeka, aż mapa będzie mogła pokazać pozycję.
     */
    fun followMe() = _state.update { it.copy(cameraRequest = it.nextCenterOnMe()) }

    fun onWhereAmIClicked() = _state.update {
        if (it.locationAccess == LocationAccess.GRANTED) {
            it.copy(cameraRequest = it.nextCenterOnMe())
        } else {
            it.copy(askForLocation = true)
        }
    }

    /**
     * Pokaż cały kształt na ekranie (np. działkę znalezioną po numerze). [zoomIn] = `false` przy
     * dotkniętej działce: mapa przesuwa się do niej, ale nie przybliża.
     */
    fun showArea(shape: List<GeoPolygon>, zoomIn: Boolean = true) {
        val bounds = shape.bounds() ?: return
        _state.update { it.copy(cameraRequest = CameraRequest.ShowArea(it.nextCameraId(), bounds, zoomIn)) }
    }

    /**
     * Przenieś mapę do miejsca (np. wsi) – domyślnie na zoomie, przy którym widać granice działek;
     * bliżej, np. [CLOSE_ZOOM] dla pojedynczego punktu jak baza.
     */
    fun showPlace(point: GeoPoint, zoom: Double = PLACE_ZOOM) = _state.update {
        it.copy(cameraRequest = CameraRequest.ShowPlace(it.nextCameraId(), point, zoom))
    }

    /** Mapa wykonała prośbę – po odtworzeniu mapy (powrót na ekran) nie powtarzamy jej. */
    fun onCameraRequestHandled(id: Int) = _state.update { it.copy(handledCameraRequestId = maxOf(it.handledCameraRequestId, id)) }

    fun selectBaseLayer(layer: BaseLayer) = _state.update { it.copy(baseLayer = layer) }

    fun toggleParcels() = _state.update { it.copy(showParcels = !it.showParcels) }

    fun onCameraIdle(zoom: Double, center: GeoPoint? = null) =
        _state.update { it.copy(zoom = zoom, center = center ?: it.center) }

    private fun MapChromeState.nextCameraId() = (cameraRequest?.id ?: 0) + 1

    private fun MapChromeState.nextCenterOnMe() = CameraRequest.CenterOnMe(nextCameraId())

    companion object {
        /** Ok. 1–2 km szerokości ekranu: cała wieś z polami, a granice działek już widoczne. */
        private const val PLACE_ZOOM = 14.5

        /** Kilkaset metrów – podwórze, silos, najbliższe pola. */
        const val CLOSE_ZOOM = 16.5
    }
}
