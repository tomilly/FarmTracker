package pl.farmtracker.core.domain.geo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointInPolygonTest {

    private fun square(minLat: Double, minLon: Double, size: Double) = listOf(
        GeoPoint(minLat, minLon),
        GeoPoint(minLat, minLon + size),
        GeoPoint(minLat + size, minLon + size),
        GeoPoint(minLat + size, minLon),
        GeoPoint(minLat, minLon),
    )

    private val field = GeoPolygon(outer = square(50.0, 17.0, 0.01))

    @Test
    fun `point inside and outside`() {
        assertTrue(field.contains(GeoPoint(50.005, 17.005)))
        assertFalse(field.contains(GeoPoint(50.02, 17.005)))
        assertFalse(field.contains(GeoPoint(50.005, 16.99)))
    }

    @Test
    fun `point in a hole is outside`() {
        val withHole = GeoPolygon(outer = square(50.0, 17.0, 0.01), holes = listOf(square(50.004, 17.004, 0.002)))

        assertFalse(withHole.contains(GeoPoint(50.005, 17.005)))
        assertTrue(withHole.contains(GeoPoint(50.001, 17.001)))
    }

    @Test
    fun `concave L shape`() {
        val lShape = GeoPolygon(
            listOf(
                GeoPoint(0.0, 0.0), GeoPoint(0.0, 2.0), GeoPoint(1.0, 2.0),
                GeoPoint(1.0, 1.0), GeoPoint(2.0, 1.0), GeoPoint(2.0, 0.0),
            ),
        )

        assertTrue(lShape.contains(GeoPoint(0.5, 1.5)))
        assertTrue(lShape.contains(GeoPoint(1.5, 0.5)))
        assertFalse(lShape.contains(GeoPoint(1.5, 1.5)))
    }

    @Test
    fun `any of several parts`() {
        val parts = listOf(field, GeoPolygon(square(51.0, 18.0, 0.01)))

        assertTrue(parts.containsPoint(GeoPoint(51.005, 18.005)))
        assertFalse(parts.containsPoint(GeoPoint(52.0, 19.0)))
    }
}
