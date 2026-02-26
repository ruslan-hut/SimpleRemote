package ua.com.programmer.simpleremote.ui.connection

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import javax.inject.Inject

@HiltViewModel
class ConnectionsListViewModel @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository,
): ViewModel() {

    val connections: LiveData<List<ConnectionSettings>> = connectionRepo.getAll().asLiveData()

    fun setCurrentConnection(guid: String) {
        viewModelScope.launch {
            connectionRepo.setCurrent(guid)
        }
    }
}