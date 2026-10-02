package pl.farmtracker.data.harvest

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import pl.farmtracker.core.domain.Harvest
import pl.farmtracker.core.domain.Invite
import pl.farmtracker.core.domain.InviteCode
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.base.DataStoreBaseRepository
import pl.farmtracker.data.field.DataStoreFieldRepository
import pl.farmtracker.data.field.fieldsOf
import pl.farmtracker.data.field.toFirestoreData
import pl.farmtracker.data.firebase.changes
import pl.farmtracker.data.firebase.retryWhenNotYetMember
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zbiór w Firestore:
 * - `users/{uid}` – do którego zbioru należę (`harvestId`),
 * - `harvests/{id}` – nazwa, właściciel, baza; `harvests/{id}/members/{uid}` – imię, telefon, rola,
 * - `invites/{kod}` – zaproszenie na rolę, z datą ważności.
 * Kto co może – `firestore.rules` w katalogu głównym repozytorium.
 *
 * Firestore trzyma kopię na telefonie: zapisy bez zasięgu idą od razu do kopii i wysyłają się później,
 * więc większość zmian nie czeka na serwer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreHarvestRepository @Inject constructor(
    private val localFields: DataStoreFieldRepository,
    private val localBase: DataStoreBaseRepository,
) : HarvestRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val userId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Zwiększane po dołączeniu i założeniu zbioru – nasłuch członkostwa zaczyna się od nowa. Po usunięciu
     * ze zbioru kończy się na „nie należę", a ponowne dołączenie do tego samego zbioru nie zmienia `users/{uid}`,
     * więc sam by się nie obudził.
     */
    private val reconnects = MutableStateFlow(0)

    override val membership: Flow<Membership> = combine(userId, reconnects) { uid, _ -> uid }.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(Membership.None)
        } else {
            db.collection(USERS).document(uid).changes().flatMapLatest { user ->
                val harvestId = user.getString(HARVEST_ID)
                if (harvestId == null) flowOf(Membership.None) else joined(harvestId, uid)
            }
        }
    }

    /** Zbiór i moja karta w nim. Brak karty (usunięto mnie) albo brak dostępu = nie należę już do zbioru. */
    private fun joined(harvestId: String, uid: String): Flow<Membership> = combine(
        harvestRef(harvestId).changes(),
        harvestRef(harvestId).collection(MEMBERS).document(uid).changes(),
    ) { harvest, me ->
        val member = me.toMember()
        if (!harvest.exists() || member == null) {
            Membership.None
        } else {
            Membership.Joined(Harvest(harvestId, harvest.getString(NAME).orEmpty()), member)
        }
    }
        .retryWhenNotYetMember()
        .catch { error -> if (error is FirebaseFirestoreException) emit(Membership.None) else throw error }

    override val members: Flow<List<Member>> = membership.flatMapLatest { membership ->
        if (membership !is Membership.Joined) {
            flowOf(emptyList())
        } else {
            harvestRef(membership.harvest.id).collection(MEMBERS).changes()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toMember() } }
                .retryWhenNotYetMember()
                .catch { error -> if (error is FirebaseFirestoreException) emit(emptyList()) else throw error }
        }
    }

    override suspend fun createHarvest(name: String, myName: String): Boolean {
        val user = auth.currentUser ?: return false
        val harvest = db.collection(HARVESTS).document()
        val batch = db.batch()
            .set(harvest, mapOf(NAME to name, OWNER_ID to user.uid, CREATED_AT to FieldValue.serverTimestamp()))
            .set(harvest.collection(MEMBERS).document(user.uid), memberData(myName, user.phoneNumber, Role.ADMIN))
            .set(db.collection(USERS).document(user.uid), mapOf(HARVEST_ID to harvest.id))
        // Bez zasięgu zapis czeka w kopii na telefonie, a ekran i tak przejdzie dalej – nie zakładamy drugi raz.
        if (!commitOrQueue { batch.commit() }) return false
        reconnects.update { it + 1 }
        moveLocalDataTo(harvest.id)
        return true
    }

    /**
     * Pola i baza zaznaczone na tym telefonie przed założeniem zbioru przechodzą do niego – admin nie
     * rysuje ich drugi raz. Osobno po utworzeniu zbioru: reguły wpuszczają zapis pól dopiero adminowi.
     */
    private suspend fun moveLocalDataTo(harvestId: String) {
        localFields.fields.first().forEach { field -> db.fieldsOf(harvestId).document(field.id).set(field.toFirestoreData()) }
        localBase.base.first()?.let { base ->
            harvestRef(harvestId).update(BASE, mapOf(LAT to base.location.latitude, LON to base.location.longitude))
        }
    }

    override suspend fun join(code: InviteCode, myName: String?): JoinResult {
        val user = auth.currentUser ?: return JoinResult.Unavailable
        // Zaproszenie musi przyjść z serwera – kopia na telefonie mogłaby być nieaktualna.
        val invite = try {
            withTimeout(SERVER_TIMEOUT_MS) { db.collection(INVITES).document(code.digits).get().await() }
        } catch (error: FirebaseFirestoreException) {
            return JoinResult.Unavailable
        } catch (error: TimeoutCancellationException) {
            return JoinResult.Unavailable
        }
        val harvestId = invite.getString(HARVEST_ID)
        val role = invite.getString(ROLE)?.toRole()
        val expiresAt = invite.getTimestamp(EXPIRES_AT)
        if (harvestId == null || role == null || expiresAt == null || expiresAt.toDate().before(Date())) {
            return JoinResult.InvalidCode
        }
        val name = myName?.takeIf { it.isNotBlank() } ?: invite.getString(NAME).orEmpty()
        // Numer z logowania; przy samym kodzie – ten, na który admin wysłał zaproszenie (do kontaktu w „Ludzie").
        val phone = user.phoneNumber?.takeIf { it.isNotBlank() } ?: invite.getString(PHONE)
        val batch = db.batch()
            .set(
                harvestRef(harvestId).collection(MEMBERS).document(user.uid),
                memberData(name, phone, role) + (INVITE_CODE to code.digits),
            )
            .set(db.collection(USERS).document(user.uid), mapOf(HARVEST_ID to harvestId))
            // Kod działa raz – zużywa się razem z dołączeniem (reguły serwera tego pilnują).
            .delete(invite.reference)
        if (!commitOrQueue { batch.commit() }) return JoinResult.Unavailable
        reconnects.update { it + 1 }
        return JoinResult.Joined
    }

    override val invites: Flow<List<Invite>> = membership.flatMapLatest { membership ->
        if (membership !is Membership.Joined || membership.me.role != Role.ADMIN) {
            flowOf(emptyList())
        } else {
            db.collection(INVITES).whereEqualTo(HARVEST_ID, membership.harvest.id).changes()
                .map { snapshot ->
                    val now = System.currentTimeMillis()
                    snapshot.documents.mapNotNull { it.toInvite() }
                        .filter { it.isValidAt(now) }
                        .sortedByDescending { it.expiresAtMillis }
                }
                .retryWhenNotYetMember()
                .catch { error -> if (error is FirebaseFirestoreException) emit(emptyList()) else throw error }
        }
    }

    override suspend fun createInvite(role: Role, name: String, phone: String): Invite? {
        val harvestId = (membership.first() as? Membership.Joined)?.harvest?.id ?: return null
        val user = auth.currentUser ?: return null
        // Wylosowany kod może być już zajęty (reguły nie pozwalają nadpisać cudzego) – wtedy kolejny.
        repeat(CODE_ATTEMPTS) {
            val invite = Invite(
                InviteCode.random(),
                harvestId,
                role,
                System.currentTimeMillis() + Invite.VALID_FOR_MILLIS,
                name,
                phone,
            )
            try {
                // Zaproszenie musi dotrzeć na serwer, zanim ktoś wpisze kod – dlatego tu czekamy.
                withTimeout(SERVER_TIMEOUT_MS) {
                    db.collection(INVITES).document(invite.code.digits).set(
                        mapOf(
                            HARVEST_ID to harvestId,
                            ROLE to role.name,
                            NAME to name,
                            PHONE to phone,
                            EXPIRES_AT to Timestamp(Date(invite.expiresAtMillis)),
                            CREATED_BY to user.uid,
                        ),
                    ).await()
                }
                return invite
            } catch (error: FirebaseFirestoreException) {
                if (error.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) return null
            } catch (error: TimeoutCancellationException) {
                return null
            }
        }
        return null
    }

    override suspend fun cancelInvite(code: InviteCode) {
        db.collection(INVITES).document(code.digits).delete()
    }

    override suspend fun changeRole(userId: String, role: Role) {
        memberRef(userId)?.update(ROLE, role.name)
    }

    override suspend fun remove(userId: String) {
        memberRef(userId)?.delete()
    }

    override suspend fun restore(member: Member) {
        memberRef(member.userId)?.set(memberData(member.name, member.phone, member.role))
    }

    private suspend fun memberRef(userId: String) =
        (membership.first() as? Membership.Joined)?.let { harvestRef(it.harvest.id).collection(MEMBERS).document(userId) }

    private fun harvestRef(harvestId: String) = db.collection(HARVESTS).document(harvestId)

    /** `true`: zapisane albo czeka w kopii na telefonie (brak zasięgu); `false`: serwer odmówił. */
    private suspend fun commitOrQueue(commit: () -> Task<Void>): Boolean = try {
        withTimeout(SERVER_TIMEOUT_MS) { commit().await() }
        true
    } catch (error: TimeoutCancellationException) {
        true
    } catch (error: FirebaseFirestoreException) {
        false
    }

    private fun memberData(name: String, phone: String?, role: Role) =
        mapOf(NAME to name, PHONE to phone.orEmpty(), ROLE to role.name)

    private fun DocumentSnapshot.toMember(): Member? {
        if (!exists()) return null
        val role = getString(ROLE)?.toRole() ?: return null
        return Member(userId = id, name = getString(NAME).orEmpty(), phone = getString(PHONE).orEmpty(), role = role)
    }

    private fun String.toRole(): Role? = Role.entries.firstOrNull { it.name == this }

    private fun DocumentSnapshot.toInvite(): Invite? {
        val code = InviteCode.parse(id) ?: return null
        return Invite(
            code = code,
            harvestId = getString(HARVEST_ID) ?: return null,
            role = getString(ROLE)?.toRole() ?: return null,
            expiresAtMillis = getTimestamp(EXPIRES_AT)?.toDate()?.time ?: return null,
            name = getString(NAME).orEmpty(),
            phone = getString(PHONE).orEmpty(),
        )
    }

    companion object {
        const val USERS = "users"
        const val HARVESTS = "harvests"
        const val MEMBERS = "members"
        const val INVITES = "invites"

        const val HARVEST_ID = "harvestId"
        const val NAME = "name"
        const val OWNER_ID = "ownerId"
        const val CREATED_AT = "createdAt"
        const val PHONE = "phone"
        const val ROLE = "role"
        const val INVITE_CODE = "inviteCode"
        const val EXPIRES_AT = "expiresAt"
        const val CREATED_BY = "createdBy"
        const val BASE = "base"
        const val LAT = "lat"
        const val LON = "lon"

        private const val SERVER_TIMEOUT_MS = 15_000L
        private const val CODE_ATTEMPTS = 3
    }
}
