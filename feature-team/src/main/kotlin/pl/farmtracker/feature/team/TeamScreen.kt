package pl.farmtracker.feature.team

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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

/** Admin → „Ludzie": zaproszenia konkretnych osób, lista osób, rola i usuwanie. */
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
            onInvite = viewModel::startInvite,
            onOpenInvite = viewModel::openInvite,
            onOpenMember = viewModel::openMember,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        )
        is TeamStep.NewInvite -> {
            BackHandler(onBack = viewModel::backToList)
            NewInviteStep(
                role = step.role,
                uiState = uiState,
                onNameChanged = viewModel::onInviteNameChanged,
                onPhoneChanged = viewModel::onInvitePhoneChanged,
                onCreate = viewModel::createInvite,
                onBack = viewModel::backToList,
                modifier = modifier,
            )
        }
        is TeamStep.ShowInvite -> {
            BackHandler(onBack = viewModel::backToList)
            val invite = step.invite
            val shareText = stringResource(
                R.string.team_invite_share_text,
                invite.name,
                uiState.harvestName,
                stringResource(RoleUi.labelRes(invite.role)),
                invite.code.display,
            )
            InviteStep(
                invite = invite,
                onSendSms = { context.sendSms(invite.phone, shareText) },
                onShare = { context.share(shareText) },
                onCancel = { viewModel.cancelInvite(invite) },
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
    onOpenInvite: (Invite) -> Unit,
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
            )
        }
        if (uiState.invites.isNotEmpty()) {
            Text(stringResource(R.string.team_invites_section), style = MaterialTheme.typography.titleMedium)
            uiState.invites.forEach { invite ->
                PersonCard(
                    role = invite.role,
                    name = invite.name,
                    details = listOf(stringResource(R.string.team_invite_pending, invite.code.display), invite.phone),
                    onClick = { onOpenInvite(invite) },
                )
            }
        }
        Text(stringResource(R.string.team_people_section), style = MaterialTheme.typography.titleMedium)
        uiState.members.forEach { teamMember ->
            val member = teamMember.member
            PersonCard(
                role = member.role,
                name = if (teamMember.isMe) stringResource(R.string.team_me, member.name) else member.name,
                details = listOf(member.phone),
                onClick = { onOpenMember(member.userId) },
            )
        }
    }
}

/** Osoba na liście: rola (ikona + podpis), imię, numer / kod; cały wiersz otwiera szczegóły. */
@Composable
private fun PersonCard(role: Role, name: String, details: List<String>, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(RoleUi.icon(role), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(RoleUi.labelRes(role)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                details.filter { it.isNotBlank() }.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

/** Kogo zapraszam: imię (wymagane) i numer – na niego pójdzie SMS z kodem; bez numeru kod się podyktuje. */
@Composable
private fun NewInviteStep(
    role: Role,
    uiState: TeamUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    val draft = uiState.draft

    FarmTrackerScaffold(
        title = stringResource(InviteRoles.first { it.first == role }.second),
        icon = RoleUi.icon(role),
        onBack = onBack,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.team_invite_name_label), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget).focusRequester(focus),
            textStyle = MaterialTheme.typography.titleLarge,
            placeholder = { Text(stringResource(R.string.team_invite_name_hint), style = MaterialTheme.typography.titleLarge) },
            supportingText = { Text(stringResource(R.string.team_invite_name_why), style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        Text(stringResource(R.string.team_invite_phone_label), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = draft.phone,
            onValueChange = onPhoneChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
            textStyle = MaterialTheme.typography.titleLarge,
            placeholder = { Text(stringResource(R.string.team_invite_phone_hint), style = MaterialTheme.typography.titleLarge) },
            supportingText = { Text(stringResource(R.string.team_invite_phone_why), style = MaterialTheme.typography.bodyMedium) },
            isError = draft.invalidPhone,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        when {
            draft.invalidPhone -> StatusPill(
                text = stringResource(R.string.team_invite_invalid_phone),
                icon = Icons.Filled.Info,
                tone = Tone.Warning,
            )
            uiState.inviting -> StatusPill(text = stringResource(R.string.team_inviting), icon = Icons.Filled.HourglassTop)
            uiState.inviteFailed -> StatusPill(
                text = stringResource(R.string.team_invite_failed),
                icon = Icons.Filled.CloudOff,
                tone = Tone.Warning,
            )
        }
        BigActionButton(
            text = stringResource(R.string.team_invite_create),
            icon = Icons.Filled.Key,
            onClick = onCreate,
            tone = Tone.Go,
            enabled = draft.canCreate && !uiState.inviting,
        )
    }
}

/** Gotowe zaproszenie: duży kod (da się go też podyktować), SMS na podany numer albo inna wysyłka. */
@Composable
private fun InviteStep(
    invite: Invite,
    onSendSms: () -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FarmTrackerScaffold(
        title = stringResource(R.string.team_invite_title),
        icon = Icons.Filled.Key,
        onBack = onDone,
        modifier = modifier,
    ) {
        StatusPill(
            text = stringResource(R.string.team_invite_for, invite.name, stringResource(RoleUi.labelRes(invite.role))),
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
        if (invite.phone.isNotBlank()) {
            BigActionButton(
                text = stringResource(R.string.team_invite_send_sms, invite.phone),
                icon = Icons.Filled.Sms,
                onClick = onSendSms,
                tone = Tone.Go,
            )
            BigActionButton(
                text = stringResource(R.string.team_invite_share_other),
                icon = Icons.Filled.Share,
                onClick = onShare,
                tone = Tone.Neutral,
            )
        } else {
            BigActionButton(
                text = stringResource(R.string.team_invite_share),
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = onShare,
                tone = Tone.Go,
            )
        }
        BigActionButton(
            text = stringResource(R.string.team_invite_done),
            icon = Icons.Filled.Check,
            onClick = onDone,
            tone = Tone.Neutral,
        )
        BigActionButton(
            text = stringResource(R.string.team_invite_cancel),
            icon = Icons.Filled.Cancel,
            onClick = onCancel,
            tone = Tone.Stop,
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
        if (member.member.phone.isNotBlank()) Text(member.member.phone, style = MaterialTheme.typography.titleMedium)
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

/** SMS na numer z zaproszenia, z gotowym tekstem; bez aplikacji do SMS-ów (tablet) – zwykłe udostępnianie. */
private fun Context.sendSms(phone: String, text: String) {
    val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).putExtra("sms_body", text)
    try {
        startActivity(sms)
    } catch (error: ActivityNotFoundException) {
        share(text)
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

private val PreviewInvite = Invite(InviteCode("482913"), "h-1", Role.DRIVER, 0, name = "Janek", phone = "+48600000003")

@PreviewLightDark
@Composable
private fun TeamListPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        TeamList(
            uiState = TeamUiState(harvestName = "Kukurydza 2026", members = PreviewMembers, invites = listOf(PreviewInvite)),
            onBack = {},
            onInvite = {},
            onOpenInvite = {},
            onOpenMember = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@PreviewLightDark
@Composable
private fun NewInvitePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        NewInviteStep(
            role = Role.DRIVER,
            uiState = TeamUiState(draft = InviteDraft(name = "Janek", phone = "600 000 003")),
            onNameChanged = {},
            onPhoneChanged = {},
            onCreate = {},
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun InvitePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        InviteStep(invite = PreviewInvite, onSendSms = {}, onShare = {}, onCancel = {}, onDone = {})
    }
}

@PreviewLightDark
@Composable
private fun MemberPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        MemberStep(member = PreviewMembers[1], onBack = {}, onRoleSelected = {}, onRemove = {})
    }
}
