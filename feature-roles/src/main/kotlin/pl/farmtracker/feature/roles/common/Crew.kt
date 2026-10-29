package pl.farmtracker.feature.roles.common

import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.distanceMeters

/** Gdzie jestem – z mojej pozycji: pole wykrywa telefon (geofencing), u kierowcy także co robi ([Trip]). */
sealed interface MyPosition {
    /** Pracuję, ale telefon jeszcze nie zna pozycji (albo dawno jej nie miał). */
    data object Searching : MyPosition

    data class OnField(val fieldName: String) : MyPosition

    data object OffField : MyPosition

    /** Kierowca jedzie przy sieczkarni. */
    data object Loading : MyPosition

    data object ToBase : MyPosition

    data object ToField : MyPosition

    data object AtBase : MyPosition
}

/** Ktoś inny, kto pracuje – np. „Marek – ładuje", „Rysiek – na polu Za lasem". */
data class Coworker(
    val name: String,
    val role: Role,
    /** `null` – poza polami (w drodze). */
    val fieldName: String?,
    /** Dawno bez nowej pozycji – nie wiadomo, czy to aktualne. */
    val isStale: Boolean,
    /** Co robi kierowca; `null` – nie kierowca albo jeszcze nie wiadomo. */
    val trip: Trip? = null,
    /** Ile minut stoi w miejscu; `null` – jedzie (albo stoi krótko, albo pozycja dawna). */
    val standingMinutes: Int? = null,
    /** Jak daleko (w linii prostej) od mnie albo od bazy; `null` – nie wiadomo. */
    val distanceMeters: Double? = null,
)

/** @param withTrip ekran kierowcy – zamiast samego pola pokazuje, co kierowca robi */
internal fun myPosition(
    locations: List<LiveLocation>,
    fields: List<Field>,
    nowMillis: Long,
    withTrip: Boolean = false,
): MyPosition {
    val mine = locations.firstOrNull { it.isMe }?.takeUnless { it.isStaleAt(nowMillis) } ?: return MyPosition.Searching
    val field = fields.firstOrNull { it.id == mine.fieldId }
    return when (if (withTrip) mine.trip else null) {
        Trip.LOADING -> MyPosition.Loading
        Trip.TO_BASE -> MyPosition.ToBase
        Trip.TO_FIELD -> MyPosition.ToField
        Trip.AT_BASE -> MyPosition.AtBase
        Trip.ON_FIELD, null -> if (field != null) MyPosition.OnField(field.name) else MyPosition.OffField
    }
}

/** Moja świeża pozycja – `null`, gdy telefon jej nie zna albo jest dawna. */
internal fun myPoint(locations: List<LiveLocation>, nowMillis: Long): GeoPoint? =
    locations.firstOrNull { it.isMe }?.takeUnless { it.isStaleAt(nowMillis) }?.point

/**
 * Pracujący w danej roli (bez tego telefonu), po imieniu.
 *
 * @param from skąd liczyć [Coworker.distanceMeters] (ja, baza); `null` – bez odległości
 */
internal fun coworkers(
    locations: List<LiveLocation>,
    fields: List<Field>,
    nowMillis: Long,
    role: Role,
    from: GeoPoint? = null,
): List<Coworker> =
    locations
        .filter { !it.isMe && it.role == role && !it.isGoneAt(nowMillis) }
        .map { location ->
            Coworker(
                name = location.name,
                role = location.role,
                fieldName = fields.firstOrNull { it.id == location.fieldId }?.name,
                isStale = location.isStaleAt(nowMillis),
                trip = location.trip.takeIf { location.role == Role.DRIVER },
                standingMinutes = location.takeUnless { it.isStaleAt(nowMillis) }?.standingMinutesAt(nowMillis),
                distanceMeters = from?.distanceMeters(location.point),
            )
        }
        .sortedBy { it.name }
