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
import pl.farmtracker.core.domain.normalizePhoneNumber
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import javax.inject.Inject

/** Co widać na ekranie „Ludzie": lista, nowe zaproszenie (kto), gotowy kod albo jedna osoba (rola, usunięcie). */
sealed interface TeamStep {
    data object List : TeamStep

    /** Imię i numer zapraszanej osoby. */
    data class NewInvite(val role: Role) : TeamStep

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

/** Kogo admin zaprasza: imię (tak zobaczą go inni) i numer, na który pójdzie SMS z kodem (można pominąć). */
data class InviteDraft(
    val name: String = "",
    val phone: String = "",
    val invalidPhone: Boolean = false,
) {
    val canCreate: Boolean get() = name.isNotBlank()
}

data class TeamUiState(
    val step: TeamStep = TeamStep.List,
    val harvestName: String = "",
    val members: List<TeamMember> = emptyList(),
    /** Zaproszeni, którzy jeszcze nie wpisali kodu. */
    val invites: List<Invite> = emptyList(),
    val draft: InviteDraft = InviteDraft(),
    /** Czekamy na zapisanie zaproszenia. */
    val inviting: Boolean = false,
    val inviteFailed: Boolean = false,
) {
    val editedMember: TeamMember?
        get() = (step as? TeamStep.EditMember)?.let { edit -> members.firstOrNull { it.member.userId == edit.userId } }
}

/**
 * Ekran admina „Ludzie": zaproszenie konkretnej osoby (imię, numer → kod SMS-em; osoba wpisuje tylko kod),
 * lista zaproszonych, zmiana roli, usunięcie ze zbioru – od razu, z „Cofnij" zamiast pytania „czy na pewno?".
 */
@HiltViewModel
class TeamViewModel @Inject constructor(
    private val harvestRepository: HarvestRepository,
) : ViewModel() {

    private val step = MutableStateFlow<TeamStep>(TeamStep.List)
    private val draft = MutableStateFlow(InviteDraft())
    private val inviting = MutableStateFlow(false)
    private val inviteFailed = MutableStateFlow(false)

    private val people = combine(harvestRepository.membership, harvestRepository.members, harvestRepository.invites) {
            membership, members, invites ->
        Triple(membership as? Membership.Joined, members, invites)
    }

    val uiState: StateFlow<TeamUiState> = combine(people, step, draft, inviting, inviteFailed) {
            (joined, members, invites), step, draft, inviting, failed ->
        TeamUiState(
            step = step,
            harvestName = joined?.harvest?.name.orEmpty(),
            members = members
                .sortedWith(compareBy({ it.role != Role.ADMIN }, { it.name }))
                .map { TeamMember(it, isMe = it.userId == joined?.me?.userId, canChange = members.canChangeOrRemove(it)) },
            invites = invites,
            draft = draft,
            inviting = inviting,
            inviteFailed = failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamUiState())

    private val _recentlyRemoved = MutableStateFlow<Member?>(null)

    /** Usunięta osoba – do „Cofnij" na liście. */
    val recentlyRemoved: StateFlow<Member?> = _recentlyRemoved.asStateFlow()

    /** „Zaproś kierowcę" – najpierw kto: imię i numer. */
    fun startInvite(role: Role) {
        draft.value = InviteDraft()
        inviteFailed.value = false
        step.value = TeamStep.NewInvite(role)
    }

    fun onInviteNameChanged(name: String) = draft.update { it.copy(name = name) }

    fun onInvitePhoneChanged(phone: String) = draft.update { it.copy(phone = phone, invalidPhone = false) }

    fun createInvite() {
        val role = (step.value as? TeamStep.NewInvite)?.role ?: return
        val typed = draft.value
        if (!typed.canCreate || inviting.value) return
        // Numer można pominąć (kod podyktowany), ale wpisany musi być numerem – na niego pójdzie SMS.
        val phone = if (typed.phone.isBlank()) "" else normalizePhoneNumber(typed.phone)
        if (phone == null) {
            draft.update { it.copy(invalidPhone = true) }
            return
        }
        inviting.value = true
        inviteFailed.value = false
        viewModelScope.launch {
            val invite = harvestRepository.createInvite(role, typed.name.trim(), phone)
            inviting.value = false
            if (invite == null) inviteFailed.value = true else step.value = TeamStep.ShowInvite(invite)
        }
    }

    /** Zaproszony jeszcze nie dołączył – kod jeszcze raz (np. wysłać ponownie). */
    fun openInvite(invite: Invite) = step.update { TeamStep.ShowInvite(invite) }

    /** Wycofanie zaproszenia – kod przestaje działać. */
    fun cancelInvite(invite: Invite) {
        step.value = TeamStep.List
        viewModelScope.launch { harvestRepository.cancelInvite(invite.code) }
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
