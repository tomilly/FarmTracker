package pl.farmtracker.feature.roles.harvester

import org.junit.Assert.assertEquals
import org.junit.Test

class HarvesterViewModelTest {

    private val viewModel = HarvesterViewModel()

    @Test
    fun `starts not working`() {
        assertEquals(HarvesterUiState(isWorking = false, isMoving = false), viewModel.uiState.value)
    }

    @Test
    fun `start work then move then arrive`() {
        viewModel.startWork()
        assertEquals(HarvesterUiState(isWorking = true, isMoving = false), viewModel.uiState.value)

        viewModel.startMoving()
        assertEquals(HarvesterUiState(isWorking = true, isMoving = true), viewModel.uiState.value)

        viewModel.arrivedAtField()
        assertEquals(HarvesterUiState(isWorking = true, isMoving = false), viewModel.uiState.value)
    }

    @Test
    fun `cannot start moving before starting work`() {
        viewModel.startMoving()

        assertEquals(HarvesterUiState(), viewModel.uiState.value)
    }

    @Test
    fun `stopping work resets everything`() {
        viewModel.startWork()
        viewModel.startMoving()

        viewModel.stopWork()

        assertEquals(HarvesterUiState(), viewModel.uiState.value)
    }
}
