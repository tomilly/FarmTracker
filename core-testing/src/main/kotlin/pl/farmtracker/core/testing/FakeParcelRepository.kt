package pl.farmtracker.core.testing

import kotlinx.coroutines.CompletableDeferred
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.parcel.ParcelLookup
import pl.farmtracker.data.parcel.ParcelRepository

/**
 * @param result co zwrócić na każde zapytanie
 * @param gate gdy ustawione, zapytanie czeka na `complete()` – pozwala sprawdzić stan „szukam…"
 */
class FakeParcelRepository(
    var result: ParcelLookup = ParcelLookup.NotFound,
    var gate: CompletableDeferred<Unit>? = null,
) : ParcelRepository {

    val requests = mutableListOf<GeoPoint>()

    override suspend fun parcelAt(point: GeoPoint): ParcelLookup {
        requests += point
        gate?.await()
        return result
    }
}
