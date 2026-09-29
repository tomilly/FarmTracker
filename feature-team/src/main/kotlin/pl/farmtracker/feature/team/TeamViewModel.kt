package pl.farmtracker.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Invite
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.canChangeOrRemove
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import javax.inject.Inject

/** Co widać na ekranie „Ludzie": lista, świeże zaproszenie albo jedna osoba (rola, usunięcie). */
sealed interface TeamStep {
    data object List : TeamStep

    data class ShowInvite(val invite: Invite) : TeamStep

    data class EditMember(val userId: String) : TeamStep
}

/** Osoba na liście z tym, co admin może z nią zrobić. */
data class TeamMember(
    val member: Member,
    val isMe: Boolean,
    /** Ostatni admin – bez zmiany roli i usuwania. */
    val canChange: Boolean,
) {
    val canRemove: Boolean get() = canChange && !isMe
}

data class TeamUiState(
    val step: TeamStep = TeamStep.List,
    val harvestName: String = "",
    val members: List<TeamMember> = emptyList(),
    /** Czekamy na zapisanie zaproszenia (tej roli). */
    val invitingRole: Role? = null,
    val inviteFailed: Boolean = false,
) {
    val editedMember: TeamMember?
        get() = (step as? TeamStep.EditMember)?.let { edit -> members.firstOrNull { it.member.userId == edit.userId } }
}

/**
 * Ekran admina „Ludzie": zaproszenia na rolę (kod do wysłania SMS-em / WhatsAppem), zmiana roli,
 * usunięcie ze zbioru – od razu, z „Cofnij" zamiast pytania „czy na pewno?".
 */
@HiltViewModel
class TeamViewModel @Inject constructor(
    private val harvestRepository: HarvestRepository,
) : ViewModel() {

    private val step = MutableStateFlow<TeamStep>(TeamStep.List)
    private val inviting = MutableStateFlow<Role?>(null)
    private val inviteFailed = MutableStateFlow(false)

    val uiState: StateFlow<TeamUiState> = combine(
        harvestRepository.membership,
        harvestRepository.members,
        step,
        inviting,
        inviteFailed,
    ) { membership, members, step, inviting, failed ->
        val joined = membership as? Membership.Joined
        TeamUiState(
            step = step,
            harvestName = joined?.harvest?.name.orEmpty(),
            members = members
                .sortedWith(compareBy({ it.role != Role.ADMIN }, { it.name }))
                .map { TeamMember(it, isMe = it.userId == joined?.me?.userId, canChange = members.canChangeOrRemove(it)) },
            invitingRole = inviting,
            inviteFailed = failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamUiState())

    private val _recentlyRemoved = MutableStateFlow<Member?>(null)

    /** Usunięta osoba – do „Cofnij" na liście. */
    val recentlyRemoved: StateFlow<Member?> = _recentlyRemoved.asStateFlow()

    fun invite(role: Role) {
        if (inviting.value != null) return
        inviting.value = role
        inviteFailed.value = false
        viewModelScope.launch {
            val invite = harvestRepository.createInvite(role)
            inviting.value = null
            if (invite == null) inviteFailed.value = true else step.value = TeamStep.ShowInvite(invite)
        }
    }

    fun openMember(userId: String) = step.update { TeamStep.EditMember(userId) }

    fun backToList() = step.update { TeamStep.List }

    /** Rola zmienia się od razu – bez „Zapisz". */
    fun changeRole(member: TeamMember, role: Role) {
        if (!member.canChange || member.member.role == role) return
        viewModelScope.launch { harvestRepository.changeRole(member.member.userId, role) }
    }

    fun remove(member: TeamMember) {
        if (!member.canRemove) return
        step.value = TeamStep.List
        _recentlyRemoved.value = member.member
        viewModelScope.launch { harvestRepository.remove(member.member.userId) }
    }

    fun undoRemove() {
        val member = _recentlyRemoved.value ?: return
        _recentlyRemoved.value = null
        viewModelScope.launch { harvestRepository.restore(member) }
    }

    fun dismissRemoved() {
        _recentlyRemoved.value = null
    }
}
