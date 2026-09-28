package pl.farmtracker.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pl.farmtracker.core.domain.Role
import java.io.IOException
import javax.inject.Inject

class DataStoreSessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionRepository {

    override val currentRole: Flow<Role?> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            val stored = preferences[ROLE_KEY]
            Role.entries.firstOrNull { it.name == stored }
        }

    override suspend fun setRole(role: Role) {
        dataStore.edit { it[ROLE_KEY] = role.name }
    }

    override suspend fun clearRole() {
        dataStore.edit { it.remove(ROLE_KEY) }
    }

    private companion object {
        val ROLE_KEY = stringPreferencesKey("role")
    }
}
