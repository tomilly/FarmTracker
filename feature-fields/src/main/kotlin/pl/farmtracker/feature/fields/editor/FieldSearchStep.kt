package pl.farmtracker.feature.fields.editor

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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.Place
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.FarmTrackerScaffold
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.format.formatHectares
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.fields.R
import java.text.NumberFormat
import java.util.Locale

/** Szukanie działki po wsi i numerze („Otusz 125"); wyniki od najbliższej. */
@Composable
internal fun FieldSearchStep(
    uiState: FieldEditorUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (Parcel) -> Unit,
    onPickPlace: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val search = {
        focusManager.clearFocus()
        onSearch()
    }
    FarmTrackerScaffold(
        title = stringResource(R.string.fields_search_title),
        icon = Icons.Filled.Search,
        onBack = onBack,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.fields_search_label), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
            textStyle = MaterialTheme.typography.titleMedium,
            placeholder = { Text(stringResource(R.string.fields_search_hint), style = MaterialTheme.typography.titleMedium) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { search() }),
        )
        BigActionButton(
            text = stringResource(R.string.fields_search_button),
            icon = Icons.Filled.Search,
            onClick = search,
            enabled = uiState.searchQuery.isNotBlank() && uiState.search != SearchState.Searching,
        )
        when (val state = uiState.search) {
            SearchState.Idle -> Unit
            SearchState.Searching -> StatusPill(text = stringResource(R.string.fields_search_searching), icon = Icons.Filled.Search)
            SearchState.NotFound -> StatusPill(
                text = stringResource(R.string.fields_search_not_found),
                icon = Icons.Filled.Info,
                tone = Tone.Warning,
            )
            SearchState.Unavailable -> StatusPill(
                text = stringResource(R.string.fields_unavailable),
                icon = Icons.Filled.CloudOff,
                tone = Tone.Warning,
            )
            is SearchState.Results -> {
                Text(stringResource(R.string.fields_search_pick), style = MaterialTheme.typography.titleMedium)
                state.hits.forEach { hit -> SearchHitCard(hit = hit, onClick = { onPick(hit.parcel) }) }
            }
            is SearchState.Places -> {
                Text(stringResource(R.string.fields_search_pick_place), style = MaterialTheme.typography.titleMedium)
                state.hits.forEach { hit -> PlaceHitCard(hit = hit, onClick = { onPickPlace(hit.place) }) }
            }
        }
    }
}

/** Wieś w wynikach: nazwa, gmina i powiat (ta sama nazwa bywa w wielu gminach) oraz odległość. */
@Composable
private fun PlaceHitCard(hit: PlaceHit, onClick: () -> Unit) {
    val place = hit.place
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = place.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = listOf(place.commune, place.county).filter { it.isNotBlank() }.distinct().joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                )
                hit.distanceKm?.let {
                    Text(
                        text = stringResource(R.string.fields_search_distance, formatKilometers(it)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun SearchHitCard(hit: SearchHit, onClick: () -> Unit) {
    val parcel = hit.parcel
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.fields_search_result_title, parcel.number, parcel.precinct),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.fields_search_result_place, parcel.commune, parcel.county),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = listOfNotNull(
                        stringResource(R.string.fields_area, formatHectares(parcel.areaHectares)),
                        hit.distanceKm?.let { stringResource(R.string.fields_search_distance, formatKilometers(it)) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

/** „0,4", „3,2", „12" – do 10 km z jednym miejscem po przecinku, dalej w pełnych km. */
private fun formatKilometers(km: Double): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"))
        .apply { maximumFractionDigits = if (km < 10) 1 else 0 }
        .format(km)

@PreviewLightDark
@Composable
private fun FieldSearchPreview() {
    fun parcel(commune: String, county: String) = Parcel(
        id = "$commune-1",
        number = "1",
        precinct = "Bystrzyca",
        commune = commune,
        county = county,
        shape = listOf(GeoPolygon(listOf(GeoPoint(50.95, 17.35), GeoPoint(50.95, 17.36), GeoPoint(50.96, 17.36)))),
    )
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        FieldSearchStep(
            uiState = FieldEditorUiState(
                step = EditorStep.SEARCH,
                searchQuery = "Bystrzyca 1",
                search = SearchState.Results(
                    listOf(
                        SearchHit(parcel("Oława", "powiat oławski"), distanceKm = 3.2),
                        SearchHit(parcel("Zakrzówek", "powiat kraśnicki"), distanceKm = 318.0),
                    ),
                ),
            ),
            onBack = {},
            onQueryChanged = {},
            onSearch = {},
            onPick = {},
            onPickPlace = {},
        )
    }
}
