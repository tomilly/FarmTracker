package pl.farmtracker.app.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.containsPoint
import pl.farmtracker.core.domain.geo.distanceMeters
import pl.farmtracker.data.base.BaseRepository
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.location.LocalLiveLocationRepository
import pl.farmtracker.data.time.Clock
import pl.farmtracker.data.time.ticks

/**
 * Wersja pokazowa (`-Pfarmtracker.demo`): na jednym telefonie, bez Firebase, pracuje też sieczkarnia „Rysiek" –
 * stoi na środku pola najbliższego bazy (ale nie tego, na którym jest baza). Do nagrań i do sprawdzenia „ładuje"
 * oraz mapy z innymi bez drugiego telefonu.
 */
class DemoLiveLocationRepository(
    private val local: LocalLiveLocationRepository,
    fieldRepository: FieldRepository,
    baseRepository: BaseRepository,
    clock: Clock,
) : LiveLocationRepository {

    override val locations: Flow<List<LiveLocation>> = combine(
        local.locations,
        fieldRepository.fields,
        baseRepository.base,
        clock.ticks(periodMillis = 10_000),
    ) { mine, fields, base, now ->
        mine + listOfNotNull(harvestedField(fields, base?.location)?.let { demoHarvester(it, now) })
    }

    override suspend fun publish(report: PositionReport) = local.publish(report)

    override suspend fun stopSharing() = local.stopSharing()

    private fun harvestedField(fields: List<Field>, base: GeoPoint?): Field? {
        if (base == null) return fields.firstOrNull()
        return fields.filterNot { it.shape.containsPoint(base) }.minByOrNull { it.shape.distanceMeters(base) }
    }

    private fun demoHarvester(field: Field, nowMillis: Long): LiveLocation? {
        val ring = field.shape.firstOrNull()?.outer?.takeIf { it.isNotEmpty() } ?: return null
        val center = GeoPoint(ring.map { it.latitude }.average(), ring.map { it.longitude }.average())
        return LiveLocation(
            userId = "demo-harvester",
            name = "Rysiek",
            role = Role.HARVESTER,
            point = center,
            timeMillis = nowMillis,
            fieldId = field.id,
        )
    }
}
