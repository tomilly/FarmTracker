package pl.farmtracker.data.base

import com.google.firebase.firestore.FieldValue
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
import pl.farmtracker.core.domain.Base
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.firebase.retryWhenNotYetMember
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.BASE
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.HARVESTS
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.LAT
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.LON
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import javax.inject.Inject
import javax.inject.Singleton

/** Baza zbioru w dokumencie zbioru (`harvests/{id}.base` = `{lat, lon}`) – wspólna dla wszystkich. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreBaseRepository @Inject constructor(
    private val harvestRepository: HarvestRepository,
) : BaseRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override val base: Flow<Base?> = harvestRepository.membership.flatMapLatest { membership ->
        if (membership !is Membership.Joined) {
            flowOf(null)
        } else {
            harvestRef(membership.harvest.id).snapshots()
                .map { snapshot ->
                    val point = snapshot.get(BASE) as? Map<*, *>
                    val lat = (point?.get(LAT) as? Number)?.toDouble()
                    val lon = (point?.get(LON) as? Number)?.toDouble()
                    if (lat != null && lon != null) Base(GeoPoint(latitude = lat, longitude = lon)) else null
                }
                .retryWhenNotYetMember()
                .catch { error -> if (error is FirebaseFirestoreException) emit(null) else throw error }
        }
    }

    override suspend fun save(base: Base) {
        val harvestId = currentHarvestId() ?: return
        harvestRef(harvestId).update(BASE, mapOf(LAT to base.location.latitude, LON to base.location.longitude))
    }

    override suspend fun clear() {
        val harvestId = currentHarvestId() ?: return
        harvestRef(harvestId).update(BASE, FieldValue.delete())
    }

    private suspend fun currentHarvestId(): String? =
        (harvestRepository.membership.first { it !is Membership.Loading } as? Membership.Joined)?.harvest?.id

    private fun harvestRef(harvestId: String) = db.collection(HARVESTS).document(harvestId)
}
