package pl.farmtracker.data.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.data.session.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Bez wspólnego zbioru: nikt inny nas nie widzi – tylko własna pozycja (do „na którym polu jestem"). */
@Singleton
class LocalLiveLocationRepository @Inject constructor(
    private val sessionRepository: SessionRepository,
) : LiveLocationRepository {

    private val mine = MutableStateFlow<PositionReport?>(null)

    override val locations: Flow<List<LiveLocation>> = combine(mine, sessionRepository.currentRole) { report, role ->
        if (report == null || role == null) {
            emptyList()
        } else {
            listOf(LiveLocation(ME, name = "", role, report.point, report.timeMillis, report.fieldId, report.trip, isMe = true))
        }
    }

    override suspend fun publish(report: PositionReport) {
        if (sessionRepository.currentRole.first() != null) mine.value = report
    }

    override suspend fun stopSharing() {
        mine.value = null
    }

    private companion object {
        const val ME = "me"
    }
}
