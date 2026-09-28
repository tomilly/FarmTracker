package pl.farmtracker.data.session

import kotlinx.coroutines.flow.Flow
import pl.farmtracker.core.domain.Role

/**
 * Rola zalogowanej osoby.
 *
 * M0: rolę wybiera się ręcznie na ekranie tymczasowym. Od M3 przyjdzie z zaproszenia (Firebase).
 */
interface SessionRepository {
    /** Bieżąca rola albo `null`, gdy jeszcze jej nie wybrano. */
    val currentRole: Flow<Role?>

    suspend fun setRole(role: Role)

    suspend fun clearRole()
}
