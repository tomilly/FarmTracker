package pl.farmtracker.feature.fields.base

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.core.map.MapScaffold
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.fields.R
import pl.farmtracker.feature.fields.common.SearchStep

/** Gdzie jest baza (silos / pryzma) – tam kierowcy wiozą kukurydzę. */
@Composable
fun BaseEditorScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BaseEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.done) { if (uiState.done) onDone() }
    // Zapisana baza jeszcze się wczytuje – bez tego mapa zdążyłaby pokazać „mnie" zamiast bazy.
    if (uiState.loading) return

    if (uiState.searchOpen) {
        BackHandler(onBack = viewModel::closeSearch)
        SearchStep(
            query = uiState.searchQuery,
            search = uiState.search,
            label = stringResource(R.string.fields_base_search_label),
            hint = stringResource(R.string.fields_base_search_hint),
            pickPlaceLabel = stringResource(R.string.fields_base_search_pick_place),
            onBack = viewModel::closeSearch,
            onQueryChanged = viewModel::onSearchQueryChanged,
            onSearch = viewModel::runSearch,
            onPickPlace = viewModel::pickPlace,
            modifier = modifier,
        )
        return
    }

    MapScaffold(
        title = stringResource(R.string.fields_base_title),
        icon = Icons.Filled.Warehouse,
        onBack = onBack,
        chrome = viewModel.chrome,
        onMapTap = viewModel::onMapTapped,
        overlays = MapOverlays(
            fields = fields,
            entryPoints = fields.flatMap { it.entryPoints },
            base = uiState.location,
        ),
        modifier = modifier,
    ) {
        BasePanel(
            uiState = uiState,
            onSearch = viewModel::openSearch,
            onRemove = viewModel::removeBase,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun BasePanel(uiState: BaseEditorUiState, onSearch: () -> Unit, onRemove: () -> Unit, onSave: () -> Unit) {
    if (uiState.location == null) {
        StatusPill(text = stringResource(R.string.fields_base_hint), icon = Icons.Filled.TouchApp)
    } else {
        StatusPill(text = stringResource(R.string.fields_base_set), icon = Icons.Filled.CheckCircle, tone = Tone.Go)
    }
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigActionButton(
            text = stringResource(R.string.fields_search_by_number),
            icon = Icons.Filled.Search,
            onClick = onSearch,
            tone = Tone.Neutral,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        BigActionButton(
            text = stringResource(R.string.fields_base_remove),
            icon = Icons.Filled.Delete,
            onClick = onRemove,
            tone = Tone.Neutral,
            enabled = uiState.location != null,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
    BigActionButton(
        text = stringResource(R.string.fields_base_save),
        icon = Icons.Filled.Check,
        onClick = onSave,
        tone = Tone.Go,
        enabled = uiState.canSave,
    )
}

@PreviewLightDark
@Composable
private fun BasePanelPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasePanel(
                    uiState = BaseEditorUiState(loading = false, location = GeoPoint(51.95, 18.62)),
                    onSearch = {},
                    onRemove = {},
                    onSave = {},
                )
            }
        }
    }
}
