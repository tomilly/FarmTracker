package pl.farmtracker.core.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

/** Stały wskaźnik stanu („Pracujesz", później „Lokalizacja włączona") – zawsze ikona + tekst. */
@Composable
fun StatusPill(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Neutral,
) {
    val colors = tone.colors
    Surface(
        modifier = modifier.heightIn(min = 48.dp),
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@PreviewLightDark
@Composable
private fun StatusPillPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusPill(text = "Pracujesz", icon = Icons.Filled.Agriculture, tone = Tone.Go)
                StatusPill(text = "Nie pracujesz", icon = Icons.Filled.Info)
            }
        }
    }
}
