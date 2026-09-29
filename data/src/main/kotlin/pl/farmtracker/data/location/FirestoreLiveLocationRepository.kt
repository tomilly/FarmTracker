package pl.farmtracker.data.location

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
import kotlinx.coroutines.flow.onStart
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.firebase.retryWhenNotYetMember
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.HARVESTS
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.LAT
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.LON
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.NAME
import pl.farmtracker.data.harvest.FirestoreHarvestRepository.Companion.ROLE
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import pl.farmtracker.data.session.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pozycje w zbiorze: `harvests/{id}/locations/{uid}` = `{name, role, lat, lon, time, fieldId, trip}` – jeden dokument
 * na osobę, nadpisywany. Zapis nie czeka na serwer: bez zasięgu Firestore wyśle ostatnią pozycję później.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreLiveLocationRepository @Inject constructor(
    private val harvestRepository: HarvestRepository,
    private val sessionRepository: SessionRepository,
) : LiveLocationRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override val locations: Flow<List<LiveLocation>> = harvestRepository.membership.flatMapLatest { membership ->
        if (membership !is Membership.Joined) {
            flowOf(emptyList())
        } else {
            db.collection(HARVESTS).document(membership.harvest.id).collection(LOCATIONS).snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toLiveLocation(myId = membership.me.userId) } }
                .retryWhenNotYetMember()
                .catch { error -> if (error is FirebaseFirestoreException) emit(emptyList()) else throw error }
        }
    }
        // Na start „nikt nie pracuje" – ekrany łączą pozycje z innymi danymi i nie mogą czekać na nie
        // (np. gdy serwer chwilę odmawia, a nasłuch próbuje ponownie).
        .onStart { emit(emptyList()) }

    override suspend fun publish(report: PositionReport) {
        val membership = currentMembership() ?: return
        // Debug „Zmień rolę" przykrywa rolę ze zbioru – inni widzą mnie w roli, której ekran mam otwarty.
        val role = sessionRepository.currentRole.first() ?: membership.me.role
        myLocation(membership).set(
            mapOf(
                NAME to membership.me.name,
                ROLE to role.name,
                LAT to report.point.latitude,
                LON to report.point.longitude,
                TIME to report.timeMillis,
                FIELD_ID to report.fieldId,
                TRIP to report.trip?.name,
            ),
        )
    }

    override suspend fun stopSharing() {
        val membership = currentMembership() ?: return
        myLocation(membership).delete()
    }

    private suspend fun currentMembership(): Membership.Joined? =
        harvestRepository.membership.first { it !is Membership.Loading } as? Membership.Joined

    private fun myLocation(membership: Membership.Joined) =
        db.collection(HARVESTS).document(membership.harvest.id).collection(LOCATIONS).document(membership.me.userId)

    private fun DocumentSnapshot.toLiveLocation(myId: String): LiveLocation? {
        val role = getString(ROLE)?.let { stored -> Role.entries.firstOrNull { it.name == stored } } ?: return null
        val lat = getDouble(LAT) ?: return null
        val lon = getDouble(LON) ?: return null
        return LiveLocation(
            userId = id,
            name = getString(NAME).orEmpty(),
            role = role,
            point = GeoPoint(latitude = lat, longitude = lon),
            timeMillis = getLong(TIME) ?: return null,
            fieldId = getString(FIELD_ID),
            trip = getString(TRIP)?.let { stored -> Trip.entries.firstOrNull { it.name == stored } },
            isMe = id == myId,
        )
    }

    private companion object {
        const val LOCATIONS = "locations"
        const val TIME = "time"
        const val FIELD_ID = "fieldId"
        const val TRIP = "trip"
    }
}
