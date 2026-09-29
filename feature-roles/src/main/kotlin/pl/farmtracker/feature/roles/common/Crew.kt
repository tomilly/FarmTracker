package pl.farmtracker.feature.roles.common

import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.Role

/** Gdzie jestem – z mojej pozycji, pole wykrywa telefon (geofencing). */
sealed interface MyPosition {
    /** Pracuję, ale telefon jeszcze nie zna pozycji (albo dawno jej nie miał). */
    data object Searching : MyPosition

    data class OnField(val fieldName: String) : MyPosition

    data object OffField : MyPosition
}

/** Ktoś inny, kto pracuje – np. „Marek – na polu Za lasem". */
data class Coworker(
    val name: String,
    val role: Role,
    /** `null` – poza polami (w drodze). */
    val fieldName: String?,
    /** Dawno bez nowej pozycji – nie wiadomo, czy to aktualne. */
    val isStale: Boolean,
)

internal fun myPosition(locations: List<LiveLocation>, fields: List<Field>, nowMillis: Long): MyPosition {
    val mine = locations.firstOrNull { it.isMe }?.takeUnless { it.isStaleAt(nowMillis) } ?: return MyPosition.Searching
    val field = fields.firstOrNull { it.id == mine.fieldId } ?: return MyPosition.OffField
    return MyPosition.OnField(field.name)
}

/** Pracujący w danej roli (bez tego telefonu), po imieniu. */
internal fun coworkers(locations: List<LiveLocation>, fields: List<Field>, nowMillis: Long, role: Role): List<Coworker> =
    locations
        .filter { !it.isMe && it.role == role && !it.isGoneAt(nowMillis) }
        .map { location ->
            Coworker(
                name = location.name,
                role = location.role,
                fieldName = fields.firstOrNull { it.id == location.fieldId }?.name,
                isStale = location.isStaleAt(nowMillis),
            )
        }
        .sortedBy { it.name }
