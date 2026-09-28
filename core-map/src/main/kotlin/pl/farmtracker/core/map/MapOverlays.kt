package pl.farmtracker.core.map

import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.geo.GeoPolygon

/**
 * Co ekran rysuje na mapie ponad warstwami bazowymi.
 *
 * @param fields pola zbioru – w swoim kolorze, z nazwą na środku
 * @param highlight zaznaczone kształty (żółta nakładka z obrysem), np. dotknięta działka
 */
data class MapOverlays(
    val fields: List<Field> = emptyList(),
    val highlight: List<GeoPolygon> = emptyList(),
)
