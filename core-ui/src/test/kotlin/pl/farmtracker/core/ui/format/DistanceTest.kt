package pl.farmtracker.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {

    /** Twarda spacja zamieniona na zwykłą – czytelniej w asercjach. */
    private fun format(meters: Double) = formatDistance(meters).replace(' ', ' ')

    @Test
    fun `metres rounded to 50, never zero`() {
        assertEquals("50 m", format(3.0))
        assertEquals("350 m", format(341.0))
        assertEquals("900 m", format(912.0))
    }

    @Test
    fun `kilometres with a Polish comma, whole kilometres from 10 km`() {
        assertEquals("1,0 km", format(960.0))
        assertEquals("4,2 km", format(4_180.0))
        assertEquals("12 km", format(12_400.0))
    }

    @Test
    fun `number and unit never split across lines`() {
        assertEquals("1,6 km", formatDistance(1_600.0))
    }
}
