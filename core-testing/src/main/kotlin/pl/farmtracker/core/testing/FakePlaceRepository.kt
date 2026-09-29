package pl.farmtracker.core.testing

import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.place.PlaceRepository
import pl.farmtracker.data.place.PlaceSearch

class FakePlaceRepository(var result: PlaceSearch = PlaceSearch.NotFound) : PlaceRepository {

    val queries = mutableListOf<String>()

    override suspend fun search(query: String, near: GeoPoint?): PlaceSearch {
        queries += query
        return result
    }
}
