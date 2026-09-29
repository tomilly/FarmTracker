package pl.farmtracker.data.field

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pl.farmtracker.core.domain.Field
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.HARVESTS
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pola zbioru w Firestore (`harvests/{id}/fields/{fieldId}`) – wspólne dla wszystkich w zbiorze.
 * Kształt zapisany jako JSON (ten sam format co na telefonie): Firestore nie przyjmuje tablic w tablicach,
 * a wielokąty z dziurami to właśnie takie tablice. Nazwa i kolejność obok – czytelne w konsoli.
 *
 * Zapis nie czeka na serwer: pole od razu jest w kopii na telefonie i wyśle się, gdy wróci zasięg.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreFieldRepository @Inject constructor(
    private val harvestRepository: HarvestRepository,
) : FieldRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override val fields: Flow<List<Field>> = harvestRepository.membership.flatMapLatest { membership ->
        if (membership !is Membership.Joined) {
            flowOf(emptyList())
        } else {
            db.fieldsOf(membership.harvest.id).snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toField() }.sortedWith(compareBy({ it.order }, { it.name }))
                }
                .catch { error -> if (error is FirebaseFirestoreException) emit(emptyList()) else throw error }
        }
    }

    override suspend fun save(field: Field) {
        val harvestId = currentHarvestId() ?: return
        db.fieldsOf(harvestId).document(field.id).set(field.toFirestoreData())
    }

    override suspend fun delete(id: String) {
        val harvestId = currentHarvestId() ?: return
        db.fieldsOf(harvestId).document(id).delete()
    }

    private suspend fun currentHarvestId(): String? =
        (harvestRepository.membership.first { it !is Membership.Loading } as? Membership.Joined)?.harvest?.id

    /** Uszkodzony zapis pomijamy – jedno złe pole nie może schować wszystkich. */
    private fun DocumentSnapshot.toField(): Field? {
        val data = getString(DATA) ?: return null
        return try {
            FirestoreJson.decodeFromString(StoredField.serializer(), data).toDomain()
        } catch (error: SerializationException) {
            null
        } catch (error: IllegalArgumentException) {
            null
        }
    }
}

private const val FIELDS = "fields"
private const val NAME = "name"
private const val ORDER = "order"
private const val DATA = "data"

private val FirestoreJson = Json { ignoreUnknownKeys = true }

internal fun FirebaseFirestore.fieldsOf(harvestId: String): CollectionReference =
    collection(HARVESTS).document(harvestId).collection(FIELDS)

internal fun Field.toFirestoreData(): Map<String, Any> = mapOf(
    NAME to name,
    ORDER to order,
    DATA to FirestoreJson.encodeToString(StoredField.serializer(), toStored()),
)
