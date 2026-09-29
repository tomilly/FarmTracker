package pl.farmtracker.core.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoDistanceTest {

    @Test
    fun `one hundredth of a degree of latitude is about 1112 m`() {
        assertEquals(1112.0, GeoPoint(50.0, 17.0).distanceMeters(GeoPoint(50.01, 17.0)), 1.0)
    }

    @Test
    fun `a point inside a shape is 0 m away, outside - to the nearest edge`() {
        val square = listOf(
            GeoPolygon(listOf(GeoPoint(50.0, 17.0), GeoPoint(50.0, 17.01), GeoPoint(50.01, 17.01), GeoPoint(50.01, 17.0))),
        )

        assertEquals(0.0, square.distanceMeters(GeoPoint(50.005, 17.005)), 0.0)
        // 0,001° na północ od górnej krawędzi.
        assertEquals(111.2, square.distanceMeters(GeoPoint(50.011, 17.005)), 0.5)
    }
}
