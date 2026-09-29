package pl.farmtracker.data.base

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.farmtracker.core.domain.Base
import pl.farmtracker.core.domain.geo.GeoPoint

class DataStoreBaseRepositoryTest {

    private val fileSystem = FakeFileSystem()
    private val path = "/base.json".toPath()

    private fun TestScope.createRepository() = DataStoreBaseRepository(
        DataStoreFactory.create(
            storage = OkioStorage(fileSystem, StoredBaseSerializer) { path },
            scope = backgroundScope,
        ),
    )

    @Test
    fun `no base at start`() = runTest {
        assertNull(createRepository().base.first())
    }

    @Test
    fun `saved base comes back and can be cleared`() = runTest {
        val repository = createRepository()
        val base = Base(GeoPoint(51.95, 18.62))

        repository.save(base)
        assertEquals(base, repository.base.first())

        repository.clear()
        assertNull(repository.base.first())
    }

    @Test
    fun `corrupted file means no base instead of a crash`() = runTest {
        fileSystem.write(path) { writeUtf8("to nie jest json") }

        assertNull(createRepository().base.first())
    }
}
