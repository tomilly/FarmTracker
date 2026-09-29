package pl.farmtracker.data.base

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
import pl.farmtracker.core.domain.Base
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreBaseRepository internal constructor(
    private val dataStore: DataStore<StoredBase>,
) : BaseRepository {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = StoredBaseSerializer,
                producePath = { File(context.filesDir, "datastore/base.json").toOkioPath() },
            ),
        ),
    )

    override val base: Flow<Base?> = dataStore.data
        // Nieczytelny plik nie może wyłożyć aplikacji – wtedy po prostu bazy nie ma.
        .catch { error -> if (error is IOException) emit(StoredBase()) else throw error }
        .map { it.toDomain() }

    override suspend fun save(base: Base) {
        dataStore.updateData { base.toStored() }
    }

    override suspend fun clear() {
        dataStore.updateData { StoredBase() }
    }
}
