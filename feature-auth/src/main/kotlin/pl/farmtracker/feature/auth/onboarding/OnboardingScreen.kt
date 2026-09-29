package pl.farmtracker.feature.auth.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WavingHand
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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

/** Po pierwszym zalogowaniu: imię, potem kod zaproszenia albo założenie zbioru. */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingContent(
        uiState = uiState,
        actions = OnboardingActions(
            onNameChanged = viewModel::onNameChanged,
            onStartJoining = viewModel::startJoining,
            onStartCreating = viewModel::startCreating,
            onBack = viewModel::back,
            onCodeChanged = viewModel::onCodeChanged,
            onRetryJoin = viewModel::retryJoin,
            onHarvestNameChanged = viewModel::onHarvestNameChanged,
            onCreate = viewModel::createHarvest,
        ),
        modifier = modifier,
    )
}

internal class OnboardingActions(
    val onNameChanged: (String) -> Unit = {},
    val onStartJoining: () -> Unit = {},
    val onStartCreating: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onCodeChanged: (String) -> Unit = {},
    val onRetryJoin: () -> Unit = {},
    val onHarvestNameChanged: (String) -> Unit = {},
    val onCreate: () -> Unit = {},
)

@Composable
internal fun OnboardingContent(uiState: OnboardingUiState, actions: OnboardingActions, modifier: Modifier = Modifier) {
    if (uiState.step != OnboardingStep.CHOOSE) BackHandler(onBack = actions.onBack)
    when (uiState.step) {
        OnboardingStep.CHOOSE -> FarmTrackerScaffold(
            title = stringResource(R.string.onboarding_title),
            icon = Icons.Filled.WavingHand,
            modifier = modifier,
        ) { ChooseStep(uiState, actions) }
        OnboardingStep.JOIN -> FarmTrackerScaffold(
            title = stringResource(R.string.onboarding_join_title),
            icon = Icons.Filled.Key,
            onBack = actions.onBack,
            modifier = modifier,
        ) { JoinStep(uiState, actions) }
        OnboardingStep.CREATE -> FarmTrackerScaffold(
            title = stringResource(R.string.onboarding_create_title),
            icon = Icons.Filled.Grass,
            onBack = actions.onBack,
            modifier = modifier,
        ) { CreateStep(uiState, actions) }
    }
}

@Composable
private fun ChooseStep(uiState: OnboardingUiState, actions: OnboardingActions) {
    val focusManager = LocalFocusManager.current
    Text(stringResource(R.string.onboarding_name_label), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = uiState.name,
        onValueChange = actions.onNameChanged,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
        textStyle = MaterialTheme.typography.titleLarge,
        placeholder = { Text(stringResource(R.string.onboarding_name_hint), style = MaterialTheme.typography.titleLarge) },
        supportingText = { Text(stringResource(R.string.onboarding_name_why), style = MaterialTheme.typography.bodyMedium) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
    // Zaproszenie to droga większości ludzi – dlatego wyżej i na zielono.
    BigActionButton(
        text = stringResource(R.string.onboarding_have_code),
        icon = Icons.Filled.Key,
        onClick = actions.onStartJoining,
        tone = Tone.Go,
        enabled = uiState.canContinue,
    )
    BigActionButton(
        text = stringResource(R.string.onboarding_create),
        icon = Icons.Filled.AddCircle,
        onClick = actions.onStartCreating,
        tone = Tone.Neutral,
        enabled = uiState.canContinue,
    )
}

@Composable
private fun JoinStep(uiState: OnboardingUiState, actions: OnboardingActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Text(stringResource(R.string.onboarding_code_label), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = uiState.code,
        onValueChange = actions.onCodeChanged,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget).focusRequester(focus),
        textStyle = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center),
        singleLine = true,
        enabled = !uiState.busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
    )
    when {
        uiState.busy -> StatusPill(text = stringResource(R.string.onboarding_joining), icon = Icons.Filled.HourglassTop)
        uiState.problem == OnboardingProblem.INVALID_CODE -> StatusPill(
            text = stringResource(R.string.onboarding_invalid_code),
            icon = Icons.Filled.Info,
            tone = Tone.Warning,
        )
        uiState.problem == OnboardingProblem.UNAVAILABLE -> {
            StatusPill(text = stringResource(R.string.onboarding_unavailable), icon = Icons.Filled.CloudOff, tone = Tone.Warning)
            BigActionButton(
                text = stringResource(R.string.onboarding_retry),
                icon = Icons.Filled.Refresh,
                onClick = actions.onRetryJoin,
            )
        }
    }
}

@Composable
private fun CreateStep(uiState: OnboardingUiState, actions: OnboardingActions) {
    Text(stringResource(R.string.onboarding_harvest_label), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = uiState.harvestName,
        onValueChange = actions.onHarvestNameChanged,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
        textStyle = MaterialTheme.typography.titleLarge,
        placeholder = { Text(stringResource(R.string.onboarding_harvest_hint), style = MaterialTheme.typography.titleLarge) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { actions.onCreate() }),
    )
    StatusPill(text = stringResource(R.string.onboarding_admin_note), icon = Icons.Filled.ManageAccounts)
    when {
        uiState.busy -> StatusPill(text = stringResource(R.string.onboarding_creating), icon = Icons.Filled.HourglassTop)
        uiState.problem == OnboardingProblem.UNAVAILABLE ->
            StatusPill(text = stringResource(R.string.onboarding_unavailable), icon = Icons.Filled.CloudOff, tone = Tone.Warning)
    }
    BigActionButton(
        text = stringResource(R.string.onboarding_create_button),
        icon = Icons.Filled.Check,
        onClick = actions.onCreate,
        tone = Tone.Go,
        enabled = uiState.canCreate,
    )
}

@PreviewLightDark
@Composable
private fun OnboardingChoosePreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        OnboardingContent(uiState = OnboardingUiState(name = "Marek"), actions = OnboardingActions())
    }
}

@PreviewLightDark
@Composable
private fun OnboardingJoinPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        OnboardingContent(
            uiState = OnboardingUiState(step = OnboardingStep.JOIN, name = "Marek", problem = OnboardingProblem.INVALID_CODE),
            actions = OnboardingActions(),
        )
    }
}
