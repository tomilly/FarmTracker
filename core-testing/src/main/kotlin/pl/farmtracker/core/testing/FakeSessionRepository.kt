package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.session.SessionRepository

class FakeSessionRepository(initialRole: Role? = null) : SessionRepository {

    private val role = MutableStateFlow(initialRole)

    override val currentRole: StateFlow<Role?> = role

    override suspend fun setRole(role: Role) {
        this.role.value = role
    }

    override suspend fun clearRole() {
        role.value = null
    }
}
