package pl.farmtracker.feature.work

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.testing.FakeClock
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.FakeLiveLocationRepository

class LocationPublisherTest {

    private val field = Field(
        id = "f1",
        name = "Za lasem",
        color = FieldColor.BLUE,
        shape = listOf(
            GeoPolygon(listOf(GeoPoint(50.0, 17.0), GeoPoint(50.0, 17.01), GeoPoint(50.01, 17.01), GeoPoint(50.01, 17.0))),
        ),
    )
    private val onTheField = GeoPoint(50.005, 17.005)
    private val onTheRoad = GeoPoint(50.02, 17.005)

    private val fields = FakeFieldRepository(listOf(field))
    private val locations = FakeLiveLocationRepository()
    private val clock = FakeClock()
    private val gps = MutableSharedFlow<GeoPoint>()
    private val shownFields = mutableListOf<String?>()

    private fun TestScope.startPublishing() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            LocationPublisher(fields, locations, clock).publish(gps) { shownFields += it?.name }
        }
    }

    @Test
    fun `the first position goes out right away, with the field it is on`() = runTest {
        startPublishing()

        gps.emit(onTheField)

        assertEquals(listOf("f1"), locations.published.map { it.fieldId })
        assertEquals(listOf("Za lasem"), shownFields)
    }

    @Test
    fun `standing still, GPS readings do not go out every time`() = runTest {
        startPublishing()

        gps.emit(onTheField)
        clock.now = 5_000
        gps.emit(onTheField)
        clock.now = 60_000
        gps.emit(onTheField)

        assertEquals(listOf(0L, 60_000L), locations.published.map { it.timeMillis })
    }

    @Test
    fun `leaving the field goes out right away`() = runTest {
        startPublishing()

        gps.emit(onTheField)
        clock.now = 1_000
        gps.emit(onTheRoad)

        assertEquals(listOf("f1", null), locations.published.map { it.fieldId })
        assertEquals(listOf("Za lasem", null), shownFields)
    }

    @Test
    fun `a field drawn by the admin while working is detected without moving`() = runTest {
        fields.delete("f1")
        startPublishing()
        gps.emit(onTheField)

        fields.save(field)

        assertEquals(listOf(null, "f1"), locations.published.map { it.fieldId })
    }
}
