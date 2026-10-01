package pl.farmtracker.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint

class StillTrackerTest {

    private val tracker = StillTracker()
    private val here = GeoPoint(51.7838, 18.4668)

    /** [meters] na północ od [here]. */
    private fun north(meters: Double) = GeoPoint(here.latitude + meters / 111_320.0, here.longitude)

    @Test
    fun `GPS jumping a few metres on the spot - still standing since it stopped`() {
        assertEquals(0L, tracker.update(here, nowMillis = 0))
        assertEquals(0L, tracker.update(north(15.0), nowMillis = 60_000))
        assertEquals(0L, tracker.update(north(-20.0), nowMillis = 600_000))
    }

    @Test
    fun `moving on - standing starts again where it stops`() {
        tracker.update(here, nowMillis = 0)

        assertEquals(60_000L, tracker.update(north(80.0), nowMillis = 60_000))
        assertEquals(120_000L, tracker.update(north(160.0), nowMillis = 120_000))
        assertEquals(120_000L, tracker.update(north(170.0), nowMillis = 500_000))
    }

    @Test
    fun `standing shows only after 5 minutes, in whole minutes`() {
        val location = LiveLocation("u1", "Marek", Role.DRIVER, here, timeMillis = 0, stillSinceMillis = 0)

        assertNull(location.standingMinutesAt(4 * 60_000))
        assertEquals(5, location.standingMinutesAt(5 * 60_000))
        assertEquals(12, location.standingMinutesAt(12 * 60_000 + 30_000))
        assertNull(location.copy(stillSinceMillis = null).standingMinutesAt(60 * 60_000))
    }
}
