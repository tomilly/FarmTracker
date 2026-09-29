package pl.farmtracker.feature.roles.driver

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
import pl.farmtracker.core.testing.FakeWorkRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewFixture
import pl.farmtracker.feature.roles.common.MyPosition

class DriverViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val crew = CrewFixture(myRole = Role.DRIVER)
    private val work = FakeWorkRepository()
    private val viewModel by lazy { DriverViewModel(work, crew.watch) }
    private val state get() = viewModel.uiState.value

    private fun driverTest(body: suspend TestScope.() -> Unit) = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.uiState.launchIn(backgroundScope)
        body()
    }

    @Test
    fun `starts not working`() = driverTest {
        assertFalse(state.isWorking)
    }

    @Test
    fun `start and stop work`() = driverTest {
        viewModel.startWork()
        assertTrue(state.isWorking)

        viewModel.stopWork()
        assertFalse(work.isWorking.value)
    }

    @Test
    fun `the phone knows where the driver is - no status buttons`() = driverTest {
        viewModel.startWork()

        crew.locations.publish(PositionReport(crew.onTheField, "f1", crew.clock.now))

        assertEquals(MyPosition.OnField("Za lasem"), state.position)
    }

    @Test
    fun `sees on which field the harvester is, and when its position is old`() = driverTest {
        crew.locations.locations.value = listOf(
            crew.someone("Rysiek", Role.HARVESTER, crew.onTheField, fieldId = "f1"),
            crew.someone("Staszek", Role.HARVESTER, crew.onTheRoad, fieldId = null, minutesAgo = 10),
        )

        assertEquals(
            listOf(
                Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false),
                Coworker("Staszek", Role.HARVESTER, fieldName = null, isStale = true),
            ),
            state.harvesters,
        )
    }
}
