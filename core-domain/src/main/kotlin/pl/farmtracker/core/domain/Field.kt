package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoArea
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon

/** Kolory pól do wyboru. Bez zielonego (tło mapy) i żółtego (zaznaczenie) – żeby pola były widoczne. */
enum class FieldColor { BLUE, ORANGE, PURPLE, PINK, CYAN, BROWN }

enum class FieldStatus { PLANNED, ACTIVE, DONE }

/**
 * Pole do zbioru (BRIEF §8). Kształt to jedna lub więcej działek (albo narysowany wielokąt).
 *
 * @param parcelIds identyfikatory działek ULDK, z których powstało pole (puste dla narysowanego)
 * @param entryPoint wjazd na pole (brama) – do nawigacji kierowców
 */
data class Field(
    val id: String,
    val name: String,
    val color: FieldColor,
    val shape: List<GeoPolygon>,
    val parcelIds: List<String> = emptyList(),
    val entryPoint: GeoPoint? = null,
    val status: FieldStatus = FieldStatus.PLANNED,
    val order: Int = 0,
) {
    val areaHectares: Double get() = GeoArea.hectares(shape)
}
