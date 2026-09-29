package pl.farmtracker.feature.fields.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.core.map.MapScaffold
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.format.formatHectares
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.fields.R
import pl.farmtracker.feature.fields.common.ColorDot
import pl.farmtracker.feature.fields.common.entriesCount

/** Pole na mapie; stąd wchodzi się w edycję, widząc pole przed sobą. Dotknięcie innego pola przełącza na nie. */
@Composable
fun FieldScreen(
    onBack: () -> Unit,
    onEdit: (fieldId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FieldViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val base by viewModel.base.collectAsStateWithLifecycle()

    when (val state = uiState) {
        FieldViewUiState.Loading -> Unit
        FieldViewUiState.Gone -> LaunchedEffect(Unit) { onBack() }
        is FieldViewUiState.Shown -> MapScaffold(
            title = state.field.name,
            icon = Icons.Filled.Grass,
            onBack = onBack,
            chrome = viewModel.chrome,
            onMapTap = viewModel::onMapTapped,
            overlays = MapOverlays(
                fields = state.allFields,
                // Bieżące pole wyróżnione – po przełączeniu dotykiem widać, które jest teraz pokazane.
                highlight = state.field.shape,
                entryPoints = state.allFields.flatMap { it.entryPoints },
                base = base,
            ),
            modifier = modifier,
        ) {
            FieldPanel(field = state.field, onEdit = { onEdit(state.field.id) })
        }
    }
}

@Composable
private fun FieldPanel(field: Field, onEdit: () -> Unit) {
    FieldSummary(field)
    BigActionButton(
        text = stringResource(R.string.fields_edit_field),
        icon = Icons.Filled.Edit,
        onClick = onEdit,
    )
}

/** Kolor, powierzchnia i czy jest wjazd – to, co warto sprawdzić, zanim się coś zmieni. */
@Composable
private fun FieldSummary(field: Field) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = field.color, size = 32.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.fields_area, formatHectares(field.areaHectares)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (field.entryPoints.isEmpty()) stringResource(R.string.fields_entry_missing) else entriesCount(field.entryPoints.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FieldPanelPreview() {
    val square = GeoPolygon(
        listOf(GeoPoint(50.0, 17.0), GeoPoint(50.0, 17.003), GeoPoint(50.002, 17.003), GeoPoint(50.002, 17.0)),
    )
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp)) {
                FieldPanel(
                    field = Field(id = "1", name = "Za lasem", color = FieldColor.ORANGE, shape = listOf(square)),
                    onEdit = {},
                )
            }
        }
    }
}
