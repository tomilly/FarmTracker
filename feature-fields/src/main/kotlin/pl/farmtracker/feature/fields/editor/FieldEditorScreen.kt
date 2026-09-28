package pl.farmtracker.feature.fields.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.core.map.MapScaffold
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.FarmTrackerScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.format.formatHectares
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.fields.R
import pl.farmtracker.feature.fields.common.ColorPicker

@Composable
fun FieldEditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FieldEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val existingFields by viewModel.existingFields.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }

    when (uiState.step) {
        EditorStep.SHAPE -> MapScaffold(
            title = stringResource(R.string.fields_new_title),
            icon = Icons.Filled.AddLocationAlt,
            onBack = onBack,
            chrome = viewModel.chrome,
            onMapTap = viewModel::onMapTapped,
            overlays = MapOverlays(fields = existingFields, highlight = uiState.parcels.flatMap { it.shape }),
            modifier = modifier,
            // Przy wyznaczaniu pola granice działek są zawsze potrzebne – przycisk tylko zabierałby miejsce mapie.
            showParcelsToggle = false,
        ) {
            ShapePanel(
                uiState = uiState,
                onUndo = viewModel::removeLastParcel,
                onSearch = viewModel::openSearch,
                onNext = viewModel::goToDetails,
            )
        }
        EditorStep.SEARCH -> {
            BackHandler(onBack = viewModel::closeSearch)
            FieldSearchStep(
                uiState = uiState,
                onBack = viewModel::closeSearch,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onSearch = viewModel::runSearch,
                onPick = viewModel::pickSearchResult,
                modifier = modifier,
            )
        }
        EditorStep.DETAILS -> {
            // „Wstecz" z formularza wraca do mapy z wybranymi działkami, a nie wyrzuca z tworzenia pola.
            BackHandler(onBack = viewModel::backToShape)
            FieldDetailsStep(
                uiState = uiState,
                onBack = viewModel::backToShape,
                onNameChanged = viewModel::onNameChanged,
                onColorSelected = viewModel::onColorSelected,
                onSave = viewModel::save,
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun ShapePanel(uiState: FieldEditorUiState, onUndo: () -> Unit, onSearch: () -> Unit, onNext: () -> Unit) {
    when {
        uiState.pendingLookups > 0 -> StatusPill(
            text = stringResource(R.string.fields_searching),
            icon = Icons.Filled.Search,
        )
        uiState.lastProblem == LookupProblem.NOT_FOUND -> StatusPill(
            text = stringResource(R.string.fields_not_found),
            icon = Icons.Filled.Info,
            tone = Tone.Warning,
        )
        uiState.lastProblem == LookupProblem.UNAVAILABLE -> StatusPill(
            text = stringResource(R.string.fields_unavailable),
            icon = Icons.Filled.CloudOff,
            tone = Tone.Warning,
        )
        uiState.parcels.isEmpty() -> StatusPill(
            text = stringResource(R.string.fields_tap_parcels_hint),
            icon = Icons.Filled.TouchApp,
        )
    }
    if (uiState.parcels.isEmpty()) {
        BigActionButton(
            text = stringResource(R.string.fields_search_by_number),
            icon = Icons.Filled.Search,
            onClick = onSearch,
            tone = Tone.Neutral,
        )
    } else {
        SelectionSummary(uiState = uiState, onUndo = onUndo)
        // Obok „Dalej" nadal można dołożyć działkę po numerze.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigActionButton(
                text = stringResource(R.string.fields_search_short),
                icon = Icons.Filled.Search,
                onClick = onSearch,
                tone = Tone.Neutral,
                modifier = Modifier.weight(1f),
            )
            BigActionButton(
                text = stringResource(R.string.fields_next),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onNext,
                tone = Tone.Go,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** „2 działki · ok. 12,40 ha" + „Cofnij" ostatnio dodanej. */
@Composable
private fun SelectionSummary(uiState: FieldEditorUiState, onUndo: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Grass, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.fields_selected_parcels,
                        uiState.parcels.size,
                        uiState.parcels.size,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.fields_area, formatHectares(uiState.areaHectares)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.fields_undo), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
internal fun FieldDetailsStep(
    uiState: FieldEditorUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onColorSelected: (FieldColor) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    FarmTrackerScaffold(
        title = stringResource(R.string.fields_new_title),
        icon = Icons.Filled.AddLocationAlt,
        onBack = onBack,
        modifier = modifier,
    ) {
        StatusPill(
            text = pluralStringResource(R.plurals.fields_selected_parcels, uiState.parcels.size, uiState.parcels.size) +
                " · " + stringResource(R.string.fields_area, formatHectares(uiState.areaHectares)),
            icon = Icons.Filled.Grass,
        )
        Text(stringResource(R.string.fields_name_label), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
            textStyle = MaterialTheme.typography.titleMedium,
            placeholder = { Text(stringResource(R.string.fields_name_hint), style = MaterialTheme.typography.titleMedium) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        Text(stringResource(R.string.fields_color_label), style = MaterialTheme.typography.titleMedium)
        ColorPicker(selected = uiState.color, onSelect = onColorSelected)
        BigActionButton(
            text = stringResource(R.string.fields_save),
            icon = Icons.Filled.Check,
            onClick = onSave,
            tone = Tone.Go,
            enabled = uiState.canSave,
        )
    }
}

private val PreviewParcel = Parcel(
    id = "021501_2.0003.2285",
    number = "2285",
    precinct = "Bystrzyca",
    commune = "Oława",
    shape = listOf(
        GeoPolygon(listOf(GeoPoint(50.95, 17.35), GeoPoint(50.95, 17.36), GeoPoint(50.96, 17.36), GeoPoint(50.96, 17.35))),
    ),
)

@PreviewLightDark
@Composable
private fun ShapePanelPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp)) {
                ShapePanel(
                    uiState = FieldEditorUiState(parcels = listOf(PreviewParcel)),
                    onUndo = {},
                    onSearch = {},
                    onNext = {},
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FieldDetailsPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        FieldDetailsStep(
            uiState = FieldEditorUiState(
                step = EditorStep.DETAILS,
                parcels = listOf(PreviewParcel),
                name = "Bystrzyca 2285",
                color = FieldColor.ORANGE,
            ),
            onBack = {},
            onNameChanged = {},
            onColorSelected = {},
            onSave = {},
        )
    }
}
