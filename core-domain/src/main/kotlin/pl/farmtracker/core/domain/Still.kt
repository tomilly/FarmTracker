package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.distanceMeters

/**
 * Od kiedy telefon stoi w miejscu – admin widzi „Marek – stoi od 12 min" (czeka pod sieczkarnią, awaria, przerwa).
 * „W miejscu" = w promieniu [STILL_RADIUS_METERS] od punktu, w którym się zatrzymał; GPS na postoju skacze o kilkanaście
 * metrów, a sieczkarnia przy pracy przejeżdża tyle w niecałą minutę.
 */
class StillTracker {

    private var anchor: GeoPoint? = null
    private var since: Long = 0

    /** @return od kiedy (ms) stoi w miejscu; po ruszeniu – czas tej pozycji */
    fun update(point: GeoPoint, nowMillis: Long): Long {
        val current = anchor
        if (current == null || current.distanceMeters(point) > STILL_RADIUS_METERS) {
            anchor = point
            since = nowMillis
        }
        return since
    }

    companion object {
        const val STILL_RADIUS_METERS = 50.0
    }
}
