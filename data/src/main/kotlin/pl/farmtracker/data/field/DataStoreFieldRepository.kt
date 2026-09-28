package pl.farmtracker.data.field

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import pl.farmtracker.core.domain.Field
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreFieldRepository internal constructor(
    private val dataStore: DataStore<StoredFields>,
) : FieldRepository {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = StoredFieldsSerializer,
                producePath = { File(context.filesDir, "datastore/fields.json").toOkioPath() },
            ),
        ),
    )

    override val fields: Flow<List<Field>> = dataStore.data
        // Nieczytelny plik nie może wyłożyć aplikacji – pokazujemy brak pól (M3: dane wrócą z serwera).
        .catch { error -> if (error is IOException) emit(StoredFields()) else throw error }
        .map { stored -> stored.fields.map { it.toDomain() }.sortedWith(compareBy({ it.order }, { it.name })) }

    override suspend fun save(field: Field) {
        dataStore.updateData { stored ->
            val others = stored.fields.filterNot { it.id == field.id }
            stored.copy(fields = others + field.toStored())
        }
    }

    override suspend fun delete(id: String) {
        dataStore.updateData { stored -> stored.copy(fields = stored.fields.filterNot { it.id == id }) }
    }
}
