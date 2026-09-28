package pl.farmtracker.feature.roles.driver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.farmtracker.core.domain.DriverState

class DriverViewModelTest {

    private val viewModel = DriverViewModel()

    @Test
    fun `starts idle without undo`() {
        assertEquals(DriverState.IDLE, viewModel.uiState.value.state)
        assertFalse(viewModel.uiState.value.canUndo)
    }

    @Test
    fun `selecting a status makes it current and allows undo`() {
        viewModel.selectState(DriverState.TO_FIELD)
        viewModel.selectState(DriverState.LOADING)

        val state = viewModel.uiState.value
        assertEquals(DriverState.LOADING, state.state)
        assertEquals(DriverState.TO_FIELD, state.previousState)
        assertTrue(state.canUndo)
    }

    @Test
    fun `undo restores previous status once`() {
        viewModel.selectState(DriverState.TO_FIELD)
        viewModel.selectState(DriverState.LOADING)

        viewModel.undo()

        assertEquals(DriverState.TO_FIELD, viewModel.uiState.value.state)
        assertFalse(viewModel.uiState.value.canUndo)
    }

    @Test
    fun `selecting the same status again keeps undo target`() {
        viewModel.selectState(DriverState.TO_FIELD)
        viewModel.selectState(DriverState.LOADING)

        viewModel.selectState(DriverState.LOADING)

        assertEquals(DriverState.TO_FIELD, viewModel.uiState.value.previousState)
    }

    @Test
    fun `undo without history does nothing`() {
        viewModel.undo()

        assertEquals(DriverUiState(), viewModel.uiState.value)
    }
}
