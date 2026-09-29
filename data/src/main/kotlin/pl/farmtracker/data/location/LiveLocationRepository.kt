package pl.farmtracker.data.location

import kotlinx.coroutines.flow.Flow
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.PositionReport

/** Pozycje osób, które pracują – mapa na żywo i „Sieczkarnia jest na polu…" (BRIEF §5 p. 5–6). */
interface LiveLocationRepository {
    /** Wszyscy, którzy teraz udostępniają lokalizację – łącznie z tym telefonem ([LiveLocation.isMe]). */
    val locations: Flow<List<LiveLocation>>

    /** Nowa pozycja tego telefonu. Bez zasięgu czeka na telefonie i wysyła się później. */
    suspend fun publish(report: PositionReport)

    /** „Kończę pracę" – inni przestają widzieć ten telefon. */
    suspend fun stopSharing()
}
