package pl.farmtracker.data.parcel

import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint

sealed interface ParcelLookup {
    data class Found(val parcel: Parcel) : ParcelLookup

    /** W tym miejscu nie ma działki ewidencyjnej (np. poza Polską). */
    data object NotFound : ParcelLookup

    /** Brak zasięgu albo usługa nie odpowiada – można spróbować ponownie. */
    data object Unavailable : ParcelLookup
}

sealed interface ParcelSearch {
    /** Jedna lub więcej działek (ta sama nazwa wsi bywa w wielu gminach). */
    data class Found(val parcels: List<Parcel>) : ParcelSearch
    data object NotFound : ParcelSearch
    data object Unavailable : ParcelSearch
}

interface ParcelRepository {
    suspend fun parcelAt(point: GeoPoint): ParcelLookup

    /** Szukanie po obrębie i numerze („Otusz 125") albo po pełnym identyfikatorze działki. */
    suspend fun search(query: String): ParcelSearch
}
