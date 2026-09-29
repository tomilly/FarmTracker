package pl.farmtracker.feature.fields.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.ui.FieldColorUi
import pl.farmtracker.core.ui.format.formatHectares
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.fields.R

/** Kółko w kolorze pola, z obwódką – widoczne także na jasnym tle. */
@Composable
internal fun ColorDot(color: FieldColor, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(FieldColorUi.color(color), CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

/** Wiersz pola na liście: kolor, nazwa, powierzchnia i „Na mapie" (cały wiersz jest dotykalny). */
@Composable
internal fun FieldCard(field: Field, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorDot(color = field.color, size = 32.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = field.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.fields_area, formatHectares(field.areaHectares)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Filled.Map, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.fields_on_map), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Wybór koloru: duże kafelki z kolorem i nazwą; wybrany ma grubą ramkę i „ptaszka". */
@Composable
internal fun ColorPicker(selected: FieldColor, onSelect: (FieldColor) -> Unit, modifier: Modifier = Modifier) {
    // Jedna kolumna: pełne nazwy („Pomarańczowy") mieszczą się także na wąskich telefonach.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldColor.entries.forEach { color ->
            ColorChoice(
                color = color,
                isSelected = color == selected,
                onClick = { onSelect(color) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColorChoice(color: FieldColor, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier.heightIn(min = FarmTrackerDimens.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (isSelected) 4.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = color, size = 36.dp)
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(FieldColorUi.labelRes(color)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun ColorPickerPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            ColorPicker(selected = FieldColor.ORANGE, onSelect = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
