package pl.farmtracker.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

/** Znaczenie kolorystyczne elementu: zielony = jedź/OK, czerwony = stop, żółty = uwaga. */
enum class Tone { Primary, Go, Stop, Warning, Neutral }

internal data class ToneColors(val container: Color, val content: Color)

internal val Tone.colors: ToneColors
    @Composable
    @ReadOnlyComposable
    get() {
        val scheme = MaterialTheme.colorScheme
        val semantic = FarmTrackerTheme.semanticColors
        return when (this) {
            Tone.Primary -> ToneColors(scheme.primary, scheme.onPrimary)
            Tone.Go -> ToneColors(semantic.go, semantic.onGo)
            Tone.Stop -> ToneColors(semantic.stop, semantic.onStop)
            Tone.Warning -> ToneColors(semantic.warning, semantic.onWarning)
            Tone.Neutral -> ToneColors(scheme.surfaceVariant, scheme.onSurfaceVariant)
        }
    }
