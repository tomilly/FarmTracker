package pl.farmtracker.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint

class LiveLocationTest {

    private val here = GeoPoint(50.0, 17.0)

    /** ~11 m na północ. */
    private val aFewStepsAway = GeoPoint(50.0001, 17.0)

    /** ~111 m na północ. */
    private val downTheRoad = GeoPoint(50.001, 17.0)

    private val sent = LiveLocation("u1", "Marek", Role.HARVESTER, here, timeMillis = 0, fieldId = "f1")

    @Test
    fun `standing still - a position goes out once a minute`() {
        assertFalse(sent.needsUpdate(aFewStepsAway, "f1", nowMillis = 30_000))
        assertTrue(sent.needsUpdate(aFewStepsAway, "f1", nowMillis = 60_000))
    }

    @Test
    fun `driving - a position goes out after a stretch of road, but not more often than every 10 s`() {
        assertFalse(sent.needsUpdate(downTheRoad, "f1", nowMillis = 5_000))
        assertTrue(sent.needsUpdate(downTheRoad, "f1", nowMillis = 10_000))
    }

    @Test
    fun `a change of field goes out right away`() {
        assertTrue(sent.needsUpdate(aFewStepsAway, null, nowMillis = 1_000))
    }

    @Test
    fun `a position becomes stale after 3 minutes without news`() {
        assertFalse(sent.isStaleAt(3 * 60_000))
        assertTrue(sent.isStaleAt(3 * 60_000 + 1))
    }
}
