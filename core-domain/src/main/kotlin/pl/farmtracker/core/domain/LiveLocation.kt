package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.distanceMeters

/**
 * Ostatnia pozycja osoby, która pracuje – udostępnia lokalizację po „Zaczynam pracę" (BRIEF §8, Location).
 *
 * @param fieldId pole, na którym jest ta osoba – wykryte na jej telefonie ([fieldWith]); `null` – poza polami
 * @param trip co robi kierowca ([TripTracker]); `null` – nie wiadomo albo to nie kierowca
 * @param isMe pozycja tego telefonu
 */
data class LiveLocation(
    val userId: String,
    val name: String,
    val role: Role,
    val point: GeoPoint,
    val timeMillis: Long,
    val fieldId: String? = null,
    val trip: Trip? = null,
    val isMe: Boolean = false,
) {
    /** Dawno bez nowej pozycji (brak zasięgu, wyłączony telefon) – pokazujemy ją z dopiskiem, na szaro. */
    fun isStaleAt(nowMillis: Long): Boolean = nowMillis - timeMillis > STALE_AFTER_MILLIS

    /** Pozycja sprzed wielu godzin (telefon zgasł bez „Kończę pracę") – już jej nie pokazujemy. */
    fun isGoneAt(nowMillis: Long): Boolean = nowMillis - timeMillis > GONE_AFTER_MILLIS

    companion object {
        const val STALE_AFTER_MILLIS = 3 * 60_000L
        const val GONE_AFTER_MILLIS = 12 * 60 * 60_000L
    }
}

/** Co telefon wysłał o sobie: gdzie był, na którym polu, co robił (kierowca) i kiedy. */
data class PositionReport(
    val point: GeoPoint,
    val fieldId: String?,
    val timeMillis: Long,
    val trip: Trip? = null,
) {
    /**
     * Czy wysłać nową pozycję – nie przy każdym odczycie GPS (bateria, transfer): od razu przy zmianie pola
     * albo statusu kierowcy, po przejechaniu kawałka, a na postoju co minutę – żeby inni widzieli, że telefon działa.
     */
    fun needsUpdate(point: GeoPoint, fieldId: String?, nowMillis: Long, trip: Trip? = this.trip): Boolean {
        val elapsed = nowMillis - timeMillis
        return fieldId != this.fieldId ||
            trip != this.trip ||
            elapsed >= HEARTBEAT_MILLIS ||
            (elapsed >= MIN_INTERVAL_MILLIS && this.point.distanceMeters(point) >= MIN_DISTANCE_METERS)
    }

    companion object {
        const val HEARTBEAT_MILLIS = 60_000L
        const val MIN_INTERVAL_MILLIS = 10_000L
        const val MIN_DISTANCE_METERS = 25.0
    }
}
