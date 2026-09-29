package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.location.LiveLocationRepository

/** Pozycje na niby: inni pracujący ustawiani w teście ([locations]), moja z [publish] (jako [myRole]). */
class FakeLiveLocationRepository(
    others: List<LiveLocation> = emptyList(),
    private val myRole: Role = Role.HARVESTER,
) : LiveLocationRepository {

    override val locations = MutableStateFlow(others)

    val published = mutableListOf<PositionReport>()

    override suspend fun publish(report: PositionReport) {
        published += report
        val mine = LiveLocation(ME, "Ja", myRole, report.point, report.timeMillis, report.fieldId, isMe = true)
        locations.update { all -> all.filterNot { it.isMe } + mine }
    }

    override suspend fun stopSharing() {
        locations.update { all -> all.filterNot { it.isMe } }
    }

    companion object {
        const val ME = "me"
    }
}
