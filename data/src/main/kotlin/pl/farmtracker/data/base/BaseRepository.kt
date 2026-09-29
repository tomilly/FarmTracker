package pl.farmtracker.data.base

import kotlinx.coroutines.flow.Flow
import pl.farmtracker.core.domain.Base

/** Baza zbioru. Na razie zapis tylko na telefonie; od M3 – Firestore, wspólna dla całego zbioru. */
interface BaseRepository {
    /** Zapisana baza albo `null`, gdy jeszcze jej nie zaznaczono. */
    val base: Flow<Base?>

    suspend fun save(base: Base)

    suspend fun clear()
}
