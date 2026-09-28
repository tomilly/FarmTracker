package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoArea
import pl.farmtracker.core.domain.geo.GeoPolygon

/**
 * Działka ewidencyjna (z usługi GUGiK ULDK).
 *
 * @param id pełny identyfikator, np. `302103_5.0007.125` (TERYT + obręb + numer)
 * @param number numer działki w obrębie, np. `125` albo `12/3`
 * @param precinct obręb ewidencyjny (zwykle nazwa wsi)
 */
data class Parcel(
    val id: String,
    val number: String,
    val precinct: String,
    val commune: String,
    val shape: List<GeoPolygon>,
) {
    val areaHectares: Double get() = GeoArea.hectares(shape)
}
