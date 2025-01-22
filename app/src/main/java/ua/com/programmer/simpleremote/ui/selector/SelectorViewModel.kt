package ua.com.programmer.simpleremote.ui.selector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.entity.DataType
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class SelectorViewModel @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository,
    private val networkRepo: NetworkRepository
): ViewModel() {

    private val _documents = MutableLiveData<List<DataType>>()
    val documents: LiveData<List<DataType>> get() = _documents

    private val _catalogs = MutableLiveData<List<DataType>>()
    val catalogs: LiveData<List<DataType>> get() = _catalogs

    private val _connection = MutableLiveData<ConnectionSettings>()
    val connection get() = _connection

    init {
        viewModelScope.launch {
            connectionRepo.currentConnection.collect {
                _connection.value = it ?: ConnectionSettings.Builder.buildDemo()
            }
        }
        viewModelScope.launch {
            networkRepo.userOptions.collect {
                _documents.value = it.document
                _catalogs.value = it.catalog
            }
        }
    }
}