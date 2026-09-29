package pl.farmtracker.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.core.map.MapScaffold
import pl.farmtracker.core.ui.FieldColorUi
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.format.formatHectares
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onEditField: (fieldId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val selection by viewModel.parcelSelection.collectAsStateWithLifecycle()
    val chrome by viewModel.chrome.state.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val selectedField by viewModel.selectedField.collectAsStateWithLifecycle()
    val canEditFields by viewModel.canEditFields.collectAsStateWithLifecycle()

    MapScaffold(
        title = stringResource(R.string.map_title),
        icon = Icons.Filled.Map,
        onBack = onBack,
        chrome = viewModel.chrome,
        onMapTap = viewModel::onMapTapped,
        overlays = MapOverlays(
            fields = fields,
            highlight = selectedField?.shape ?: selection.selectedParcel?.shape.orEmpty(),
            entryPoints = fields.flatMap { it.entryPoints },
        ),
        modifier = modifier,
    ) {
        val field = selectedField
        if (field != null) {
            SelectedFieldCard(field = field, onClose = viewModel::clearFieldSelection)
            if (canEditFields) {
                BigActionButton(
                    text = stringResource(R.string.map_edit_field),
                    icon = Icons.Filled.Edit,
                    onClick = { onEditField(field.id) },
                )
            }
        } else {
            ParcelMessage(
                selection = selection,
                parcelsVisible = chrome.parcelsVisible,
                onClearParcel = viewModel::clearParcelSelection,
            )
        }
    }
}

/** Dotknięte pole: kolor, nazwa i powierzchnia – jak karta działki, żeby wyglądało znajomo. */
@Composable
private fun SelectedFieldCard(field: Field, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(FieldColorUi.color(field.color), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = field.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.map_parcel_area, formatHectares(field.areaHectares)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget)) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.map_close), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Jeden komunikat o działkach naraz – ten najważniejszy dla tego, co użytkownik właśnie zrobił. */
@Composable
internal fun ParcelMessage(selection: ParcelSelection, parcelsVisible: Boolean, onClearParcel: () -> Unit) {
    when (selection) {
        is ParcelSelection.Selected -> SelectedParcelCard(parcel = selection.parcel, onClose = onClearParcel)
        ParcelSelection.Searching -> StatusPill(
            text = stringResource(R.string.map_parcel_searching),
            icon = Icons.Filled.Search,
        )
        ParcelSelection.NotFound -> StatusPill(
            text = stringResource(R.string.map_parcel_not_found),
            icon = Icons.Filled.Info,
            tone = Tone.Warning,
        )
        ParcelSelection.Unavailable -> StatusPill(
            text = stringResource(R.string.map_parcel_unavailable),
            icon = Icons.Filled.CloudOff,
            tone = Tone.Warning,
        )
        ParcelSelection.None -> if (parcelsVisible) {
            StatusPill(text = stringResource(R.string.map_tap_parcel_hint), icon = Icons.Filled.TouchApp)
        }
    }
}

@Composable
private fun SelectedParcelCard(parcel: Parcel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
                    text = stringResource(R.string.map_parcel_title, parcel.number),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.map_parcel_area, formatHectares(parcel.areaHectares)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.map_parcel_place, parcel.precinct, parcel.commune),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget)) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.map_close), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SelectedParcelPreview() {
    val parcel = Parcel(
        id = "302103_5.0007.125",
        number = "125",
        precinct = "Otusz",
        commune = "Buk",
        shape = listOf(
            GeoPolygon(listOf(GeoPoint(52.355, 16.578), GeoPoint(52.354, 16.581), GeoPoint(52.352, 16.585))),
        ),
    )
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp)) {
                ParcelMessage(selection = ParcelSelection.Selected(parcel), parcelsVisible = true, onClearParcel = {})
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ParcelHintPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp)) {
                ParcelMessage(selection = ParcelSelection.None, parcelsVisible = true, onClearParcel = {})
            }
        }
    }
}
