package pl.farmtracker.feature.roles.driver

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.DriverState
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.testing.FakeWorkRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewFixture

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
    fun `starts idle without undo`() = driverTest {
        assertEquals(DriverState.IDLE, state.state)
        assertFalse(state.canUndo)
    }

    @Test
    fun `selecting a status makes it current and allows undo`() = driverTest {
        viewModel.selectState(DriverState.TO_FIELD)
        viewModel.selectState(DriverState.LOADING)

        assertEquals(DriverState.LOADING, state.state)
        assertEquals(DriverState.TO_FIELD, state.previousState)
        assertTrue(state.canUndo)
    }

    @Test
    fun `undo restores previous status once`() = driverTest {
        viewModel.selectState(DriverState.TO_FIELD)
        viewModel.selectState(DriverState.LOADING)

        viewModel.undo()

        assertEquals(DriverState.TO_FIELD, state.state)
        assertFalse(state.canUndo)
    }

    @Test
    fun `selecting the same status again keeps undo target`() = driverTest {
        viewModel.selectState(DriverState.TO_FIELD)
        viewModel.selectState(DriverState.LOADING)

        viewModel.selectState(DriverState.LOADING)

        assertEquals(DriverState.TO_FIELD, state.previousState)
    }

    @Test
    fun `undo without history does nothing`() = driverTest {
        viewModel.undo()

        assertEquals(DriverState.IDLE, state.state)
        assertFalse(state.canUndo)
    }

    @Test
    fun `start and stop work`() = driverTest {
        viewModel.startWork()
        assertTrue(state.isWorking)

        viewModel.stopWork()
        assertFalse(work.isWorking.value)
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
