package pl.farmtracker.feature.work

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import pl.farmtracker.core.domain.HarvesterMoves
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.session.PeopleRepository
import javax.inject.Inject

/** „Rysiek – teraz na polu Przy drodze": komu i co pokazać w powiadomieniu. */
data class HarvesterMoveNotice(val harvesterName: String, val fieldName: String)

/**
 * Przejazdy sieczkarni na inne pole – liczone na telefonie kierowcy w czasie pracy, bez serwera ([HarvesterMoves]).
 * Tylko dla kierowcy (także admina, który pracuje jako kierowca): jemu mówią, dokąd teraz jechać po ładunek.
 */
class HarvesterMoveWatch @Inject constructor(
    private val liveLocationRepository: LiveLocationRepository,
    private val fieldRepository: FieldRepository,
    private val peopleRepository: PeopleRepository,
) {
    fun notices(): Flow<HarvesterMoveNotice> = flow {
        val moves = HarvesterMoves()
        combine(liveLocationRepository.locations, fieldRepository.fields, peopleRepository.myRole) { locations, fields, role ->
            Triple(locations, fields, role)
        }.collect { (locations, fields, role) ->
            // Śledzimy zawsze – po zmianie roli nie powiadamiamy o „przejeździe", który był dawno.
            val found = moves.update(locations)
            if (role != Role.DRIVER) return@collect
            found.forEach { move ->
                val field = fields.firstOrNull { it.id == move.fieldId } ?: return@forEach
                emit(HarvesterMoveNotice(move.harvesterName, field.name))
            }
        }
    }
}
