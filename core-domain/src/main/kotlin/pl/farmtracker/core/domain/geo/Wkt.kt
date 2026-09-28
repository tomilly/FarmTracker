package pl.farmtracker.core.domain.geo

/**
 * Minimalny parser WKT dla wielokątów (tak zwraca geometrię usługa ULDK), np.
 * `SRID=4326;POLYGON((lon lat, lon lat, ...))` albo `MULTIPOLYGON(((...)),((...)))`.
 * Współrzędne w WKT są w kolejności x y = długość szerokość.
 */
object Wkt {

    /** @throws IllegalArgumentException gdy tekst nie jest wielokątem WKT. */
    fun parsePolygons(wkt: String): List<GeoPolygon> {
        val body = wkt.substringAfter(';', wkt).trim()
        val type = body.substringBefore('(').trim().uppercase()
        return when (type) {
            "POLYGON" -> listOf(polygon(inner(body)))
            "MULTIPOLYGON" -> splitTopLevel(inner(body)).map { polygon(inner(it)) }
            else -> throw IllegalArgumentException("Nieobsługiwany typ WKT: '$type'")
        }
    }

    private fun polygon(ringsText: String): GeoPolygon {
        val rings = splitTopLevel(ringsText).map { ring(inner(it)) }
        require(rings.isNotEmpty()) { "Wielokąt bez obrysu" }
        return GeoPolygon(outer = rings.first(), holes = rings.drop(1))
    }

    private fun ring(pointsText: String): List<GeoPoint> = pointsText.split(',').map { pair ->
        val parts = pair.trim().split(Whitespace)
        require(parts.size >= 2) { "Zły punkt WKT: '$pair'" }
        GeoPoint(latitude = parts[1].toDouble(), longitude = parts[0].toDouble())
    }

    /** Zawartość najbardziej zewnętrznych nawiasów. */
    private fun inner(text: String): String {
        val start = text.indexOf('(')
        val end = text.lastIndexOf(')')
        require(start >= 0 && end > start) { "Brak nawiasów w WKT" }
        return text.substring(start + 1, end).trim()
    }

    /** Dzieli „(a),(b)" na „(a)" i „(b)" – tylko po przecinkach poza nawiasami. */
    private fun splitTopLevel(text: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var from = 0
        text.forEachIndexed { index, char ->
            when (char) {
                '(' -> depth++
                ')' -> depth--
                ',' -> if (depth == 0) {
                    parts += text.substring(from, index).trim()
                    from = index + 1
                }
            }
        }
        parts += text.substring(from).trim()
        return parts.filter { it.isNotEmpty() }
    }

    private val Whitespace = Regex("\\s+")
}
