package pl.farmtracker.data.harvest

import kotlinx.coroutines.flow.Flow
import pl.farmtracker.core.domain.Harvest
import pl.farmtracker.core.domain.Invite
import pl.farmtracker.core.domain.InviteCode
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role

/** Do którego zbioru należy zalogowana osoba. */
sealed interface Membership {
    data object Loading : Membership

    /** Zalogowany, ale jeszcze bez zbioru – zakłada nowy albo wpisuje kod zaproszenia. */
    data object None : Membership

    data class Joined(val harvest: Harvest, val me: Member) : Membership
}

sealed interface JoinResult {
    data object Joined : JoinResult

    /** Nie ma takiego kodu (literówka) albo już wygasł. */
    data object InvalidCode : JoinResult

    data object Unavailable : JoinResult
}

/**
 * Zbiór zalogowanej osoby i ludzie w nim. Zmiany ludzi (rola, usunięcie) robi tylko admin –
 * pilnują tego też reguły po stronie serwera.
 */
interface HarvestRepository {
    val membership: Flow<Membership>

    /** Ludzie w moim zbiorze, admini na górze. */
    val members: Flow<List<Member>>

    /** Nowy zbiór; zakładający zostaje jego adminem. */
    suspend fun createHarvest(name: String, myName: String): Boolean

    /**
     * Dołączenie kodem – kod działa raz (zużywa się w tym samym zapisie).
     * @param myName imię wpisane przy logowaniu numerem; `null` – imię z zaproszenia (dołączenie samym kodem)
     */
    suspend fun join(code: InviteCode, myName: String?): JoinResult

    /** Zaproszenia admina, na które jeszcze nikt nie dołączył (i ważne), najnowsze na górze. */
    val invites: Flow<List<Invite>>

    /** Nowe zaproszenie osoby na rolę; `null`, gdy nie udało się go zapisać (brak zasięgu). */
    suspend fun createInvite(role: Role, name: String, phone: String): Invite?

    /** Admin wycofuje zaproszenie – kod przestaje działać. */
    suspend fun cancelInvite(code: InviteCode)

    suspend fun changeRole(userId: String, role: Role)

    suspend fun remove(userId: String)

    /** „Cofnij" po usunięciu – przywraca osobę z tą samą rolą. */
    suspend fun restore(member: Member)
}
