package pl.farmtracker.feature.team

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.Invite
import pl.farmtracker.core.domain.InviteCode
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.FarmTrackerScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

/** Kogo admin zaprasza najczęściej – w tej kolejności. */
private val InviteRoles = listOf(
    Role.DRIVER to R.string.team_invite_driver,
    Role.HARVESTER to R.string.team_invite_harvester,
    Role.BASE to R.string.team_invite_base,
)

/** Kolejność ról przy zmianie – jak na ekranie wyboru roli. */
private val RoleOrder = listOf(Role.HARVESTER, Role.DRIVER, Role.BASE, Role.ADMIN)

/** Admin → „Ludzie": zaproszenia, lista osób, rola i usuwanie. */
@Composable
fun TeamScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentlyRemoved by viewModel.recentlyRemoved.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val removed = recentlyRemoved
    if (removed != null) {
        val message = stringResource(R.string.team_removed, removed.name)
        val undo = stringResource(R.string.team_undo)
        LaunchedEffect(removed) {
            val result = snackbarHostState.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove() else viewModel.dismissRemoved()
        }
    }

    when (val step = uiState.step) {
        TeamStep.List -> TeamList(
            uiState = uiState,
            onBack = onBack,
            onInvite = viewModel::invite,
            onOpenMember = viewModel::openMember,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        )
        is TeamStep.ShowInvite -> {
            BackHandler(onBack = viewModel::backToList)
            val roleLabel = stringResource(RoleUi.labelRes(step.invite.role))
            val shareText = stringResource(
                R.string.team_invite_share_text,
                uiState.harvestName,
                roleLabel,
                step.invite.code.display,
            )
            InviteStep(
                invite = step.invite,
                onShare = { context.share(shareText) },
                onDone = viewModel::backToList,
                modifier = modifier,
            )
        }
        is TeamStep.EditMember -> {
            BackHandler(onBack = viewModel::backToList)
            // Osoba mogła zniknąć (usunięta na innym telefonie) – wtedy wracamy do listy.
            val member = uiState.editedMember
            if (member == null) {
                LaunchedEffect(Unit) { viewModel.backToList() }
            } else {
                MemberStep(
                    member = member,
                    onBack = viewModel::backToList,
                    onRoleSelected = { viewModel.changeRole(member, it) },
                    onRemove = { viewModel.remove(member) },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun TeamList(
    uiState: TeamUiState,
    onBack: () -> Unit,
    onInvite: (Role) -> Unit,
    onOpenMember: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    FarmTrackerScaffold(
        title = stringResource(R.string.team_title),
        icon = Icons.Filled.Groups,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.team_invite_section), style = MaterialTheme.typography.titleMedium)
        InviteRoles.forEach { (role, label) ->
            BigActionButton(
                text = stringResource(label),
                icon = RoleUi.icon(role),
                onClick = { onInvite(role) },
                tone = Tone.Go,
                enabled = uiState.invitingRole == null,
            )
        }
        when {
            uiState.invitingRole != null ->
                StatusPill(text = stringResource(R.string.team_inviting), icon = Icons.Filled.HourglassTop)
            uiState.inviteFailed ->
                StatusPill(text = stringResource(R.string.team_invite_failed), icon = Icons.Filled.CloudOff, tone = Tone.Warning)
        }
        Text(stringResource(R.string.team_people_section), style = MaterialTheme.typography.titleMedium)
        uiState.members.forEach { teamMember ->
            MemberCard(teamMember = teamMember, onClick = { onOpenMember(teamMember.member.userId) })
        }
    }
}

/** Osoba na liście: rola (ikona + podpis), imię, numer; cały wiersz otwiera zmianę roli. */
@Composable
private fun MemberCard(teamMember: TeamMember, onClick: () -> Unit) {
    val member = teamMember.member
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(RoleUi.icon(member.role), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (teamMember.isMe) stringResource(R.string.team_me, member.name) else member.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(RoleUi.labelRes(member.role)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = member.phone, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

/** Świeże zaproszenie: duży kod (da się go też podyktować) i wysyłka przez SMS / WhatsApp. */
@Composable
private fun InviteStep(invite: Invite, onShare: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    FarmTrackerScaffold(
        title = stringResource(R.string.team_invite_title),
        icon = Icons.Filled.Key,
        onBack = onDone,
        modifier = modifier,
    ) {
        StatusPill(
            text = stringResource(R.string.team_invite_for, stringResource(RoleUi.labelRes(invite.role))),
            icon = RoleUi.icon(invite.role),
            tone = Tone.Go,
        )
        Text(stringResource(R.string.team_invite_code_label), style = MaterialTheme.typography.titleMedium)
        Text(
            text = invite.code.display,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        StatusPill(text = stringResource(R.string.team_invite_valid), icon = Icons.Filled.Timer)
        BigActionButton(
            text = stringResource(R.string.team_invite_share),
            icon = Icons.Filled.Share,
            onClick = onShare,
            tone = Tone.Go,
        )
        BigActionButton(
            text = stringResource(R.string.team_invite_done),
            icon = Icons.Filled.Check,
            onClick = onDone,
            tone = Tone.Neutral,
        )
    }
}

/** Jedna osoba: rola jednym dotknięciem (od razu zapisana) i usunięcie ze zbioru. */
@Composable
private fun MemberStep(
    member: TeamMember,
    onBack: () -> Unit,
    onRoleSelected: (Role) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FarmTrackerScaffold(
        title = member.member.name,
        icon = RoleUi.icon(member.member.role),
        onBack = onBack,
        modifier = modifier,
    ) {
        Text(member.member.phone, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.team_member_role), style = MaterialTheme.typography.titleMedium)
        RoleOrder.forEach { role ->
            val selected = member.member.role == role
            BigActionButton(
                text = stringResource(RoleUi.labelRes(role)),
                icon = RoleUi.icon(role),
                onClick = { onRoleSelected(role) },
                tone = if (selected) Tone.Primary else Tone.Neutral,
                enabled = member.canChange || selected,
                selected = selected,
            )
        }
        when {
            !member.canChange -> StatusPill(
                text = stringResource(R.string.team_member_last_admin),
                icon = Icons.Filled.Info,
                tone = Tone.Warning,
            )
            member.isMe -> StatusPill(text = stringResource(R.string.team_member_is_you), icon = Icons.Filled.Info)
            else -> BigActionButton(
                text = stringResource(R.string.team_member_remove),
                icon = Icons.Filled.PersonRemove,
                onClick = onRemove,
                tone = Tone.Stop,
            )
        }
    }
}

private fun Context.share(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}

private val PreviewMembers = listOf(
    TeamMember(Member("me", "Tomek", "+48600000001", Role.ADMIN), isMe = true, canChange = false),
    TeamMember(Member("u2", "Marek", "+48600000002", Role.DRIVER), isMe = false, canChange = true),
)

@PreviewLightDark
@Composable
private fun TeamListPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        TeamList(
            uiState = TeamUiState(harvestName = "Kukurydza 2026", members = PreviewMembers),
            onBack = {},
            onInvite = {},
            onOpenMember = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@PreviewLightDark
@Composable
private fun InvitePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        InviteStep(invite = Invite(InviteCode("482913"), "h-1", Role.DRIVER, 0), onShare = {}, onDone = {})
    }
}

@PreviewLightDark
@Composable
private fun MemberPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        MemberStep(member = PreviewMembers[1], onBack = {}, onRoleSelected = {}, onRemove = {})
    }
}
