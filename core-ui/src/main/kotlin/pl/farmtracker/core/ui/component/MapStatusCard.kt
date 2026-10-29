package pl.farmtracker.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

/**
 * Mała karta stanu w rogu mapy – zamiast rzędu kolorowych pigułek: kropka „na żywo", stan jednym słowem
 * („Ładujesz"), pod nim drobniej szczegóły (kto gdzie). Do odczytania jednym rzutem oka; ciemny tekst na białym
 * tle jest czytelny w słońcu.
 *
 * @param live `true` – zielona kropka (inni mnie widzą), `false` – szara (nie pracuję), `null` – bez tej linii
 * @param details krótkie linijki pod stanem, np. „Rysiek – na polu Za lasem"
 */
@Composable
fun MapStatusCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    live: Boolean? = null,
    liveText: String? = null,
    details: List<String> = emptyList(),
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (live != null && liveText != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot(live)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = liveText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            details.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Zielona kropka z poświatą, jak „na żywo" w aplikacjach; szara – nikt mnie nie widzi. */
@Composable
private fun LiveDot(live: Boolean) {
    val go = FarmTrackerTheme.semanticColors.go
    val color = if (live) go else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    Box(
        Modifier
            .size(14.dp)
            .then(if (live) Modifier.border(3.dp, go.copy(alpha = 0.3f), CircleShape) else Modifier)
            .padding(if (live) 2.dp else 1.dp)
            .background(color, CircleShape),
    )
}

@PreviewLightDark
@Composable
private fun MapStatusCardPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MapStatusCard(
                title = "Ładujesz",
                icon = Icons.Filled.Download,
                live = true,
                liveText = "Na żywo – inni Cię widzą",
                details = listOf("Rysiek – na polu Za lasem"),
            )
            MapStatusCard(
                title = "Kierowca",
                live = false,
                liveText = "Nie pracujesz – nikt Cię nie widzi",
                details = listOf("Sieczkarnia nie pracuje"),
            )
        }
    }
}
