package pl.farmtracker.core.domain.geo

/** Punkt w WGS84 (EPSG:4326), stopnie. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** Wielokąt: obrys zewnętrzny i ewentualne „dziury" (np. enklawa innej działki w środku). */
data class GeoPolygon(
    val outer: List<GeoPoint>,
    val holes: List<List<GeoPoint>> = emptyList(),
)
