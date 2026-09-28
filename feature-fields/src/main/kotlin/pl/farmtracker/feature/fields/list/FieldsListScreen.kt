package pl.farmtracker.feature.fields.list

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.FarmTrackerScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.fields.R
import pl.farmtracker.feature.fields.common.FieldCard

@Composable
fun FieldsListScreen(
    onBack: () -> Unit,
    onAddField: () -> Unit,
    onEditField: (fieldId: String) -> Unit,
    onShowMap: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FieldsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentlyDeleted by viewModel.recentlyDeleted.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Usunięcie bez pytania „czy na pewno?" – zamiast tego „Cofnij" przez dłuższą chwilę.
    val deletedField = recentlyDeleted
    if (deletedField != null) {
        val message = stringResource(R.string.fields_deleted, deletedField.name)
        val undo = stringResource(R.string.fields_undo)
        LaunchedEffect(deletedField) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undo,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.dismissDeleted()
        }
    }

    FieldsListContent(
        uiState = uiState,
        onBack = onBack,
        onAddField = onAddField,
        onEditField = onEditField,
        onShowMap = onShowMap,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
internal fun FieldsListContent(
    uiState: FieldsListUiState,
    onBack: () -> Unit,
    onAddField: () -> Unit,
    onEditField: (fieldId: String) -> Unit,
    onShowMap: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    FarmTrackerScaffold(
        title = stringResource(R.string.fields_title),
        icon = Icons.Filled.Grass,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        BigActionButton(
            text = stringResource(R.string.fields_add),
            icon = Icons.Filled.Add,
            onClick = onAddField,
            tone = Tone.Go,
        )
        if (uiState is FieldsListUiState.Ready) {
            if (uiState.fields.isEmpty()) {
                StatusPill(text = stringResource(R.string.fields_empty), icon = Icons.Filled.Info)
            } else {
                uiState.fields.forEach { field -> FieldCard(field, onClick = { onEditField(field.id) }) }
                BigActionButton(
                    text = stringResource(R.string.fields_show_on_map),
                    icon = Icons.Filled.Map,
                    onClick = onShowMap,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FieldsListPreview() {
    val square = GeoPolygon(
        listOf(GeoPoint(50.0, 17.0), GeoPoint(50.0, 17.003), GeoPoint(50.002, 17.003), GeoPoint(50.002, 17.0)),
    )
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        FieldsListContent(
            uiState = FieldsListUiState.Ready(
                listOf(
                    Field(id = "1", name = "Za lasem", color = FieldColor.ORANGE, shape = listOf(square)),
                    Field(id = "2", name = "Przy drodze", color = FieldColor.BLUE, shape = listOf(square)),
                ),
            ),
            onBack = {},
            onAddField = {},
            onEditField = {},
            onShowMap = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun FieldsListEmptyPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        FieldsListContent(
            uiState = FieldsListUiState.Ready(emptyList()),
            onBack = {},
            onAddField = {},
            onEditField = {},
            onShowMap = {},
        )
    }
}
