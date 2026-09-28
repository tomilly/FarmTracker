package pl.farmtracker.core.map

import pl.farmtracker.core.domain.geo.GeoPolygon

/**
 * Co ekran rysuje na mapie ponad warstwami bazowymi.
 *
 * @param highlight zaznaczone kształty (żółta nakładka z obrysem), np. dotknięta działka
 */
data class MapOverlays(
    val highlight: List<GeoPolygon> = emptyList(),
)
