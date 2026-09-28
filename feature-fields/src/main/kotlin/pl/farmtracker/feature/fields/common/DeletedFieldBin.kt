package pl.farmtracker.feature.fields.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.farmtracker.core.domain.Field
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ostatnio usunięte pole – do „Cofnij" na liście pól (BRIEF §4: cofnięcie zamiast pytania „czy na pewno?").
 * Pole usuwa ekran edycji, a przywrócić je można z listy, więc schowek jest wspólny dla obu ekranów.
 */
@Singleton
class DeletedFieldBin @Inject constructor() {

    private val _lastDeleted = MutableStateFlow<Field?>(null)
    val lastDeleted: StateFlow<Field?> = _lastDeleted.asStateFlow()

    fun put(field: Field) {
        _lastDeleted.value = field
    }

    /** Zabiera pole ze schowka (np. do przywrócenia); kolejne wywołanie zwraca `null`. */
    fun take(): Field? = _lastDeleted.value.also { _lastDeleted.value = null }
}
