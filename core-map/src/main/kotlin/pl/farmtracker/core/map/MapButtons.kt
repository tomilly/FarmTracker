package pl.farmtracker.core.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

/** Okrągłe przyciski przy krawędzi mapy, jak w aplikacjach nawigacyjnych – mało zasłaniają. */
private val MapButtonSize = 56.dp

/** Niebieski „ja" – jak kropka mojej pozycji na mapie i celownik w Google Maps. */
private val FollowingBlue = Color(0xFF1A73E8)

/**
 * Okrągły przycisk na mapie z samą ikoną – tylko dla ikon znanych z nawigacji (wyśrodkuj, warstwy, powiększ);
 * opis dla TalkBack zawsze jest. Biały i z cieniem – widoczny na zdjęciu lotniczym.
 */
@Composable
fun MapIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(MapButtonSize),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = tint,
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(28.dp))
        }
    }
}

/** Mały przycisk z podpisem na mapie (np. „Admin", „Zmień rolę") – rzadko używany, więc nie na pierwszym planie. */
@Composable
fun MapTextButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = MapButtonSize),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/**
 * „Gdzie jestem" jako celownik: niebieski, gdy mapa jedzie za mną; pusty w środku, gdy ktoś ją przesunął
 * (dotknięcie znów pokazuje mnie i włącza jazdę za mną).
 */
@Composable
internal fun RecenterButton(following: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MapIconButton(
        icon = if (following) Icons.Filled.MyLocation else Icons.Filled.LocationSearching,
        contentDescription = stringResource(R.string.core_map_where_am_i),
        onClick = onClick,
        modifier = modifier,
        tint = if (following) FollowingBlue else MaterialTheme.colorScheme.onSurface,
    )
}

/** Warstwy: przełącza mapę ↔ zdjęcie lotnicze (TalkBack mówi, na co przełączy). */
@Composable
internal fun LayersButton(baseLayer: BaseLayer, onSelectBaseLayer: (BaseLayer) -> Unit, modifier: Modifier = Modifier) {
    val photo = baseLayer == BaseLayer.PHOTO
    MapIconButton(
        icon = Icons.Filled.Layers,
        contentDescription = stringResource(if (photo) R.string.core_map_layer_map else R.string.core_map_layer_photo),
        onClick = { onSelectBaseLayer(if (photo) BaseLayer.MAP else BaseLayer.PHOTO) },
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun MapButtonsPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LayersButton(baseLayer = BaseLayer.MAP, onSelectBaseLayer = {})
            RecenterButton(following = true, onClick = {})
            RecenterButton(following = false, onClick = {})
        }
    }
}
