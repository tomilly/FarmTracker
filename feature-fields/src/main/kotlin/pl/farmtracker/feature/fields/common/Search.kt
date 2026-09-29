package pl.farmtracker.feature.fields.common

import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.Place
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.distanceMetersTo
import pl.farmtracker.data.place.PlaceRepository
import pl.farmtracker.data.place.PlaceSearch

/** Wynik wyszukiwania z odległością od środka mapy (gdy znany) – najbliższe na górze. */
data class SearchHit(val parcel: Parcel, val distanceKm: Double?)

/** Znaleziona miejscowość z odległością od środka mapy (gdy znany). */
data class PlaceHit(val place: Place, val distanceKm: Double?)

/** Stan ekranu „Znajdź wieś" – wspólny dla pól i bazy. */
sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Results(val hits: List<SearchHit>) : SearchState
    data class Places(val hits: List<PlaceHit>) : SearchState
    data object NotFound : SearchState
    data object Unavailable : SearchState
}

/** Wsie o tej nazwie (także wpisanej bez polskich znaków), najbliższe środka mapy na górze. */
suspend fun PlaceRepository.searchVillages(query: String, center: GeoPoint?): SearchState =
    when (val result = search(query, center)) {
        is PlaceSearch.Found -> SearchState.Places(
            result.places
                .map { place -> PlaceHit(place, center?.let { place.location.distanceMetersTo(it) / 1000.0 }) }
                .sortedBy { it.distanceKm ?: Double.MAX_VALUE },
        )
        PlaceSearch.NotFound -> SearchState.NotFound
        PlaceSearch.Unavailable -> SearchState.Unavailable
    }
