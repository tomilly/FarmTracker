package pl.farmtracker.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class PolishPluralTest {

    private fun forms(vararg counts: Int) = counts.map { polishPlural(it) }

    @Test
    fun `one only for exactly one`() {
        assertEquals(listOf(PolishPlural.ONE), forms(1))
        assertEquals(PolishPlural.MANY, polishPlural(21))
        assertEquals(PolishPlural.MANY, polishPlural(101))
    }

    @Test
    fun `few for numbers ending in 2 to 4 except teens`() {
        assertEquals(List(6) { PolishPlural.FEW }, forms(2, 3, 4, 22, 34, 104))
        assertEquals(List(3) { PolishPlural.MANY }, forms(12, 13, 14))
        assertEquals(List(2) { PolishPlural.MANY }, forms(112, 114))
    }

    @Test
    fun `many for zero, five and up`() {
        assertEquals(List(5) { PolishPlural.MANY }, forms(0, 5, 11, 20, 25))
    }
}
