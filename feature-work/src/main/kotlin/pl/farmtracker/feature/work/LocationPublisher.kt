package pl.farmtracker.feature.work

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.core.domain.fieldWith
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.time.Clock
import javax.inject.Inject

/**
 * Zamienia odczyty GPS na „gdzie jestem i na którym polu" (geofencing na telefonie) i wysyła je, gdy trzeba –
 * nie przy każdym odczycie ([PositionReport.needsUpdate]).
 */
class LocationPublisher @Inject constructor(
    private val fieldRepository: FieldRepository,
    private val liveLocationRepository: LiveLocationRepository,
    private val clock: Clock,
) {

    /**
     * Działa do przerwania (koniec pracy). Po zmianie pól (admin dorysował pole) sprawdza ostatnią pozycję jeszcze raz.
     *
     * @param onField pole z każdej pozycji (`null` – poza polami), np. do powiadomienia
     */
    suspend fun publish(points: Flow<GeoPoint>, onField: (Field?) -> Unit = {}) {
        var last: PositionReport? = null
        combine(points, fieldRepository.fields) { point, fields -> point to fields }.collect { (point, fields) ->
            val now = clock.nowMillis()
            // Zmiana pola zawsze się wysyła, więc ostatnio wysłane pole = pole z poprzedniej pozycji.
            val field = fields.fieldWith(point, last?.fieldId)
            onField(field)
            if (last?.needsUpdate(point, field?.id, now) != false) {
                val report = PositionReport(point, field?.id, now)
                liveLocationRepository.publish(report)
                last = report
            }
        }
    }
}
