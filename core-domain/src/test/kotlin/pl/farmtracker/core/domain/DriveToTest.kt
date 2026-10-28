package pl.farmtracker.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon

class DriveToTest {

    private val northGate = GeoPoint(50.01, 17.005)
    private val southGate = GeoPoint(50.0, 17.005)
    private val field = Field(
        id = "f1",
        name = "Za lasem",
        color = FieldColor.BLUE,
        shape = listOf(
            GeoPolygon(listOf(GeoPoint(50.0, 17.0), GeoPoint(50.0, 17.01), GeoPoint(50.01, 17.01), GeoPoint(50.01, 17.0))),
        ),
        entryPoints = listOf(northGate, southGate),
    )
    private val base = GeoPoint(49.95, 17.005)
    private val harvester = LiveLocation("h1", "Rysiek", Role.HARVESTER, GeoPoint(50.005, 17.005), timeMillis = 0, fieldId = "f1")

    private fun me(point: GeoPoint, trip: Trip?) =
        LiveLocation("me", "Marek", Role.DRIVER, point, timeMillis = 0, trip = trip, isMe = true)

    @Test
    fun `from the base - to the gate of the harvester field closest to me`() {
        val atBase = me(base, Trip.AT_BASE)

        assertEquals(DriveTo.ToField("Za lasem", southGate), driveTo(listOf(atBase, harvester), listOf(field), base, 0))
    }

    @Test
    fun `loaded or heading back - to the base`() {
        val loading = me(GeoPoint(50.005, 17.005), Trip.LOADING)
        val toBase = me(GeoPoint(49.99, 17.005), Trip.TO_BASE)

        assertEquals(DriveTo.ToBase(base), driveTo(listOf(loading, harvester), listOf(field), base, 0))
        assertEquals(DriveTo.ToBase(base), driveTo(listOf(toBase, harvester), listOf(field), base, 0))
    }

    @Test
    fun `a field without gates - to the harvester itself`() {
        val noGates = field.copy(entryPoints = emptyList())

        assertEquals(
            DriveTo.ToField("Za lasem", harvester.point),
            driveTo(listOf(me(base, Trip.AT_BASE), harvester), listOf(noGates), base, 0),
        )
    }

    @Test
    fun `nowhere to go - no harvester at work, harvester off the fields, no base when loaded`() {
        val atBase = me(base, Trip.AT_BASE)

        assertNull(driveTo(listOf(atBase), listOf(field), base, 0))
        assertNull(driveTo(listOf(atBase, harvester.copy(fieldId = null)), listOf(field), base, 0))
        assertNull(driveTo(listOf(me(base, Trip.LOADING), harvester), listOf(field), base = null, nowMillis = 0))
    }

    @Test
    fun `before starting work (no position of mine) - still to the harvester field`() {
        assertEquals(DriveTo.ToField("Za lasem", northGate), driveTo(listOf(harvester), listOf(field), base, 0))
    }

    @Test
    fun `already on the harvester field - the next way is to the base`() {
        val onTheField = me(GeoPoint(50.004, 17.004), Trip.ON_FIELD).copy(fieldId = "f1")

        assertEquals(DriveTo.ToBase(base), driveTo(listOf(onTheField, harvester), listOf(field), base, 0))
    }
}
