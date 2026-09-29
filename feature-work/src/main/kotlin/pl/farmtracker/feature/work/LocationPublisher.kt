package pl.farmtracker.feature.work

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.withIndex
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.TripTracker
import pl.farmtracker.core.domain.fieldWith
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.base.BaseRepository
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.time.Clock
import javax.inject.Inject

/**
 * Zamienia odczyty GPS na „gdzie jestem, na którym polu i co robię" (geofencing i status kierowcy na telefonie)
 * i wysyła je, gdy trzeba – nie przy każdym odczycie ([PositionReport.needsUpdate]).
 */
class LocationPublisher @Inject constructor(
    private val fieldRepository: FieldRepository,
    private val baseRepository: BaseRepository,
    private val liveLocationRepository: LiveLocationRepository,
    private val clock: Clock,
) {

    /**
     * Działa do przerwania (koniec pracy). Po zmianie pól (admin dorysował pole) sprawdza ostatnią pozycję jeszcze raz.
     *
     * @param onField pole z każdej pozycji (`null` – poza polami), np. do powiadomienia
     */
    suspend fun publish(points: Flow<GeoPoint>, onField: (Field?) -> Unit = {}) = coroutineScope {
        val base = baseRepository.base.map { it?.location }.stateIn(this, SharingStarted.Eagerly, null)
        val others = liveLocationRepository.locations.stateIn(this, SharingStarted.Eagerly, emptyList())
        val tripTracker = TripTracker()
        var last: PositionReport? = null
        var lastFixIndex = -1
        combine(points.withIndex(), fieldRepository.fields) { fix, fields -> fix to fields }.collect { (fix, fields) ->
            val point = fix.value
            val now = clock.nowMillis()
            // Zmiana pola zawsze się wysyła, więc ostatnio wysłane pole = pole z poprzedniej pozycji.
            val field = fields.fieldWith(point, last?.fieldId)
            onField(field)
            // Status liczy się z nowych odczytów GPS – nie z tej samej pozycji po zmianie pól.
            val trip = if (fix.index != lastFixIndex) {
                lastFixIndex = fix.index
                tripTracker.update(point, field?.id, base.value, others.value.freshHarvesters(now))
            } else {
                tripTracker.trip
            }
            if (last?.needsUpdate(point, field?.id, now, trip) != false) {
                val report = PositionReport(point, field?.id, now, trip)
                liveLocationRepository.publish(report)
                last = report
            }
        }
    }

    private fun List<LiveLocation>.freshHarvesters(nowMillis: Long): List<GeoPoint> =
        filter { !it.isMe && it.role == Role.HARVESTER && nowMillis - it.timeMillis <= HARVESTER_FRESH_MILLIS }
            .map { it.point }

    private companion object {
        /** Pozycja sieczkarni starsza niż 2 minuty nie mówi, czy kierowca jedzie przy niej. */
        const val HARVESTER_FRESH_MILLIS = 2 * 60_000L
    }
}
