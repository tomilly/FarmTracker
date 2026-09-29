package pl.farmtracker.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

private val ButtonContentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

/**
 * Główny przycisk aplikacji: pełna szerokość, min. 72 dp, zawsze ikona + podpis.
 *
 * @param selected `null` – zwykły przycisk; `true`/`false` – przycisk wyboru (np. status kierowcy),
 * zaznaczony pokazuje „ptaszek" i jest tak ogłaszany przez TalkBack.
 */
@Composable
fun BigActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Primary,
    enabled: Boolean = true,
    selected: Boolean? = null,
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .heightIn(min = FarmTrackerDimens.BigButtonMinHeight)
        .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)

    val disabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)

    val content: @Composable RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(FarmTrackerDimens.BigButtonIconSize))
        Spacer(Modifier.width(FarmTrackerDimens.IconTextGap))
        Text(text = text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        if (selected == true) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(FarmTrackerDimens.BigButtonIconSize),
            )
        }
    }

    if (tone == Tone.Neutral) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            // Pełne tło – przycisk bywa położony na mapie albo zdjęciu lotniczym.
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = disabledContent,
            ),
            contentPadding = ButtonContentPadding,
            content = content,
        )
    } else {
        val colors = tone.colors
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            // Wyłączony też z pełnym tłem – domyślnie jest prawie przezroczysty i na mapie znika.
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.container,
                contentColor = colors.content,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = disabledContent,
            ),
            contentPadding = ButtonContentPadding,
            content = content,
        )
    }
}

@PreviewLightDark
@Composable
private fun BigActionButtonPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BigActionButton(text = "Zaczynam pracę", icon = Icons.Filled.PlayArrow, onClick = {}, tone = Tone.Go)
                BigActionButton(text = "Kończę pracę", icon = Icons.Filled.Stop, onClick = {}, tone = Tone.Stop)
                BigActionButton(
                    text = "Wiozę do bazy",
                    icon = Icons.Filled.LocalShipping,
                    onClick = {},
                    tone = Tone.Go,
                    selected = true,
                )
                BigActionButton(
                    text = "Jadę na pole",
                    icon = Icons.Filled.LocalShipping,
                    onClick = {},
                    tone = Tone.Neutral,
                    selected = false,
                )
            }
        }
    }
}

/** Jak w Material 3 – wyłączony napis wyraźnie bledszy, ale nadal czytelny. */
private const val DISABLED_CONTENT_ALPHA = 0.38f
