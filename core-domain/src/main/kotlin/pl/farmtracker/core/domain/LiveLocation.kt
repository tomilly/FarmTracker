package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.distanceMeters

/**
 * Ostatnia pozycja osoby, która pracuje – udostępnia lokalizację po „Zaczynam pracę" (BRIEF §8, Location).
 *
 * @param fieldId pole, na którym jest ta osoba – wykryte na jej telefonie ([fieldWith]); `null` – poza polami
 * @param isMe pozycja tego telefonu
 */
data class LiveLocation(
    val userId: String,
    val name: String,
    val role: Role,
    val point: GeoPoint,
    val timeMillis: Long,
    val fieldId: String? = null,
    val isMe: Boolean = false,
) {
    /** Dawno bez nowej pozycji (brak zasięgu, wyłączony telefon) – na mapie szara, z dopiskiem. */
    fun isStaleAt(nowMillis: Long): Boolean = nowMillis - timeMillis > STALE_AFTER_MILLIS

    /**
     * Czy wysłać nową pozycję – nie przy każdym odczycie GPS (bateria, transfer): od razu przy zmianie pola,
     * po przejechaniu kawałka, a na postoju co minutę – żeby inni widzieli, że telefon wciąż działa.
     */
    fun needsUpdate(point: GeoPoint, fieldId: String?, nowMillis: Long): Boolean {
        val elapsed = nowMillis - timeMillis
        return fieldId != this.fieldId ||
            elapsed >= HEARTBEAT_MILLIS ||
            (elapsed >= MIN_INTERVAL_MILLIS && this.point.distanceMeters(point) >= MIN_DISTANCE_METERS)
    }

    companion object {
        const val STALE_AFTER_MILLIS = 3 * 60_000L
        const val HEARTBEAT_MILLIS = 60_000L
        const val MIN_INTERVAL_MILLIS = 10_000L
        const val MIN_DISTANCE_METERS = 25.0
    }
}
