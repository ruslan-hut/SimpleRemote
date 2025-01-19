package ua.com.programmer.simpleremote.ui.connection

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    private val _guid = MutableLiveData("")

    val connection get() = _guid.switchMap {
        connectionRepo.getByGuid(it).asLiveData()
    }

    fun setCurrentConnection(guid: String?) {
        if (guid.isNullOrBlank()) {
            _guid.value = UUID.randomUUID().toString()
        }else{
            _guid.value = guid
        }
    }

    fun saveConnection(connection: ConnectionSettings, afterSave: () -> Unit) {
        viewModelScope.launch {
            connectionRepo.save(connection)
            withContext(Dispatchers.Main) {
                afterSave()
            }
        }
    }

    fun deleteConnection(guid: String, afterDelete: () -> Unit) {
        viewModelScope.launch {
            connectionRepo.delete(guid)
            withContext(Dispatchers.Main) {
                afterDelete()
            }
        }
    }

}