package pl.farmtracker.feature.roles.picker

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.testing.FakeSessionRepository
import pl.farmtracker.core.testing.MainDispatcherRule

class RolePickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `selecting a role saves it in the session`() {
        val sessionRepository = FakeSessionRepository()
        val viewModel = RolePickerViewModel(sessionRepository)

        viewModel.selectRole(Role.HARVESTER)

        assertEquals(Role.HARVESTER, sessionRepository.currentRole.value)
    }
}
