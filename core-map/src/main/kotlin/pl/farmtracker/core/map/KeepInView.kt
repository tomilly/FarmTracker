package pl.farmtracker.core.map

import pl.farmtracker.core.domain.geo.GeoPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln

/**
 * Zoom, przy którym mapa z [center] na środku pokazuje też wszystkie [points] (np. kierowca jedzie, a sieczkarnia
 * ma być na ekranie). [halfWidthDp] / [halfHeightDp] – od środka do brzegu odkrytej części mapy, już bez marginesu.
 * Bliżej niż [maxZoom] nie przybliża (sam „ja" – zwykły widok z bliska), dalej niż [minZoom] nie oddala.
 */
internal fun zoomKeepingInView(
    center: GeoPoint,
    points: List<GeoPoint>,
    halfWidthDp: Double,
    halfHeightDp: Double,
    maxZoom: Double,
    minZoom: Double,
): Double {
    if (halfWidthDp <= 0.0 || halfHeightDp <= 0.0) return maxZoom
    val cosLat = cos(Math.toRadians(center.latitude))
    // Ile metrów na dp musi zmieścić mapa, żeby najdalszy punkt był jeszcze na ekranie.
    val metersPerDp = points.maxOfOrNull { point ->
        val east = abs(point.longitude - center.longitude) * METERS_PER_DEGREE * cosLat
        val north = abs(point.latitude - center.latitude) * METERS_PER_DEGREE
        maxOf(east / halfWidthDp, north / halfHeightDp)
    } ?: return maxZoom
    if (metersPerDp <= 0.0) return maxZoom
    val zoom = ln(METERS_PER_DP_AT_ZOOM_0 * cosLat / metersPerDp) / ln(2.0)
    return zoom.coerceIn(minZoom, maxZoom)
}

private const val METERS_PER_DEGREE = 111_320.0

/** Równik przy zoomie 0 w MapLibre (kafle 512 dp): obwód Ziemi / 512. */
private const val METERS_PER_DP_AT_ZOOM_0 = 78_271.517
