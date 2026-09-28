package pl.farmtracker.core.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos

class GeoAreaTest {

    private val metersPerDegreeLat = 6_371_008.8 * PI / 180

    /** Kwadrat o boku [sideMeters] z lewym dolnym rogiem w (lat, lon). */
    private fun square(lat: Double, lon: Double, sideMeters: Double): List<GeoPoint> {
        val dLat = sideMeters / metersPerDegreeLat
        val dLon = dLat / cos(lat * PI / 180)
        return listOf(
            GeoPoint(lat, lon),
            GeoPoint(lat, lon + dLon),
            GeoPoint(lat + dLat, lon + dLon),
            GeoPoint(lat + dLat, lon),
            GeoPoint(lat, lon),
        )
    }

    @Test
    fun `100 m square is one hectare`() {
        val hectares = GeoArea.hectares(listOf(GeoPolygon(square(52.0, 17.0, 100.0))))

        assertEquals(1.0, hectares, 0.005)
    }

    @Test
    fun `200 by 200 m field is four hectares regardless of point order`() {
        val ring = square(50.98, 17.42, 200.0)

        assertEquals(4.0, GeoArea.hectares(listOf(GeoPolygon(ring))), 0.02)
        assertEquals(4.0, GeoArea.hectares(listOf(GeoPolygon(ring.reversed()))), 0.02)
    }

    @Test
    fun `holes are subtracted and parts are added`() {
        val withHole = GeoPolygon(outer = square(52.0, 17.0, 200.0), holes = listOf(square(52.0005, 17.0005, 100.0)))
        val separate = GeoPolygon(square(52.1, 17.1, 100.0))

        assertEquals(4.0, GeoArea.hectares(listOf(withHole, separate)), 0.03)
    }
}
