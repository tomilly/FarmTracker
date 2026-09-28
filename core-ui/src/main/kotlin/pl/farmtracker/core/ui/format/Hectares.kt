package pl.farmtracker.core.ui.format

import java.text.NumberFormat
import java.util.Locale

/** Powierzchnia po polsku, zawsze z dwoma miejscami po przecinku: `9,30`. */
fun formatHectares(hectares: Double): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"))
        .apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        .format(hectares)
