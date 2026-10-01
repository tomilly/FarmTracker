package pl.farmtracker.data.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.harvest.Membership
import javax.inject.Inject

/**
 * Kim jestem i kto jeszcze jest w zbiorze – tak samo ze wspólnym zbiorem i bez niego (wybiera aplikacja, moduł app).
 * Ekrany pytają tutaj, a nie [SessionRepository]: ze wspólnym zbiorem rola przychodzi z zaproszenia.
 */
interface PeopleRepository {
    /** Moja rola; `null` – jeszcze nie wiadomo (nie wybrano / nie należę do zbioru). */
    val myRole: Flow<Role?>

    /** Ludzie w zbiorze; bez wspólnego zbioru – nikt (dane tylko na tym telefonie). */
    val members: Flow<List<Member>>
}

/** Wspólny zbiór: rola z członkostwa; debug „Zmień rolę" przykrywa ją tylko na tym telefonie. */
class SharedPeopleRepository @Inject constructor(
    harvestRepository: HarvestRepository,
    sessionRepository: SessionRepository,
) : PeopleRepository {

    override val myRole: Flow<Role?> =
        combine(sessionRepository.currentRole, harvestRepository.membership) { picked, membership ->
            picked ?: (membership as? Membership.Joined)?.me?.role
        }

    override val members: Flow<List<Member>> = harvestRepository.members
}

/** Bez Firebase: rola wybrana na telefonie, nikogo więcej. */
class LocalPeopleRepository @Inject constructor(sessionRepository: SessionRepository) : PeopleRepository {

    override val myRole: Flow<Role?> = sessionRepository.currentRole

    override val members: Flow<List<Member>> = flowOf(emptyList())
}
