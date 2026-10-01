package pl.farmtracker.feature.roles.admin

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.testing.FakePeopleRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.feature.roles.common.Coworker
import pl.farmtracker.feature.roles.common.CrewFixture

class AdminViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val crew = CrewFixture(myRole = Role.ADMIN)
    private val people = FakePeopleRepository(
        role = Role.ADMIN,
        members = listOf(
            Member("Admin", "Admin", "+48600000001", Role.ADMIN),
            Member("Rysiek", "Rysiek", "+48600000002", Role.HARVESTER),
            Member("Marek", "Marek", "+48600000003", Role.DRIVER),
            Member("Wojtek", "Wojtek", "+48600000004", Role.DRIVER),
            Member("Staszek", "Staszek", "+48600000005", Role.DRIVER),
            Member("Silos", "Silos", "+48600000006", Role.BASE),
        ),
    )
    private val viewModel by lazy { AdminViewModel(crew.watch, people) }
    private val state get() = viewModel.uiState.value

    @Test
    fun `sees who does what - loading, back to base, standing for how long - and who does not work`() =
        runTest(mainDispatcherRule.testDispatcher) {
            crew.locations.locations.value = listOf(
                crew.someone("Rysiek", Role.HARVESTER, crew.onTheField, fieldId = "f1")
                    .copy(stillSinceMillis = crew.clock.now - 7 * 60_000),
                crew.someone("Marek", Role.DRIVER, crew.onTheField, fieldId = "f1", trip = Trip.LOADING),
                crew.someone("Wojtek", Role.DRIVER, crew.onTheRoad, fieldId = null, trip = Trip.TO_BASE)
                    .copy(stillSinceMillis = crew.clock.now - 60_000),
                // Telefon zgasł dawno temu – już nie pracuje.
                crew.someone("Staszek", Role.DRIVER, crew.onTheRoad, fieldId = null, minutesAgo = 3 * 60),
            )
            viewModel.uiState.launchIn(backgroundScope)

            assertEquals(
                listOf(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false, standingMinutes = 7)),
                state.harvesters,
            )
            assertEquals(
                listOf(
                    Coworker("Marek", Role.DRIVER, fieldName = "Za lasem", isStale = false, trip = Trip.LOADING),
                    Coworker("Wojtek", Role.DRIVER, fieldName = null, isStale = false, trip = Trip.TO_BASE),
                ),
                state.drivers,
            )
            assertEquals(listOf("Staszek"), state.notWorking)
        }

    @Test
    fun `an old position does not say how long someone stands`() = runTest(mainDispatcherRule.testDispatcher) {
        crew.locations.locations.value = listOf(
            crew.someone("Marek", Role.DRIVER, crew.onTheRoad, fieldId = null, minutesAgo = 10)
                .copy(stillSinceMillis = crew.clock.now - 30 * 60_000),
        )
        viewModel.uiState.launchIn(backgroundScope)

        assertEquals(listOf(Coworker("Marek", Role.DRIVER, fieldName = null, isStale = true)), state.drivers)
    }
}
