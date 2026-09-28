package pl.farmtracker.data.field

import kotlinx.coroutines.flow.Flow
import pl.farmtracker.core.domain.Field

/**
 * Pola zbioru. M2: zapis tylko na telefonie. Od M3 – Firestore (wspólne dla wszystkich w zbiorze,
 * z pamięcią podręczną offline); interfejs zostaje ten sam.
 */
interface FieldRepository {
    /** Pola w kolejności [Field.order], potem nazwy. */
    val fields: Flow<List<Field>>

    /** Dodaje nowe pole albo zastępuje istniejące o tym samym id. */
    suspend fun save(field: Field)

    suspend fun delete(id: String)
}
