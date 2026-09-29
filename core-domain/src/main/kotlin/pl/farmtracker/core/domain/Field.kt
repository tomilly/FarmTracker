package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoArea
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.domain.geo.containsPoint

/** Kolory pól do wyboru. Bez zielonego (tło mapy) i żółtego (zaznaczenie) – żeby pola były widoczne. */
enum class FieldColor { BLUE, ORANGE, PURPLE, PINK, CYAN, BROWN }

enum class FieldStatus { PLANNED, ACTIVE, DONE }

/**
 * Pole do zbioru (BRIEF §8). Kształt to jedna lub więcej działek (albo narysowany wielokąt).
 *
 * @param parcelIds identyfikatory działek ULDK, z których powstało pole (puste dla narysowanego)
 * @param entryPoints wjazdy na pole (bramy) – do nawigacji kierowców; może ich być kilka albo żaden
 */
data class Field(
    val id: String,
    val name: String,
    val color: FieldColor,
    val shape: List<GeoPolygon>,
    val parcelIds: List<String> = emptyList(),
    val entryPoints: List<GeoPoint> = emptyList(),
    val status: FieldStatus = FieldStatus.PLANNED,
    val order: Int = 0,
) {
    val areaHectares: Double get() = GeoArea.hectares(shape)
}

/** Pole pod dotkniętym punktem mapy (`null` – dotknięto poza polami). */
fun List<Field>.fieldAt(point: GeoPoint): Field? = firstOrNull { it.shape.containsPoint(point) }
