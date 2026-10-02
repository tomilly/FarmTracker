package pl.farmtracker.feature.auth.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.FarmTrackerScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.auth.R

/** Pierwszy ekran po instalacji: kod zaproszenia (większość ludzi) albo logowanie numerem → kod z SMS-a (admin). */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(
        uiState = uiState,
        onHaveInvite = viewModel::startWithInviteCode,
        onUsePhone = viewModel::usePhoneNumber,
        onBackToStart = viewModel::backToStart,
        onPhoneChanged = viewModel::onPhoneChanged,
        onSendCode = viewModel::sendCode,
        onCodeChanged = viewModel::onCodeChanged,
        onResend = viewModel::resendCode,
        onChangeNumber = viewModel::changeNumber,
        modifier = modifier,
    )
}

@Composable
internal fun LoginContent(
    uiState: LoginUiState,
    onHaveInvite: () -> Unit,
    onUsePhone: () -> Unit,
    onBackToStart: () -> Unit,
    onPhoneChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FarmTrackerScaffold(
        title = stringResource(R.string.auth_title),
        icon = Icons.Filled.Phone,
        modifier = modifier,
        onBack = when (uiState.step) {
            LoginStep.START -> null
            LoginStep.PHONE -> onBackToStart
            LoginStep.CODE -> onChangeNumber
        },
    ) {
        when (uiState.step) {
            LoginStep.START -> StartStep(uiState, onHaveInvite, onUsePhone)
            LoginStep.PHONE -> {
                BackHandler(onBack = onBackToStart)
                PhoneStep(uiState, onPhoneChanged, onSendCode)
            }
            LoginStep.CODE -> {
                BackHandler(onBack = onChangeNumber)
                CodeStep(uiState, onCodeChanged, onResend, onChangeNumber)
            }
        }
    }
}

/** Zaproszeni nie podają numeru ani nie czekają na SMS – tylko kod; numer telefonu zostaje dla admina. */
@Composable
private fun StartStep(uiState: LoginUiState, onHaveInvite: () -> Unit, onUsePhone: () -> Unit) {
    Text(stringResource(R.string.auth_welcome), style = MaterialTheme.typography.bodyLarge)
    BigActionButton(
        text = stringResource(R.string.auth_have_invite),
        icon = Icons.Filled.Key,
        onClick = onHaveInvite,
        tone = Tone.Go,
        enabled = !uiState.busy,
    )
    Problem(uiState.problem)
    if (uiState.busy) StatusPill(text = stringResource(R.string.auth_starting), icon = Icons.Filled.HourglassTop)
    Text(stringResource(R.string.auth_use_phone_note), style = MaterialTheme.typography.bodyLarge)
    BigActionButton(
        text = stringResource(R.string.auth_use_phone),
        icon = Icons.Filled.Phone,
        onClick = onUsePhone,
        tone = Tone.Neutral,
        enabled = !uiState.busy,
    )
}

@Composable
private fun PhoneStep(uiState: LoginUiState, onPhoneChanged: (String) -> Unit, onSendCode: () -> Unit) {
    Text(stringResource(R.string.auth_intro), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.auth_phone_label), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = uiState.phone,
        onValueChange = onPhoneChanged,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
        textStyle = MaterialTheme.typography.titleLarge,
        placeholder = { Text(stringResource(R.string.auth_phone_hint), style = MaterialTheme.typography.titleLarge) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSendCode() }),
    )
    Problem(uiState.problem)
    if (uiState.busy) StatusPill(text = stringResource(R.string.auth_sending), icon = Icons.Filled.HourglassTop)
    BigActionButton(
        text = stringResource(R.string.auth_send_code),
        icon = Icons.AutoMirrored.Filled.Send,
        onClick = onSendCode,
        tone = Tone.Go,
        enabled = uiState.canSend,
    )
}

@Composable
private fun CodeStep(
    uiState: LoginUiState,
    onCodeChanged: (String) -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    StatusPill(
        text = stringResource(R.string.auth_code_sent_to, uiState.sentTo.orEmpty()),
        icon = Icons.Filled.Sms,
        tone = Tone.Go,
    )
    Text(stringResource(R.string.auth_code_label), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = uiState.code,
        onValueChange = onCodeChanged,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget).focusRequester(focus),
        // Duże, rozstrzelone cyfry – łatwo porównać z SMS-em.
        textStyle = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center),
        singleLine = true,
        enabled = !uiState.busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
    )
    Problem(uiState.problem)
    if (uiState.busy) StatusPill(text = stringResource(R.string.auth_checking), icon = Icons.Filled.HourglassTop)
    BigActionButton(
        text = stringResource(R.string.auth_resend),
        icon = Icons.Filled.Refresh,
        onClick = onResend,
        tone = Tone.Neutral,
        enabled = !uiState.busy,
    )
    BigActionButton(
        text = stringResource(R.string.auth_change_number),
        icon = Icons.Filled.Edit,
        onClick = onChangeNumber,
        tone = Tone.Neutral,
    )
}

@Composable
private fun Problem(problem: LoginProblem?) {
    if (problem == null) return
    StatusPill(
        text = stringResource(
            when (problem) {
                LoginProblem.INVALID_NUMBER -> R.string.auth_invalid_number
                LoginProblem.TOO_MANY_ATTEMPTS -> R.string.auth_too_many
                LoginProblem.UNAVAILABLE -> R.string.auth_unavailable
                LoginProblem.SERVICE_DOWN -> R.string.auth_service_down
                LoginProblem.WRONG_CODE -> R.string.auth_wrong_code
                LoginProblem.EXPIRED -> R.string.auth_expired
                LoginProblem.INVITE_START_FAILED -> R.string.auth_invite_start_failed
            },
        ),
        icon = if (problem == LoginProblem.UNAVAILABLE || problem == LoginProblem.INVITE_START_FAILED) {
            Icons.Filled.CloudOff
        } else {
            Icons.Filled.Info
        },
        tone = Tone.Warning,
    )
}

@PreviewLightDark
@Composable
private fun LoginStartPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        LoginContent(
            uiState = LoginUiState(),
            onHaveInvite = {},
            onUsePhone = {},
            onBackToStart = {},
            onPhoneChanged = {},
            onSendCode = {},
            onCodeChanged = {},
            onResend = {},
            onChangeNumber = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginPhonePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        LoginContent(
            uiState = LoginUiState(step = LoginStep.PHONE, phone = "600 123 456"),
            onHaveInvite = {},
            onUsePhone = {},
            onBackToStart = {},
            onPhoneChanged = {},
            onSendCode = {},
            onCodeChanged = {},
            onResend = {},
            onChangeNumber = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginCodePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        LoginContent(
            uiState = LoginUiState(
                step = LoginStep.CODE,
                sentTo = "+48600123456",
                code = "123",
                problem = LoginProblem.WRONG_CODE,
            ),
            onHaveInvite = {},
            onUsePhone = {},
            onBackToStart = {},
            onPhoneChanged = {},
            onSendCode = {},
            onCodeChanged = {},
            onResend = {},
            onChangeNumber = {},
        )
    }
}
