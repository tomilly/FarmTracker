package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.farmtracker.core.domain.Base
import pl.farmtracker.data.base.BaseRepository

class FakeBaseRepository(initial: Base? = null) : BaseRepository {

    private val _base = MutableStateFlow(initial)
    override val base: StateFlow<Base?> = _base

    override suspend fun save(base: Base) {
        _base.value = base
    }

    override suspend fun clear() {
        _base.value = null
    }
}
