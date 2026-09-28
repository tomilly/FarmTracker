package pl.farmtracker.core.testing

import kotlinx.coroutines.CompletableDeferred
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.parcel.ParcelLookup
import pl.farmtracker.data.parcel.ParcelRepository
import pl.farmtracker.data.parcel.ParcelSearch

/**
 * @param result co zwrócić na każde zapytanie o punkt
 * @param searchResult co zwrócić na każde wyszukiwanie po numerze
 * @param gate gdy ustawione, zapytania czekają na `complete()` – pozwala sprawdzić stan „szukam…"
 */
class FakeParcelRepository(
    var result: ParcelLookup = ParcelLookup.NotFound,
    var searchResult: ParcelSearch = ParcelSearch.NotFound,
    var gate: CompletableDeferred<Unit>? = null,
) : ParcelRepository {

    val requests = mutableListOf<GeoPoint>()
    val searches = mutableListOf<String>()

    override suspend fun parcelAt(point: GeoPoint): ParcelLookup {
        requests += point
        gate?.await()
        return result
    }

    override suspend fun search(query: String): ParcelSearch {
        searches += query
        gate?.await()
        return searchResult
    }
}
