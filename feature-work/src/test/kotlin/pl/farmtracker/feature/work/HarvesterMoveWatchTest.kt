package pl.farmtracker.feature.work

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.FakeLiveLocationRepository
import pl.farmtracker.core.testing.FakePeopleRepository

class HarvesterMoveWatchTest {

    private val fields = FakeFieldRepository(
        listOf(
            Field("f1", "Za lasem", FieldColor.BLUE, shape = emptyList()),
            Field("f2", "Przy drodze", FieldColor.ORANGE, shape = emptyList()),
        ),
    )
    private val locations = FakeLiveLocationRepository(myRole = Role.DRIVER)
    private val people = FakePeopleRepository(role = Role.DRIVER)
    private val notices = mutableListOf<HarvesterMoveNotice>()

    private fun rysiek(fieldId: String?) =
        LiveLocation("h1", "Rysiek", Role.HARVESTER, GeoPoint(50.0, 17.0), timeMillis = 0, fieldId = fieldId)

    private fun TestScope.watch() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            HarvesterMoveWatch(locations, fields, people).notices().collect { notices += it }
        }
    }

    @Test
    fun `the driver hears where the harvester moved, by field name`() = runTest {
        locations.locations.value = listOf(rysiek("f1"))
        watch()

        locations.locations.value = listOf(rysiek(null))
        locations.locations.value = listOf(rysiek("f2"))

        assertEquals(listOf(HarvesterMoveNotice("Rysiek", "Przy drodze")), notices)
    }

    @Test
    fun `others (the harvester itself, the base, the admin) are not told`() = runTest {
        people.myRole.value = Role.BASE
        locations.locations.value = listOf(rysiek("f1"))
        watch()

        locations.locations.value = listOf(rysiek("f2"))

        assertEquals(emptyList<HarvesterMoveNotice>(), notices)
    }
}
