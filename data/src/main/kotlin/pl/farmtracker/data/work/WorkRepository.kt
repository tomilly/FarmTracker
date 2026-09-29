package pl.farmtracker.data.work

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Czy ta osoba pracuje – od „Zaczynam pracę" do „Kończę pracę" telefon udostępnia lokalizację (CLAUDE.md).
 * Zapamiętane na telefonie: po ponownym otwarciu aplikacji praca trwa dalej.
 */
interface WorkRepository {
    val isWorking: Flow<Boolean>

    suspend fun setWorking(working: Boolean)
}

class DataStoreWorkRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : WorkRepository {

    override val isWorking: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[WORKING_KEY] == true }
        .distinctUntilChanged()

    override suspend fun setWorking(working: Boolean) {
        dataStore.edit { it[WORKING_KEY] = working }
    }

    private companion object {
        val WORKING_KEY = booleanPreferencesKey("working")
    }
}
