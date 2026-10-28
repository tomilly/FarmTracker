package pl.farmtracker.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GpsPaceTest {

    @Test
    fun `moving or a short stop - fast, two minutes standing - slow`() {
        assertEquals(GpsPace.FAST, GpsPace.forStillSince(stillSinceMillis = 0, nowMillis = 0))
        assertEquals(GpsPace.FAST, GpsPace.forStillSince(stillSinceMillis = 0, nowMillis = 119_000))
        assertEquals(GpsPace.SLOW, GpsPace.forStillSince(stillSinceMillis = 0, nowMillis = 120_000))
    }

    @Test
    fun `slow is still often enough for the minute heartbeat`() {
        assert(GpsPace.SLOW.intervalMillis < PositionReport.HEARTBEAT_MILLIS)
    }
}
