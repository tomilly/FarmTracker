package pl.farmtracker.feature.fields.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.GridOn
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
import pl.farmtracker.core.ui.format.pluralStringPl
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.fields.R
import pl.farmtracker.feature.fields.common.ColorPicker

@Composable
fun FieldEditorScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FieldEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val existingFields by viewModel.existingFields.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.done) { if (uiState.done) onDone() }
    // Edytowane pole jeszcze się wczytuje – pusty ekran zamiast mignięcia mapy tworzenia pola.
    if (uiState.loadingField) return

    when (uiState.step) {
        EditorStep.SHAPE -> MapScaffold(
            title = stringResource(R.string.fields_new_title),
            icon = Icons.Filled.AddLocationAlt,
            onBack = onBack,
            chrome = viewModel.chrome,
            onMapTap = viewModel::onMapTapped,
            overlays = MapOverlays(
                fields = existingFields,
                highlight = uiState.parcels.flatMap { it.shape },
                draft = uiState.drawnPoints,
            ),
            modifier = modifier,
            // Przy wyznaczaniu pola granice działek są zawsze potrzebne – przycisk tylko zabierałby miejsce mapie.
            showParcelsToggle = false,
        ) {
            ShapePanel(
                uiState = uiState,
                onUndo = viewModel::undoLast,
                onSearch = viewModel::openSearch,
                onDraw = viewModel::startDrawing,
                onPickParcels = viewModel::stopDrawing,
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
                onPickPlace = viewModel::pickPlace,
                modifier = modifier,
            )
        }
        EditorStep.DETAILS -> {
            // „Wstecz" z formularza wraca do mapy z wybranym kształtem, a nie wyrzuca z tworzenia pola.
            // W edycji „Wstecz" zamyka ekran; przy tworzeniu wraca do mapy z wybranym kształtem.
            val back = if (uiState.isEditing) onBack else viewModel::backToShape
            BackHandler(onBack = back)
            FieldDetailsStep(
                uiState = uiState,
                onBack = back,
                onNameChanged = viewModel::onNameChanged,
                onColorSelected = viewModel::onColorSelected,
                onMarkEntry = viewModel::openEntry,
                onRemoveEntry = viewModel::clearEntry,
                onSave = viewModel::save,
                onDelete = viewModel::delete,
                modifier = modifier,
            )
        }
        EditorStep.ENTRY -> {
            BackHandler(onBack = viewModel::closeEntry)
            MapScaffold(
                title = stringResource(R.string.fields_entry_title),
                icon = Icons.Filled.Fence,
                onBack = viewModel::closeEntry,
                chrome = viewModel.chrome,
                onMapTap = viewModel::onMapTapped,
                overlays = MapOverlays(
                    fields = existingFields,
                    highlight = uiState.fieldShape,
                    entryPoints = listOfNotNull(uiState.entryPoint),
                ),
                modifier = modifier,
                showParcelsToggle = false,
            ) {
                EntryPanel(uiState = uiState, onDone = viewModel::closeEntry)
            }
        }
    }
}

@Composable
internal fun ShapePanel(
    uiState: FieldEditorUiState,
    onUndo: () -> Unit,
    onSearch: () -> Unit,
    onDraw: () -> Unit,
    onPickParcels: () -> Unit,
    onNext: () -> Unit,
) {
    when (uiState.shapeMode) {
        ShapeMode.PARCELS -> ParcelsPanel(uiState, onUndo, onSearch, onDraw, onNext)
        ShapeMode.DRAW -> DrawPanel(uiState, onUndo, onPickParcels, onNext)
    }
}

