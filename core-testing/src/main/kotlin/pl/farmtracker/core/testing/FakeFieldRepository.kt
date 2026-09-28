package pl.farmtracker.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import pl.farmtracker.core.domain.Field
import pl.farmtracker.data.field.FieldRepository

class FakeFieldRepository(initial: List<Field> = emptyList()) : FieldRepository {

    private val _fields = MutableStateFlow(initial)
    override val fields: StateFlow<List<Field>> = _fields

    override suspend fun save(field: Field) {
        _fields.update { current -> current.filterNot { it.id == field.id } + field }
    }

    override suspend fun delete(id: String) {
        _fields.update { current -> current.filterNot { it.id == id } }
    }
}
