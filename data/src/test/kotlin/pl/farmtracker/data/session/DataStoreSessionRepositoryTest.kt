package pl.farmtracker.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.farmtracker.core.domain.Role

class DataStoreSessionRepositoryTest {

    // System plików w pamięci: testy są szybkie i działają tak samo na Windows i w CI.
    private fun TestScope.createDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FakeFileSystem(),
                serializer = PreferencesSerializer,
                producePath = { "/session.preferences_pb".toPath() },
            ),
            scope = backgroundScope,
        )

    @Test
    fun `no role before one is chosen`() = runTest {
        val repository = DataStoreSessionRepository(createDataStore())

        assertNull(repository.currentRole.first())
    }

    @Test
    fun `chosen role is remembered`() = runTest {
        val repository = DataStoreSessionRepository(createDataStore())

        repository.setRole(Role.DRIVER)

        assertEquals(Role.DRIVER, repository.currentRole.first())
    }

    @Test
    fun `clearing role removes it`() = runTest {
        val repository = DataStoreSessionRepository(createDataStore())
        repository.setRole(Role.BASE)

        repository.clearRole()

        assertNull(repository.currentRole.first())
    }

    @Test
    fun `unknown stored value is treated as no role`() = runTest {
        val dataStore = createDataStore()
        dataStore.edit { it[stringPreferencesKey("role")] = "PILOT" }

        assertNull(DataStoreSessionRepository(dataStore).currentRole.first())
    }
}
