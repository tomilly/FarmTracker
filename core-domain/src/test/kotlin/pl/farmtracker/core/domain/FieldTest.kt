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

    private val east = Field(id = "e", name = "Wschód", color = FieldColor.CYAN, shape = listOf(square(50.0, 17.02)))

    /** Punkt ~14 m na wschód od wschodniej granicy pola „Południe" (0,0002° długości na 50° N). */
    private val justOutsideSouth = GeoPoint(50.005, 17.0102)

    @Test
    fun `a machine entering a field is on it`() {
        assertEquals(south, listOf(south, east).fieldWith(GeoPoint(50.005, 17.005), currentFieldId = null))
    }

    @Test
    fun `a machine a few metres past the edge stays on its field - GPS jumps`() {
        assertEquals(south, listOf(south, east).fieldWith(justOutsideSouth, currentFieldId = "s"))
    }

    @Test
    fun `the same spot does not count as the field for a machine arriving from the road`() {
        assertNull(listOf(south, east).fieldWith(justOutsideSouth, currentFieldId = null))
    }

    @Test
    fun `a machine well past the edge has left the field`() {
        assertNull(listOf(south, east).fieldWith(GeoPoint(50.005, 17.015), currentFieldId = "s"))
        assertEquals(east, listOf(south, east).fieldWith(GeoPoint(50.005, 17.025), currentFieldId = "s"))
    }
}
