package ua.com.programmer.simpleremote.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository
) : ViewModel() {

    private val _connection = MutableLiveData<ConnectionSettings>()
    val connection get() = _connection

    init {
        Log.d("RC_MainViewModel", "init")
        viewModelScope.launch {
            connectionRepo.currentConnection.collect {
                _connection.value = it ?: ConnectionSettings.Builder.buildDemo()
            }
        }
    }
}