package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import pl.farmtracker.core.domain.Member
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.session.PeopleRepository

class FakePeopleRepository(role: Role? = null, members: List<Member> = emptyList()) : PeopleRepository {
    override val myRole = MutableStateFlow(role)
    override val members = MutableStateFlow(members)
}
