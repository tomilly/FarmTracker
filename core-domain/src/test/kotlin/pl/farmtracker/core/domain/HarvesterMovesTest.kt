package pl.farmtracker.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint

class HarvesterMovesTest {

    private val moves = HarvesterMoves()

    private fun rysiek(fieldId: String?) =
        LiveLocation("h1", "Rysiek", Role.HARVESTER, GeoPoint(50.0, 17.0), timeMillis = 0, fieldId = fieldId)

    @Test
    fun `starting work on a field is not a move`() {
        assertEquals(emptyList<HarvesterMove>(), moves.update(listOf(rysiek("za-lasem"))))
    }

    @Test
    fun `to another field over the road - one move, when it gets there`() {
        moves.update(listOf(rysiek("za-lasem")))

        assertEquals(emptyList<HarvesterMove>(), moves.update(listOf(rysiek(null))))
        assertEquals(listOf(HarvesterMove("Rysiek", "przy-drodze")), moves.update(listOf(rysiek("przy-drodze"))))
        assertEquals(emptyList<HarvesterMove>(), moves.update(listOf(rysiek("przy-drodze"))))
    }

    @Test
    fun `out to the road and back to the same field - no move`() {
        moves.update(listOf(rysiek("za-lasem")))
        moves.update(listOf(rysiek(null)))

        assertEquals(emptyList<HarvesterMove>(), moves.update(listOf(rysiek("za-lasem"))))
    }

    @Test
    fun `started on the road - the first field it drives onto is a move`() {
        moves.update(listOf(rysiek(null)))

        assertEquals(listOf(HarvesterMove("Rysiek", "za-lasem")), moves.update(listOf(rysiek("za-lasem"))))
    }

    @Test
    fun `drivers and my own position are not harvesters on the move`() {
        val driver = LiveLocation("d1", "Marek", Role.DRIVER, GeoPoint(50.0, 17.0), timeMillis = 0, fieldId = "a")
        val me = rysiek("a").copy(userId = "me", isMe = true)
        moves.update(listOf(driver, me))

        assertEquals(emptyList<HarvesterMove>(), moves.update(listOf(driver.copy(fieldId = "b"), me.copy(fieldId = "b"))))
    }
}
