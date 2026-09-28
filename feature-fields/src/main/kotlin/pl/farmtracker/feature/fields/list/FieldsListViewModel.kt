package pl.farmtracker.feature.fields.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.farmtracker.core.domain.Field
import pl.farmtracker.data.field.FieldRepository
import javax.inject.Inject

sealed interface FieldsListUiState {
    data object Loading : FieldsListUiState
    data class Ready(val fields: List<Field>) : FieldsListUiState
}

@HiltViewModel
class FieldsListViewModel @Inject constructor(
    fieldRepository: FieldRepository,
) : ViewModel() {

    val uiState: StateFlow<FieldsListUiState> = fieldRepository.fields
        .map<List<Field>, FieldsListUiState> { FieldsListUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FieldsListUiState.Loading)
}
