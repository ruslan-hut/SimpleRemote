package ua.com.programmer.simpleremote.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository
): ViewModel() {

    private val _guid = MutableStateFlow("")

    val connection: StateFlow<ConnectionSettings?> = _guid.flatMapLatest { guid ->
        connectionRepo.getByGuid(guid)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setCurrentConnection(guid: String?) {
        if (guid.isNullOrBlank()) {
            _guid.value = UUID.randomUUID().toString()
        } else {
            _guid.value = guid
        }
    }

    fun saveConnection(connection: ConnectionSettings, afterSave: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                connectionRepo.save(connection)
            }
            afterSave()
        }
    }

    fun deleteConnection(guid: String, afterDelete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                connectionRepo.delete(guid)
            }
            afterDelete()
        }
    }

}
