package pl.farmtracker.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon

class FieldTest {

    private fun square(lat: Double, lon: Double) =
        GeoPolygon(listOf(GeoPoint(lat, lon), GeoPoint(lat, lon + 0.01), GeoPoint(lat + 0.01, lon + 0.01), GeoPoint(lat + 0.01, lon)))

    private val south = Field(id = "s", name = "Południe", color = FieldColor.BLUE, shape = listOf(square(50.0, 17.0)))
    private val north = Field(id = "n", name = "Północ", color = FieldColor.PINK, shape = listOf(square(50.01, 17.0)))

    @Test
    fun `finds the field under a tapped point`() {
        assertEquals(north, listOf(south, north).fieldAt(GeoPoint(50.015, 17.005)))
    }

    @Test
    fun `a tap outside every field finds nothing`() {
        assertNull(listOf(south, north).fieldAt(GeoPoint(50.1, 17.1)))
    }
}
