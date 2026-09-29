package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoPoint

/**
 * Miejscowość (wieś, miasto) – żeby przenieść mapę w okolicę pól, gdy nie pamięta się numerów działek.
 *
 * @param commune gmina, np. „gmina Goszczanów"
 * @param county powiat, np. „powiat sieradzki"
 */
data class Place(
    val name: String,
    val commune: String,
    val county: String,
    val location: GeoPoint,
)
