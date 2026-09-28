package pl.farmtracker.core.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import pl.farmtracker.core.domain.FieldColor

/** Jak kolor pola wygląda w UI i na mapie; nazwa – dla czytnika ekranu i osób nierozróżniających kolorów. */
object FieldColorUi {
    fun color(fieldColor: FieldColor): Color = when (fieldColor) {
        FieldColor.BLUE -> Color(0xFF1E88E5)
        FieldColor.ORANGE -> Color(0xFFF57C00)
        FieldColor.PURPLE -> Color(0xFF8E24AA)
        FieldColor.PINK -> Color(0xFFD81B60)
        FieldColor.CYAN -> Color(0xFF00ACC1)
        FieldColor.BROWN -> Color(0xFF8D6E63)
    }

    @StringRes
    fun labelRes(fieldColor: FieldColor): Int = when (fieldColor) {
        FieldColor.BLUE -> R.string.core_ui_color_blue
        FieldColor.ORANGE -> R.string.core_ui_color_orange
        FieldColor.PURPLE -> R.string.core_ui_color_purple
        FieldColor.PINK -> R.string.core_ui_color_pink
        FieldColor.CYAN -> R.string.core_ui_color_cyan
        FieldColor.BROWN -> R.string.core_ui_color_brown
    }
}
