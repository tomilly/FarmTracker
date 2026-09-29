package pl.farmtracker.data.field

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.FieldStatus
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon

class DataStoreFieldRepositoryTest {

    private val fileSystem = FakeFileSystem()
    private val path = "/fields.json".toPath()

    private fun TestScope.createRepository() = DataStoreFieldRepository(
        DataStoreFactory.create(
            storage = OkioStorage(fileSystem, StoredFieldsSerializer) { path },
            scope = backgroundScope,
        ),
    )

    private val field = Field(
        id = "f1",
        name = "Za lasem",
        color = FieldColor.ORANGE,
        shape = listOf(
            GeoPolygon(
                outer = listOf(GeoPoint(50.0, 17.0), GeoPoint(50.0, 17.01), GeoPoint(50.01, 17.01)),
                holes = listOf(listOf(GeoPoint(50.001, 17.005), GeoPoint(50.002, 17.006), GeoPoint(50.002, 17.005))),
            ),
        ),
        parcelIds = listOf("160802_2.0012.345"),
        entryPoints = listOf(GeoPoint(50.0, 17.005), GeoPoint(50.01, 17.005)),
        status = FieldStatus.ACTIVE,
        order = 2,
    )

    @Test
    fun `no fields at start`() = runTest {
        assertTrue(createRepository().fields.first().isEmpty())
    }

    @Test
    fun `saved field comes back unchanged`() = runTest {
        val repository = createRepository()

        repository.save(field)

        assertEquals(listOf(field), repository.fields.first())
    }

    @Test
    fun `saving the same id replaces the field`() = runTest {
        val repository = createRepository()
        repository.save(field)

        repository.save(field.copy(name = "Przy drodze"))

        assertEquals(listOf("Przy drodze"), repository.fields.first().map { it.name })
    }

    @Test
    fun `fields are ordered by order then name, and can be deleted`() = runTest {
        val repository = createRepository()
        repository.save(field.copy(id = "a", name = "Zagon", order = 1))
        repository.save(field.copy(id = "b", name = "Bagno", order = 1))
        repository.save(field.copy(id = "c", name = "Łąka", order = 0))

        assertEquals(listOf("c", "b", "a"), repository.fields.first().map { it.id })

        repository.delete("b")

        assertEquals(listOf("c", "a"), repository.fields.first().map { it.id })
    }

    @Test
    fun `corrupted file shows no fields instead of crashing`() = runTest {
        fileSystem.write(path) { writeUtf8("to nie jest json") }

        assertTrue(createRepository().fields.first().isEmpty())
    }

    @Test
    fun `a file from before several entries keeps its single entry`() = runTest {
        fileSystem.write(path) {
            writeUtf8(
                """{"fields":[{"id":"f1","name":"Stare","color":"BLUE","polygons":[],"entry":{"lat":50.0,"lon":17.0}}]}""",
            )
        }

        assertEquals(listOf(GeoPoint(50.0, 17.0)), createRepository().fields.first().single().entryPoints)
    }
}
