package pl.farmtracker.core.ui.format

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Odległość w linii prostej, krótko i po polsku: do kilometra co 50 m („350 m"), dalej z jednym miejscem po przecinku
 * („4,2 km"), od 10 km w pełnych kilometrach („12 km") – dokładniej i tak nie wiadomo, bo droga jest dłuższa.
 * Liczba i jednostka nierozdzielne (twarda spacja) – „1,6" i „km" nie trafią do różnych linijek.
 */
fun formatDistance(meters: Double): String {
    if (meters < 950) return "${maxOf(50, (meters / 50).roundToInt() * 50)}${NBSP}m"
    val km = meters / 1000
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL")).apply {
        maximumFractionDigits = if (km < 10) 1 else 0
        minimumFractionDigits = if (km < 10) 1 else 0
    }
    return "${format.format(km)}${NBSP}km"
}

private const val NBSP = ' '
