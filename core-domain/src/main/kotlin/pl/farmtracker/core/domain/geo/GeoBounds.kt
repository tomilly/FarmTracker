package pl.farmtracker.core.domain.geo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/** Prostokąt obejmujący kształt – np. żeby pokazać całą działkę na ekranie. */
data class GeoBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    val center: GeoPoint get() = GeoPoint(latitude = (south + north) / 2, longitude = (west + east) / 2)
}

/** `null`, gdy nie ma żadnego punktu. */
fun List<GeoPolygon>.bounds(): GeoBounds? {
    val points = flatMap { it.outer }
    if (points.isEmpty()) return null
    return GeoBounds(
        south = points.minOf { it.latitude },
        west = points.minOf { it.longitude },
        north = points.maxOf { it.latitude },
        east = points.maxOf { it.longitude },
    )
}

private const val EARTH_RADIUS_M = 6_371_008.8

/**
 * Odległość w metrach (rzut równoodległościowy). Dokładna na odległościach spotykanych przy zbiorze
 * (do kilkudziesięciu km); do sortowania wyników wystarcza w całej Polsce.
 */
fun GeoPoint.distanceMetersTo(other: GeoPoint): Double {
    val meanLat = (latitude + other.latitude) / 2 * PI / 180
    val dx = (other.longitude - longitude) * PI / 180 * cos(meanLat)
    val dy = (other.latitude - latitude) * PI / 180
    return sqrt(dx * dx + dy * dy) * EARTH_RADIUS_M
}
