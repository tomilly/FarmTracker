package pl.farmtracker.feature.fields.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Field
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.feature.fields.common.DeletedFieldBin
import javax.inject.Inject

sealed interface FieldsListUiState {
    data object Loading : FieldsListUiState
    data class Ready(val fields: List<Field>) : FieldsListUiState
}

@HiltViewModel
class FieldsListViewModel @Inject constructor(
    private val fieldRepository: FieldRepository,
    private val deletedFieldBin: DeletedFieldBin,
) : ViewModel() {

    val uiState: StateFlow<FieldsListUiState> = fieldRepository.fields
        .map<List<Field>, FieldsListUiState> { FieldsListUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FieldsListUiState.Loading)

    /** Pole usunięte przed chwilą – lista pokazuje „Usunięto… Cofnij". */
    val recentlyDeleted: StateFlow<Field?> = deletedFieldBin.lastDeleted

    fun undoDelete() {
        val field = deletedFieldBin.take() ?: return
        viewModelScope.launch { fieldRepository.save(field) }
    }

    fun dismissDeleted() {
        deletedFieldBin.take()
    }
}
