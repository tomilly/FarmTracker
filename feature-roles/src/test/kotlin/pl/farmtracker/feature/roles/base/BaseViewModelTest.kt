package pl.farmtracker.feature.roles.base

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Base
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.map.CameraRequest
import pl.farmtracker.core.testing.FakeBaseRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewFixture

class BaseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val crew = CrewFixture(myRole = Role.BASE)
    private val base = GeoPoint(49.95, 17.005)
    private val viewModel by lazy { BaseViewModel(crew.watch, FakeBaseRepository(Base(base))) }

    @Test
    fun `sees the harvester and the drivers, but not people gone for hours`() = runTest(mainDispatcherRule.testDispatcher) {
        crew.locations.locations.value = listOf(
            crew.someone("Rysiek", Role.HARVESTER, crew.onTheField, fieldId = "f1"),
            crew.someone("Marek", Role.DRIVER, crew.onTheField, fieldId = "f1", trip = Trip.LOADING),
            crew.someone("Janek", Role.DRIVER, crew.onTheRoad, fieldId = null, minutesAgo = 13 * 60),
        )
        viewModel.uiState.launchIn(backgroundScope)

        assertEquals(
            listOf(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false)),
            viewModel.uiState.value.harvesters,
        )
        assertEquals(listOf("Marek"), viewModel.uiState.value.drivers.map { it.name })
    }

    @Test
    fun `drivers coming back with a load are on top, nearest first, without a position at the end`() =
        runTest(mainDispatcherRule.testDispatcher) {
            crew.locations.locations.value = listOf(
                crew.someone("Adam", Role.DRIVER, crew.onTheField, fieldId = "f1", trip = Trip.LOADING),
                crew.someone("Bartek", Role.DRIVER, crew.onTheRoad, fieldId = null, trip = Trip.TO_BASE),
                crew.someone("Czarek", Role.DRIVER, GeoPoint(49.96, 17.005), fieldId = null, trip = Trip.TO_BASE),
                crew.someone("Darek", Role.DRIVER, GeoPoint(49.951, 17.005), fieldId = null, minutesAgo = 10),
            )
            viewModel.uiState.launchIn(backgroundScope)

            val drivers = viewModel.uiState.value.drivers
            assertEquals(listOf("Czarek", "Bartek", "Adam", "Darek"), drivers.map { it.name })
            // Odległość od bazy: 0,01° szerokości to ok. 1,1 km.
            assertEquals(1_112.0, drivers.first().distanceMeters!!, 5.0)
        }

    @Test
    fun `the map starts with all fields and the base in view`() = runTest(mainDispatcherRule.testDispatcher) {
        val request = viewModel.chrome.state.value.cameraRequest as CameraRequest.ShowArea

        assertEquals(49.95, request.bounds.south, 1e-9)
        assertEquals(50.01, request.bounds.north, 1e-9)
    }
}
