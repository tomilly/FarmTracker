package pl.farmtracker.core.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WktTest {

    @Test
    fun `polygon with srid prefix reads lon lat order`() {
        val polygons = Wkt.parsePolygons("SRID=4326;POLYGON((16.5 52.3,16.6 52.3,16.6 52.4,16.5 52.3))")

        assertEquals(1, polygons.size)
        assertEquals(GeoPoint(latitude = 52.3, longitude = 16.5), polygons[0].outer[0])
        assertEquals(4, polygons[0].outer.size)
        assertTrue(polygons[0].holes.isEmpty())
    }

    @Test
    fun `polygon with a hole`() {
        val polygon = Wkt.parsePolygons(
            "POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0), (2 2, 3 2, 3 3, 2 2))",
        ).single()

        assertEquals(5, polygon.outer.size)
        assertEquals(1, polygon.holes.size)
        assertEquals(GeoPoint(latitude = 2.0, longitude = 3.0), polygon.holes[0][1])
    }

    @Test
    fun `multipolygon gives one polygon per part`() {
        val polygons = Wkt.parsePolygons(
            "MULTIPOLYGON(((0 0,1 0,1 1,0 0)),((5 5,6 5,6 6,5 5),(5.2 5.2,5.3 5.2,5.3 5.3,5.2 5.2)))",
        )

        assertEquals(2, polygons.size)
        assertEquals(0, polygons[0].holes.size)
        assertEquals(1, polygons[1].holes.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `other geometry types are rejected`() {
        Wkt.parsePolygons("POINT(16.5 52.3)")
    }
}
