package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.distanceMeters

/** Dokąd prowadzić kierowcę jednym przyciskiem (nawigacja w Google Maps) – bez wybierania celu. */
sealed interface DriveTo {
    val point: GeoPoint

    /** Na pole, na którym pracuje sieczkarnia – do wjazdu (albo do samej sieczkarni, gdy pole nie ma wjazdów). */
    data class ToField(val fieldName: String, override val point: GeoPoint) : DriveTo

    data class ToBase(override val point: GeoPoint) : DriveTo
}

/**
 * Cel z tego, co wykrył telefon ([Trip]): z ładunkiem (ładuje, wraca do bazy) albo już na polu sieczkarni (ładował
 * albo czeka – nawigacja na pole na nic) – baza; w pozostałych chwilach – pole z sieczkarnią, wjazd najbliższy
 * kierowcy. `null` – nie ma dokąd (sieczkarnia nie pracuje, brak bazy).
 *
 * @param locations pozycje pracujących, z moją ([LiveLocation.isMe])
 */
fun driveTo(locations: List<LiveLocation>, fields: List<Field>, base: GeoPoint?, nowMillis: Long): DriveTo? {
    val me = locations.firstOrNull { it.isMe }
    if (me?.trip == Trip.LOADING || me?.trip == Trip.TO_BASE) return base?.let { DriveTo.ToBase(it) }

    val harvester = locations
        .filter { !it.isMe && it.role == Role.HARVESTER && !it.isGoneAt(nowMillis) && it.fieldId != null }
        // Kilka sieczkarń – ta najbliżej mnie.
        .minByOrNull { harvester -> me?.point?.distanceMeters(harvester.point) ?: 0.0 }
        ?: return null
    val field = fields.firstOrNull { it.id == harvester.fieldId } ?: return null
    if (me?.fieldId == field.id) return base?.let { DriveTo.ToBase(it) }
    val entry = field.entryPoints.minByOrNull { entry -> me?.point?.distanceMeters(entry) ?: 0.0 }
    return DriveTo.ToField(field.name, entry ?: harvester.point)
}
