package pl.farmtracker.core.domain.geo

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_M = 6_371_008.8

/** Odległość po powierzchni Ziemi (wzór haversine), w metrach. */
fun GeoPoint.distanceMeters(other: GeoPoint): Double {
    val dLat = (other.latitude - latitude).toRadians()
    val dLon = (other.longitude - longitude).toRadians()
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(latitude.toRadians()) * cos(other.latitude.toRadians()) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceAtMost(1.0)))
}

/**
 * Jak daleko punkt jest od kształtu (pola, działki), w metrach; 0 – punkt w środku. Rzut równoodległościowy
 * wokół punktu – dla odległości rzędu setek metrów błąd jest pomijalny.
 */
fun List<GeoPolygon>.distanceMeters(point: GeoPoint): Double {
    if (containsPoint(point)) return 0.0
    val scaleX = cos(point.latitude.toRadians())
    fun GeoPoint.x() = (longitude - point.longitude).toRadians() * scaleX * EARTH_RADIUS_M
    fun GeoPoint.y() = (latitude - point.latitude).toRadians() * EARTH_RADIUS_M
    var nearest = Double.POSITIVE_INFINITY
    for (ring in flatMap { listOf(it.outer) + it.holes }) {
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            nearest = minOf(nearest, distanceToSegment(a.x(), a.y(), b.x(), b.y()))
        }
    }
    return nearest
}

/** Odległość początku układu (punktu) od odcinka a–b. */
private fun distanceToSegment(ax: Double, ay: Double, bx: Double, by: Double): Double {
    val dx = bx - ax
    val dy = by - ay
    val lengthSquared = dx * dx + dy * dy
    val t = if (lengthSquared == 0.0) 0.0 else ((-ax * dx - ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
    return hypot(ax + t * dx, ay + t * dy)
}

private fun Double.toRadians() = this * PI / 180.0
