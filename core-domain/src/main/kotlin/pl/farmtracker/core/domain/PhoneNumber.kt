package pl.farmtracker.core.domain

/**
 * Numer telefonu w formacie międzynarodowym („+48600123456") z tego, co wpisał człowiek:
 * „600 123 456", „600-123-456", „+48 600 123 456", „0048600123456". Bez kierunkowego – Polska.
 * `null`, gdy to nie wygląda na numer.
 */
fun normalizePhoneNumber(text: String): String? {
    val compact = text.filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' }
    val international = when {
        compact.startsWith("+") -> compact
        compact.startsWith("00") -> "+" + compact.drop(2)
        compact.length == POLISH_NUMBER_LENGTH -> POLAND_PREFIX + compact
        else -> return null
    }
    val digits = international.drop(1)
    return international.takeIf { digits.all { it.isDigit() } && digits.length in MIN_DIGITS..MAX_DIGITS }
}

private const val POLAND_PREFIX = "+48"
private const val POLISH_NUMBER_LENGTH = 9

// E.164: najwyżej 15 cyfr; krótszych niż 8 (z kierunkowym) w praktyce nie ma.
private const val MIN_DIGITS = 8
private const val MAX_DIGITS = 15