@Composable
private fun ParcelsPanel(
    uiState: FieldEditorUiState,
    onUndo: () -> Unit,
    onSearch: () -> Unit,
    onDraw: () -> Unit,
    onNext: () -> Unit,
) {
    MessageSlot {
        if (uiState.parcels.isEmpty()) {
            when {
                uiState.pendingLookups > 0 -> StatusPill(stringResource(R.string.fields_searching), Icons.Filled.Search)
                uiState.lastProblem != null -> ProblemPill(uiState.lastProblem, uiState.problemFieldName)
                else -> StatusPill(stringResource(R.string.fields_tap_parcels_hint), Icons.Filled.TouchApp)
            }
        } else {
            ShapeSummary(
                title = parcelsCount(uiState.parcels.size),
                detail = when {
                    uiState.pendingLookups > 0 -> SummaryDetail.Note(stringResource(R.string.fields_searching))
                    uiState.lastProblem != null ->
                        SummaryDetail.Warning(problemText(uiState.lastProblem, uiState.problemFieldName))
                    else -> SummaryDetail.Area(uiState.areaHectares)
                },
                onUndo = onUndo,
            )
        }
    }
    if (uiState.parcels.isEmpty()) {
        // Dwie pozostałe drogi do kształtu pola: po numerze albo narysowanie, gdy pole ≠ działki.
        ButtonPair {
            BigActionButton(
                text = stringResource(R.string.fields_search_by_number),
                icon = Icons.Filled.Search,
                onClick = onSearch,
                tone = Tone.Neutral,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            BigActionButton(
                text = stringResource(R.string.fields_draw),
                icon = Icons.Filled.Draw,
                onClick = onDraw,
                tone = Tone.Neutral,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    } else {
        // Obok „Dalej" nadal można dołożyć działkę po numerze.
        ButtonPair {
            BigActionButton(
                text = stringResource(R.string.fields_search_by_number),
                icon = Icons.Filled.Search,
                onClick = onSearch,
                tone = Tone.Neutral,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            NextButton(onClick = onNext, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun DrawPanel(uiState: FieldEditorUiState, onUndo: () -> Unit, onPickParcels: () -> Unit, onNext: () -> Unit) {
    val corners = uiState.drawnPoints.size
    MessageSlot {
        if (corners == 0) {
            StatusPill(stringResource(R.string.fields_draw_hint), Icons.Filled.TouchApp)
        } else {
            ShapeSummary(
                title = cornersCount(corners),
                detail = if (uiState.canContinue) {
                    SummaryDetail.Area(uiState.areaHectares)
                } else {
                    SummaryDetail.Note(stringResource(R.string.fields_draw_more))
                },
                onUndo = onUndo,
            )
        }
    }
    ButtonPair {
        BigActionButton(
            text = stringResource(R.string.fields_pick_parcels),
            icon = Icons.Filled.GridOn,
            onClick = onPickParcels,
            tone = Tone.Neutral,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        NextButton(onClick = onNext, enabled = uiState.canContinue, modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

/**
 * Jedno miejsce na komunikat o stałej minimalnej wysokości. Gdy komunikaty pojawiały się i znikały,
 * przyciski pod nimi skakały pod palcem – przy rysowaniu rogów to przeszkadza.
 */
@Composable
private fun MessageSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = MessageSlotMinHeight),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

/** Dwa przyciski obok siebie, zawsze równej wysokości (nawet gdy jeden podpis ma dwie linie). */
@Composable
private fun ButtonPair(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

private val MessageSlotMinHeight = 80.dp

@Composable
private fun ProblemPill(problem: LookupProblem, fieldName: String?) {
    StatusPill(
        text = problemText(problem, fieldName),
        icon = if (problem == LookupProblem.UNAVAILABLE) Icons.Filled.CloudOff else Icons.Filled.Info,
        tone = Tone.Warning,
    )
}

@Composable
private fun problemText(problem: LookupProblem, fieldName: String?): String = when (problem) {
    LookupProblem.NOT_FOUND -> stringResource(R.string.fields_not_found)
    LookupProblem.UNAVAILABLE -> stringResource(R.string.fields_unavailable)
    LookupProblem.ALREADY_USED -> stringResource(R.string.fields_parcel_in_other_field, fieldName.orEmpty())
}

@Composable
private fun NextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    BigActionButton(
        text = stringResource(R.string.fields_next),
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        onClick = onClick,
        tone = Tone.Go,
        enabled = enabled,
        modifier = modifier,
    )
}

/** Druga linia podsumowania: powierzchnia, uwaga albo ostrzeżenie. */
private sealed interface SummaryDetail {
    data class Area(val hectares: Double) : SummaryDetail
    data class Note(val text: String) : SummaryDetail
    data class Warning(val text: String) : SummaryDetail
}

/** „2 działki / ok. 12,40 ha" albo „4 rogi / ok. 3,10 ha" + „Cofnij" ostatniego kroku. */
@Composable
private fun ShapeSummary(title: String, detail: SummaryDetail, onUndo: () -> Unit) {
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
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                when (detail) {
                    is SummaryDetail.Area -> Text(
                        text = stringResource(R.string.fields_area, formatHectares(detail.hectares)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    is SummaryDetail.Note -> Text(text = detail.text, style = MaterialTheme.typography.bodyMedium)
                    is SummaryDetail.Warning -> Text(
                        text = detail.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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
private fun EntryPanel(uiState: FieldEditorUiState, onDone: () -> Unit) {
    if (uiState.entryPoint == null) {
        StatusPill(text = stringResource(R.string.fields_entry_hint), icon = Icons.Filled.TouchApp)
    } else {
        StatusPill(text = stringResource(R.string.fields_entry_set), icon = Icons.Filled.CheckCircle, tone = Tone.Go)
    }
    BigActionButton(
        text = stringResource(R.string.fields_entry_done),
        icon = Icons.Filled.Check,
        onClick = onDone,
        tone = Tone.Go,
    )
}

@Composable
internal fun FieldDetailsStep(
    uiState: FieldEditorUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onColorSelected: (FieldColor) -> Unit,
    onMarkEntry: () -> Unit,
    onRemoveEntry: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    FarmTrackerScaffold(
        title = stringResource(if (uiState.isEditing) R.string.fields_edit_title else R.string.fields_new_title),
        icon = Icons.Filled.AddLocationAlt,
        onBack = onBack,
        modifier = modifier,
        // „Zapisz" zawsze widoczny na dole, nawet gdy lista kolorów się przewija.
        bottomAction = {
            BigActionButton(
                text = stringResource(R.string.fields_save),
                icon = Icons.Filled.Check,
                onClick = onSave,
                tone = Tone.Go,
                enabled = uiState.canSave,
            )
        },
    ) {
        StatusPill(
            text = shapeSummaryText(uiState) + " · " +
                stringResource(R.string.fields_area, formatHectares(uiState.areaHectares)),
            icon = Icons.Filled.Grass,
        )
        Text(stringResource(R.string.fields_name_label), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
            textStyle = MaterialTheme.typography.titleMedium,
            placeholder = { Text(stringResource(R.string.fields_name_hint), style = MaterialTheme.typography.titleMedium) },
            supportingText = if (uiState.name.isBlank()) {
                { Text(stringResource(R.string.fields_name_required), style = MaterialTheme.typography.bodyMedium) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )

        Text(stringResource(R.string.fields_entry_section), style = MaterialTheme.typography.titleMedium)
        if (uiState.entryPoint != null) {
            StatusPill(text = stringResource(R.string.fields_entry_set), icon = Icons.Filled.CheckCircle, tone = Tone.Go)
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigActionButton(
                text = stringResource(
                    if (uiState.entryPoint == null) R.string.fields_entry_mark else R.string.fields_entry_change,
                ),
                icon = Icons.Filled.Fence,
                onClick = onMarkEntry,
                tone = Tone.Neutral,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            if (uiState.entryPoint != null) {
                BigActionButton(
                    text = stringResource(R.string.fields_entry_remove),
                    icon = Icons.Filled.Delete,
                    onClick = onRemoveEntry,
                    tone = Tone.Neutral,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        Text(stringResource(R.string.fields_color_label), style = MaterialTheme.typography.titleMedium)
        ColorPicker(selected = uiState.color, onSelect = onColorSelected)

        if (uiState.isEditing) {
            BigActionButton(
                text = stringResource(R.string.fields_delete),
                icon = Icons.Filled.Delete,
                onClick = onDelete,
                tone = Tone.Stop,
            )
        }
    }
}

@Composable
private fun shapeSummaryText(uiState: FieldEditorUiState): String {
    val editing = uiState.editing
    return when {
        editing != null && editing.parcelIds.isNotEmpty() -> parcelsCount(editing.parcelIds.size)
        editing != null -> cornersCount(editing.shape.firstOrNull()?.outer?.size ?: 0)
        uiState.shapeMode == ShapeMode.PARCELS -> parcelsCount(uiState.parcels.size)
        else -> cornersCount(uiState.drawnPoints.size)
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
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShapePanel(
                    uiState = FieldEditorUiState(parcels = listOf(PreviewParcel)),
                    onUndo = {},
                    onSearch = {},
                    onDraw = {},
                    onPickParcels = {},
                    onNext = {},
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DrawPanelPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShapePanel(
                    uiState = FieldEditorUiState(
                        shapeMode = ShapeMode.DRAW,
                        drawnPoints = listOf(GeoPoint(50.95, 17.35), GeoPoint(50.95, 17.36), GeoPoint(50.96, 17.36)),
                    ),
                    onUndo = {},
                    onSearch = {},
                    onDraw = {},
                    onPickParcels = {},
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
                entryPoint = GeoPoint(50.95, 17.355),
            ),
            onBack = {},
            onNameChanged = {},
            onColorSelected = {},
            onMarkEntry = {},
            onRemoveEntry = {},
            onSave = {},
        )
    }
}

@Composable
private fun parcelsCount(count: Int): String = pluralStringPl(
    R.string.fields_parcels_one,
    R.string.fields_parcels_few,
    R.string.fields_parcels_many,
    count,
)

@Composable
private fun cornersCount(count: Int): String = pluralStringPl(
    R.string.fields_corners_one,
    R.string.fields_corners_few,
    R.string.fields_corners_many,
    count,
)
