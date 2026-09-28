package pl.farmtracker.feature.roles.base

import org.junit.Assert.assertTrue
import org.junit.Test

class BaseViewModelTest {

    @Test
    fun `nobody is incoming before live data exists`() {
        assertTrue(BaseViewModel().uiState.value.incomingDrivers.isEmpty())
    }
}
