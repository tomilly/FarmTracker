package pl.farmtracker.core.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource

/**
 * Formy liczby mnogiej w polskim: 1 działka, 2–4 działki, 5+ działek (ale 12–14 działek, 22 działki).
 *
 * Nie używamy `plurals` z zasobów: Android dobiera w nich formę według języka *telefonu*, więc na
 * telefonie po angielsku wyszłoby „4 rogu". Aplikacja jest tylko po polsku, więc reguła jest stała.
 */
enum class PolishPlural { ONE, FEW, MANY }

fun polishPlural(count: Int): PolishPlural {
    val lastDigit = count % 10
    val lastTwo = count % 100
    return when {
        count == 1 -> PolishPlural.ONE
        lastDigit in 2..4 && lastTwo !in 12..14 -> PolishPlural.FEW
        else -> PolishPlural.MANY
    }
}

/**
 * Tekst z liczbą w poprawnej formie, np. `pluralStringPl(R.string.x_one, R.string.x_few, R.string.x_many, 4)`
 * → „4 rogi". Każdy z zasobów dostaje liczbę jako `%1$d`.
 */
@Composable
@ReadOnlyComposable
fun pluralStringPl(@StringRes one: Int, @StringRes few: Int, @StringRes many: Int, count: Int): String =
    when (polishPlural(count)) {
        PolishPlural.ONE -> stringResource(one, count)
        PolishPlural.FEW -> stringResource(few, count)
        PolishPlural.MANY -> stringResource(many, count)
    }
