package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.farmtracker.data.work.WorkRepository

class FakeWorkRepository(working: Boolean = false) : WorkRepository {

    private val _isWorking = MutableStateFlow(working)
    override val isWorking: StateFlow<Boolean> = _isWorking

    override suspend fun setWorking(working: Boolean) {
        _isWorking.value = working
    }
}
