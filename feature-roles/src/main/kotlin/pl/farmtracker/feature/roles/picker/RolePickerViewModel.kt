package pl.farmtracker.feature.roles.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Role
import pl.farmtracker.data.session.SessionRepository
import javax.inject.Inject

@HiltViewModel
class RolePickerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    fun selectRole(role: Role) {
        viewModelScope.launch { sessionRepository.setRole(role) }
    }
}
