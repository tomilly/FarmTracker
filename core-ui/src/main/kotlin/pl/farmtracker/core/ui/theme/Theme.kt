package pl.farmtracker.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

private val FarmTrackerShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

/**
 * Motyw aplikacji. Domyślnie jasny (czytelny w słońcu) niezależnie od ustawień systemu;
 * [darkTheme] przyda się później jako „tryb nocny" przełączany przez użytkownika.
 */
@Composable
fun FarmTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSemanticColors provides if (darkTheme) DarkSemanticColors else LightSemanticColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = FarmTrackerTypography,
            shapes = FarmTrackerShapes,
            content = content,
        )
    }
}

object FarmTrackerTheme {
    val semanticColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current
}
