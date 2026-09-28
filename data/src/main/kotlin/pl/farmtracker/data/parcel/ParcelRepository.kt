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

interface ParcelRepository {
    suspend fun parcelAt(point: GeoPoint): ParcelLookup
}
