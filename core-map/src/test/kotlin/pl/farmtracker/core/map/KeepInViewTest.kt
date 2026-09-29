package pl.farmtracker.core.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint
import kotlin.math.cos
import kotlin.math.pow

class KeepInViewTest {

    private val me = GeoPoint(51.7846, 18.4609)

    private fun zoom(vararg points: GeoPoint) =
        zoomKeepingInView(me, points.toList(), halfWidthDp = 150.0, halfHeightDp = 250.0, maxZoom = 16.0, minZoom = 11.0)

    /** Ile metrów od środka do brzegu mapy przy danym zoomie – w poziomie. */
    private fun halfWidthMeters(zoom: Double) = 150.0 * 78_271.517 * cos(Math.toRadians(me.latitude)) / 2.0.pow(zoom)

    @Test
    fun `alone or with someone right next to me - the usual close view`() {
        assertEquals(16.0, zoom(), 0.0)
        assertEquals(16.0, zoom(GeoPoint(me.latitude, me.longitude + 0.0001)), 0.0)
    }

    @Test
    fun `a harvester 400 m east is just on the screen`() {
        val harvester = GeoPoint(me.latitude, me.longitude + 0.0058) // ok. 400 m

        val zoom = zoom(harvester)

        assertTrue(zoom < 16.0)
        assertEquals(400.0, halfWidthMeters(zoom), 5.0)
    }

    @Test
    fun `the farthest one decides, and the taller side of the screen fits more`() {
        val north = GeoPoint(me.latitude + 0.0036, me.longitude) // ok. 400 m na północ – wysokość ekranu większa
        val east = GeoPoint(me.latitude, me.longitude + 0.0058) // ok. 400 m na wschód

        assertEquals(zoom(east), zoom(north, east), 0.0)
        assertTrue(zoom(north) > zoom(east))
    }

    @Test
    fun `a harvester far away does not zoom out beyond the limit`() {
        assertEquals(11.0, zoom(GeoPoint(52.5, 19.5)), 0.0)
    }
}
