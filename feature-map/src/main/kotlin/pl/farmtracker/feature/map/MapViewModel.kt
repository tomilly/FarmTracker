package pl.farmtracker.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.activeFieldIds
import pl.farmtracker.core.domain.fieldAt
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapPerson
import pl.farmtracker.core.map.toMapPeople
import pl.farmtracker.data.base.BaseRepository
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.parcel.ParcelLookup
import pl.farmtracker.data.parcel.ParcelRepository
import pl.farmtracker.data.session.PeopleRepository
import pl.farmtracker.data.time.Clock
import pl.farmtracker.data.time.ticks
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
    peopleRepository: PeopleRepository,
    baseRepository: BaseRepository,
    liveLocationRepository: LiveLocationRepository,
    clock: Clock,
) : ViewModel() {

    val chrome = MapChromeController()

    /** Pola zbioru – widoczne na mapie dla każdej roli. */
    val fields: StateFlow<List<Field>> = fieldRepository.fields
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Baza (silos / pryzma) – cel kursów kierowców, widoczna dla każdej roli. */
    val base: StateFlow<GeoPoint?> = baseRepository.base
        .map { it?.location }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Inni pracujący (siebie pokazuje niebieska kropka „ja"); pozycja sprzed wielu godzin znika. */
    val people: StateFlow<List<MapPerson>> =
        combine(liveLocationRepository.locations, clock.ticks()) { locations, now ->
            locations.toMapPeople(maxOf(now, clock.nowMillis()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pola, na których pracuje sieczkarnia – podświetlone. */
    val activeFieldIds: StateFlow<Set<String>> =
        combine(liveLocationRepository.locations, clock.ticks()) { locations, now ->
            locations.activeFieldIds(maxOf(now, clock.nowMillis()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Pola zmienia tylko admin – pozostałe role widzą przy polu sam opis. */
    val canEditFields: StateFlow<Boolean> = peopleRepository.myRole
        .map { it == Role.ADMIN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _parcelSelection = MutableStateFlow<ParcelSelection>(ParcelSelection.None)
    val parcelSelection: StateFlow<ParcelSelection> = _parcelSelection.asStateFlow()

    private val selectedFieldId = MutableStateFlow<String?>(null)

    /** Dotknięte pole (aktualne – po edycji z nową nazwą; znika, gdy pole usunięto). */
    val selectedField: StateFlow<Field?> = combine(fields, selectedFieldId) { all, id -> all.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var parcelLookup: Job? = null

    init {
        // Ukrycie warstwy działek chowa też zaznaczenie – inaczej żółta plama zostaje bez kontekstu.
        viewModelScope.launch {
            chrome.state.map { it.showParcels }.distinctUntilChanged().filter { !it }.collect {
                clearParcelSelection()
            }
        }
        // Admin patrzy na całą pracę: na start wszystkie pola i baza na jednym widoku (zamiast „ja").
        viewModelScope.launch {
            if (peopleRepository.myRole.first { it != null } != Role.ADMIN) return@launch
            // Bez pól (jeszcze żadnego nie narysowano) zostaje zwykły widok – mapa nie skacze potem sama.
            val shapes = withTimeoutOrNull(FIELDS_WAIT_MILLIS) { fieldRepository.fields.first { it.isNotEmpty() } }
                ?.flatMap { it.shape }
                ?: return@launch
            val base = baseRepository.base.first()?.location
            chrome.showArea(shapes + listOfNotNull(base?.let { GeoPolygon(listOf(it)) }))
        }
    }

    /**
     * Dotknięcie pola pokazuje jego opis (admin może stąd przejść do edycji). Poza polami dotknięcie
     * zaznacza działkę – gdy granice działek są widoczne.
     */
    fun onMapTapped(point: GeoPoint) {
        val field = fields.value.fieldAt(point)
        if (field != null) {
            clearParcelSelection()
            selectedFieldId.value = field.id
            chrome.showArea(field.shape, zoomIn = false)
            return
        }
        selectedFieldId.value = null
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

    fun clearFieldSelection() {
        selectedFieldId.value = null
    }

    private companion object {
        /** Pola z serwera (albo z kopii na telefonie) – zwykle są od razu; dłużej nie czekamy z widokiem. */
        const val FIELDS_WAIT_MILLIS = 5_000L
    }
}
