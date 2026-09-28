package pl.farmtracker.core.domain.geo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Powierzchnia wielokątów na podstawie współrzędnych WGS84. Rzut równoodległościowy wokół średniej
 * szerokości geograficznej – dla pól i działek (do kilku km) błąd jest pomijalny (< 0,5%).
 */
object GeoArea {

    private const val EARTH_RADIUS_M = 6_371_008.8
    private const val SQUARE_METERS_PER_HECTARE = 10_000.0

    fun hectares(polygons: List<GeoPolygon>): Double =
        polygons.sumOf { squareMeters(it) } / SQUARE_METERS_PER_HECTARE

    fun squareMeters(polygon: GeoPolygon): Double =
        ringArea(polygon.outer) - polygon.holes.sumOf { ringArea(it) }

    private fun ringArea(ring: List<GeoPoint>): Double {
        if (ring.size < 3) return 0.0
        val scaleX = cos(ring.map { it.latitude }.average().toRadians())
        var doubled = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            val ax = a.longitude.toRadians() * scaleX
            val bx = b.longitude.toRadians() * scaleX
            doubled += ax * b.latitude.toRadians() - bx * a.latitude.toRadians()
        }
        return abs(doubled) / 2 * EARTH_RADIUS_M * EARTH_RADIUS_M
    }

    private fun Double.toRadians() = this * PI / 180.0
}
