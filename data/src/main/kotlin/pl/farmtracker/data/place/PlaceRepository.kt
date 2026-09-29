package pl.farmtracker.data.place

import pl.farmtracker.core.domain.Place
import pl.farmtracker.core.domain.geo.GeoPoint

sealed interface PlaceSearch {
    data class Found(val places: List<Place>) : PlaceSearch
    data object NotFound : PlaceSearch
    data object Unavailable : PlaceSearch
}

interface PlaceRepository {
    /**
     * Miejscowości w Polsce po nazwie – także wpisanej bez polskich znaków („sulmow" → Sulmów).
     *
     * @param near gdy podane, bliższe miejscowości są preferowane (ta sama nazwa bywa w wielu gminach)
     */
    suspend fun search(query: String, near: GeoPoint?): PlaceSearch
}
