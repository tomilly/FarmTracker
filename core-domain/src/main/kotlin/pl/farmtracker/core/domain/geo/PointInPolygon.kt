package pl.farmtracker.core.domain.geo

/**
 * Czy punkt leży w wielokącie (w obrysie i poza „dziurami"). Metoda promienia – dla pól i działek
 * (do kilku km) krzywizna Ziemi nie ma znaczenia. Punkt dokładnie na krawędzi może wypaść w dowolną stronę.
 */
fun GeoPolygon.contains(point: GeoPoint): Boolean =
    ringContains(outer, point) && holes.none { ringContains(it, point) }

fun List<GeoPolygon>.containsPoint(point: GeoPoint): Boolean = any { it.contains(point) }

private fun ringContains(ring: List<GeoPoint>, point: GeoPoint): Boolean {
    var inside = false
    var j = ring.lastIndex
    for (i in ring.indices) {
        val a = ring[i]
        val b = ring[j]
        val crosses = (a.latitude > point.latitude) != (b.latitude > point.latitude)
        if (crosses) {
            val lonAtLat = (b.longitude - a.longitude) * (point.latitude - a.latitude) /
                (b.latitude - a.latitude) + a.longitude
            if (point.longitude < lonAtLat) inside = !inside
        }
        j = i
    }
    return inside
}
