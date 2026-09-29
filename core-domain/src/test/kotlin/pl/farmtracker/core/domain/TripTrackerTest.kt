package pl.farmtracker.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint

class TripTrackerTest {

    // Baza na południu, pole ok. 5,5 km na północ; 0,001° szerokości ≈ 111 m.
    private val base = GeoPoint(50.0, 17.0)
    private val harvester = GeoPoint(50.055, 17.0)
    private val besideHarvester = GeoPoint(50.0551, 17.0) // ~11 m
    private val elsewhereOnField = GeoPoint(50.058, 17.0) // ~330 m od sieczkarni
    private val road = List(5) { GeoPoint(50.04 - it * 0.005, 17.0) } // z pola w stronę bazy, co ~550 m
    private val atBase = GeoPoint(50.0005, 17.0) // ~55 m od bazy

    private val tracker = TripTracker()

    private fun onField(point: GeoPoint) = tracker.update(point, fieldId = "f1", base = base, harvesters = listOf(harvester))

    private fun offField(point: GeoPoint) = tracker.update(point, fieldId = null, base = base, harvesters = listOf(harvester))

    @Test
    fun `driving alongside the harvester is loading - passing by is not`() {
        assertEquals(Trip.ON_FIELD, onField(besideHarvester))
        assertEquals(Trip.LOADING, onField(besideHarvester))

        assertEquals(Trip.ON_FIELD, onField(elsewhereOnField))
    }

    @Test
    fun `leaving the field after loading - back to the base, then unloading at the base`() {
        onField(besideHarvester)
        onField(besideHarvester)

        assertEquals(Trip.TO_BASE, offField(road[0]))
        assertEquals(Trip.TO_BASE, offField(road[3]))
        assertEquals(Trip.AT_BASE, offField(atBase))
    }

    @Test
    fun `leaving the base - on the way to the field`() {
        offField(atBase)

        assertEquals(Trip.TO_FIELD, offField(road[4]))
        assertEquals(Trip.TO_FIELD, offField(road[1]))
    }

    @Test
    fun `turning back on the way to the base - heading to the field again`() {
        onField(elsewhereOnField)
        offField(road[1])
        offField(road[2])

        // Zawraca: ~110 m to jeszcze nic (manewr, GPS), ~550 m od bazy – jedzie z powrotem na pole.
        assertEquals(Trip.TO_BASE, offField(GeoPoint(50.031, 17.0)))
        assertEquals(Trip.TO_FIELD, offField(road[1]))
    }

    @Test
    fun `starting work on the road - direction decides once the driver has covered some distance`() {
        assertNull(offField(road[2]))
        assertNull(offField(GeoPoint(50.029, 17.0))) // ~110 m – za mało

        assertEquals(Trip.TO_BASE, offField(road[3]))
    }

    @Test
    fun `a base on the edge of a field is still the base`() {
        assertEquals(Trip.AT_BASE, tracker.update(atBase, fieldId = "home", base = base, harvesters = emptyList()))
    }

    @Test
    fun `a few metres of GPS jitter do not end loading`() {
        onField(besideHarvester)
        onField(besideHarvester)

        // ~67 m od sieczkarni – dalej niż na początku ładowania, ale bliżej niż koniec.
        assertEquals(Trip.LOADING, onField(GeoPoint(50.0556, 17.0)))
    }
}
