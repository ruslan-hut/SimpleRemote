package ua.com.programmer.simpleremote.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import javax.inject.Inject

@HiltViewModel
class ConnectionsListViewModel @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository,
): ViewModel() {

    val connections: StateFlow<List<ConnectionSettings>> = connectionRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setCurrentConnection(guid: String) {
        viewModelScope.launch {
            connectionRepo.setCurrent(guid)
        }
    }
}
