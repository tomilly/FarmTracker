package pl.farmtracker.feature.roles.base

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewFixture

class BaseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val crew = CrewFixture()

    @Test
    fun `sees the harvester and the drivers, but not people gone for hours`() = runTest(mainDispatcherRule.testDispatcher) {
        crew.locations.locations.value = listOf(
            crew.someone("Rysiek", Role.HARVESTER, crew.onTheField, fieldId = "f1"),
            crew.someone("Marek", Role.DRIVER, crew.onTheRoad, fieldId = null),
            crew.someone("Janek", Role.DRIVER, crew.onTheRoad, fieldId = null, minutesAgo = 13 * 60),
        )
        val viewModel = BaseViewModel(crew.watch)
        viewModel.uiState.launchIn(backgroundScope)

        assertEquals(
            BaseUiState(
                harvesters = listOf(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false)),
                drivers = listOf(Coworker("Marek", Role.DRIVER, fieldName = null, isStale = false)),
            ),
            viewModel.uiState.value,
        )
    }
}
