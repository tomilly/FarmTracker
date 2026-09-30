package pl.farmtracker.feature.roles.harvester

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.map.CameraRequest
import pl.farmtracker.core.testing.FakeBaseRepository
import pl.farmtracker.core.testing.FakeWorkRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewFixture
import pl.farmtracker.feature.roles.common.MyPosition

class HarvesterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val crew = CrewFixture(myRole = Role.HARVESTER)
    private val work = FakeWorkRepository()
    private val viewModel by lazy { HarvesterViewModel(work, crew.watch, FakeBaseRepository()) }
    private val state get() = viewModel.uiState.value

    private fun TestScope.watch() = viewModel.uiState.launchIn(backgroundScope)

    @Test
    fun `starts not working`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()

        assertFalse(state.isWorking)
    }

    @Test
    fun `start work, then stop`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()

        viewModel.startWork()
        assertTrue(work.isWorking.value)
        assertTrue(state.isWorking)

        viewModel.stopWork()
        assertFalse(state.isWorking)
    }

    @Test
    fun `the field is detected by the phone - no buttons for it`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()
        viewModel.startWork()
        assertEquals(MyPosition.Searching, state.position)

        crew.locations.publish(PositionReport(crew.onTheField, "f1", crew.clock.now))
        assertEquals(MyPosition.OnField("Za lasem"), state.position)

        crew.locations.publish(PositionReport(crew.onTheRoad, null, crew.clock.now))
        assertEquals(MyPosition.OffField, state.position)
    }

    @Test
    fun `sees where the drivers are`() = runTest(mainDispatcherRule.testDispatcher) {
        crew.locations.locations.value = listOf(
            crew.someone("Marek", Role.DRIVER, crew.onTheRoad, fieldId = null),
            crew.someone("Rysiek", Role.HARVESTER, crew.onTheField, fieldId = "f1"),
        )
        watch()

        assertEquals(listOf(Coworker("Marek", Role.DRIVER, fieldName = null, isStale = false)), state.drivers)
    }

    @Test
    fun `starting work shows the map following the harvester`() = runTest(mainDispatcherRule.testDispatcher) {
        watch()

        viewModel.startWork()

        assertTrue(viewModel.chrome.state.value.pendingCameraRequest is CameraRequest.CenterOnMe)
    }

    @Test
    fun `the map keeps trailers coming to the harvester on screen, lights up its field`() =
        runTest(mainDispatcherRule.testDispatcher) {
            crew.locations.locations.value = listOf(
                crew.someone("Marek", Role.DRIVER, crew.onTheRoad, fieldId = null, trip = Trip.TO_FIELD),
                crew.someone("Janek", Role.DRIVER, crew.onTheField, fieldId = "f1", trip = Trip.LOADING),
                crew.someone("Wojtek", Role.DRIVER, crew.onTheRoad, fieldId = null, trip = Trip.TO_BASE),
            )
            crew.locations.publish(PositionReport(crew.onTheField, "f1", crew.clock.now))
            viewModel.overlays.launchIn(backgroundScope)

            val overlays = viewModel.overlays.value
            assertEquals(listOf(crew.onTheRoad, crew.onTheField), overlays.keepInView)
            assertEquals(setOf("f1"), overlays.activeFieldIds)
            assertEquals(listOf("Marek", "Janek", "Wojtek"), overlays.people.map { it.name })
        }
}
