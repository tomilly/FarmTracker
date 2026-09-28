package pl.farmtracker.core.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoBoundsTest {

    @Test
    fun `bounds cover all parts and give the middle`() {
        val parts = listOf(
            GeoPolygon(listOf(GeoPoint(50.0, 17.0), GeoPoint(50.1, 17.2))),
            GeoPolygon(listOf(GeoPoint(49.9, 17.1), GeoPoint(50.05, 17.3))),
        )

        val bounds = parts.bounds()!!

        assertEquals(GeoBounds(south = 49.9, west = 17.0, north = 50.1, east = 17.3), bounds)
        assertEquals(50.0, bounds.center.latitude, 1e-9)
        assertEquals(17.15, bounds.center.longitude, 1e-9)
    }

    @Test
    fun `no points means no bounds`() {
        assertNull(emptyList<GeoPolygon>().bounds())
    }

    @Test
    fun `distance matches known values`() {
        // 0,01° szerokości ≈ 1112 m niezależnie od miejsca.
        assertEquals(1112.0, GeoPoint(50.0, 17.0).distanceMetersTo(GeoPoint(50.01, 17.0)), 2.0)
        // Poznań – Wrocław ≈ 145 km w linii prostej.
        assertEquals(145_000.0, GeoPoint(52.4064, 16.9252).distanceMetersTo(GeoPoint(51.1079, 17.0385)), 2_000.0)
    }
}
