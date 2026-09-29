package pl.farmtracker.feature.roles.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.time.Clock
import pl.farmtracker.data.time.ticks
import javax.inject.Inject

/** Pozycje pracujących, pola (do nazw) i bieżący czas – z czasem pozycja bez nowych danych robi się „dawna". */
data class CrewSnapshot(
    val locations: List<LiveLocation> = emptyList(),
    val fields: List<Field> = emptyList(),
    val nowMillis: Long = 0,
)

class CrewWatch @Inject constructor(
    private val liveLocationRepository: LiveLocationRepository,
    private val fieldRepository: FieldRepository,
    private val clock: Clock,
) {
    val snapshot: Flow<CrewSnapshot> =
        combine(liveLocationRepository.locations, fieldRepository.fields, clock.ticks()) { locations, fields, now ->
            // Czas z chwili zmiany pozycji, nie z ostatniego „tyknięcia" – świeża pozycja nie może wyjść na dawną.
            CrewSnapshot(locations, fields, maxOf(now, clock.nowMillis()))
        }
}
