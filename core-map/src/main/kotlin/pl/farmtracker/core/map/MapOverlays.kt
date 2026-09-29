package pl.farmtracker.core.map

import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon

/**
 * Co ekran rysuje na mapie ponad warstwami bazowymi.
 *
 * @param fields pola zbioru – w swoim kolorze, z nazwą na środku
 * @param highlight zaznaczone kształty (żółta nakładka z obrysem), np. dotknięta działka
 * @param draft rysowany właśnie kształt – rogi w kolejności dotykania
 * @param entryPoints wjazdy na pola (zielone kropki z podpisem „Wjazd")
 * @param base baza zbioru – silos / pryzma (czarna kropka z podpisem „Baza")
 * @param people inni pracujący – kropka w kolorze roli z imieniem i rolą (siebie pokazuje niebieska kropka „ja")
 */
data class MapOverlays(
    val fields: List<Field> = emptyList(),
    val highlight: List<GeoPolygon> = emptyList(),
    val draft: List<GeoPoint> = emptyList(),
    val entryPoints: List<GeoPoint> = emptyList(),
    val base: GeoPoint? = null,
    val people: List<MapPerson> = emptyList(),
)

/** Pracujący na mapie. [isStale] – dawno bez nowej pozycji: szara kropka. */
data class MapPerson(
    val point: GeoPoint,
    val name: String,
    val role: Role,
    val isStale: Boolean,
)
