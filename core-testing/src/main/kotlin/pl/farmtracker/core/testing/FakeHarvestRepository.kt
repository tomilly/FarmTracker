package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import pl.farmtracker.core.domain.Harvest
import pl.farmtracker.core.domain.Invite
import pl.farmtracker.core.domain.InviteCode
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.JoinResult
import pl.farmtracker.data.harvest.Membership

/** Zbiór w pamięci. [available] = `false` udaje brak zasięgu. */
class FakeHarvestRepository(
    initial: Membership = Membership.None,
    members: List<Member> = emptyList(),
    private val me: Member = Member("me", "", "+48600000001", Role.ADMIN),
) : HarvestRepository {

    private val _membership = MutableStateFlow(initial)
    override val membership: StateFlow<Membership> = _membership

    private val _members = MutableStateFlow(members)
    override val members: StateFlow<List<Member>> = _members

    var available = true
    val invites = mutableListOf<Invite>()
    var nextCode = InviteCode("482913")
    var now = 0L

    override suspend fun createHarvest(name: String, myName: String): Boolean {
        if (!available) return false
        val admin = me.copy(name = myName, role = Role.ADMIN)
        _members.value = listOf(admin)
        _membership.value = Membership.Joined(Harvest("h-1", name), admin)
        return true
    }

    override suspend fun join(code: InviteCode, myName: String): JoinResult {
        if (!available) return JoinResult.Unavailable
        val invite = invites.firstOrNull { it.code == code && it.isValidAt(now) } ?: return JoinResult.InvalidCode
        val member = me.copy(name = myName, role = invite.role)
        _members.update { it + member }
        _membership.value = Membership.Joined(Harvest(invite.harvestId, "Kukurydza"), member)
        return JoinResult.Joined
    }

    override suspend fun createInvite(role: Role): Invite? {
        if (!available) return null
        return Invite(nextCode, "h-1", role, now + Invite.VALID_FOR_MILLIS).also { invites += it }
    }

    override suspend fun changeRole(userId: String, role: Role) {
        _members.update { list -> list.map { if (it.userId == userId) it.copy(role = role) else it } }
    }

    override suspend fun remove(userId: String) {
        _members.update { list -> list.filterNot { it.userId == userId } }
    }

    override suspend fun restore(member: Member) {
        _members.update { it + member }
    }
}
