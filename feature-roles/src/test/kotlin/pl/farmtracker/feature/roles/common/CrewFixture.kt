package pl.farmtracker.feature.roles.common

import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.testing.FakeClock
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.FakeLiveLocationRepository

/** Pole „Za lasem", zegar i pozycje pracujących – wspólne dla testów ekranów ról. */
class CrewFixture(myRole: Role = Role.HARVESTER) {

    val field = Field(
        id = "f1",
        name = "Za lasem",
        color = FieldColor.BLUE,
        shape = listOf(
            GeoPolygon(listOf(GeoPoint(50.0, 17.0), GeoPoint(50.0, 17.01), GeoPoint(50.01, 17.01), GeoPoint(50.01, 17.0))),
        ),
    )
    val onTheField = GeoPoint(50.005, 17.005)
    val onTheRoad = GeoPoint(50.02, 17.005)

    val clock = FakeClock(now = 1_000_000)
    val locations = FakeLiveLocationRepository(myRole = myRole)
    val watch = CrewWatch(locations, FakeFieldRepository(listOf(field)), clock)

    fun someone(name: String, role: Role, point: GeoPoint, fieldId: String?, minutesAgo: Int = 0) = LiveLocation(
        userId = name,
        name = name,
        role = role,
        point = point,
        timeMillis = clock.now - minutesAgo * 60_000L,
        fieldId = fieldId,
    )
}
