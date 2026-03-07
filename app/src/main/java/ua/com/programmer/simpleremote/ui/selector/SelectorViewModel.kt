package ua.com.programmer.simpleremote.ui.selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.ListType
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class SelectorViewModel @Inject constructor(
    private val networkRepo: NetworkRepository
): ViewModel() {

    private val _documents = MutableStateFlow<List<ListType>>(emptyList())
    val documents: StateFlow<List<ListType>> get() = _documents

    private val _catalogs = MutableStateFlow<List<ListType>>(emptyList())
    val catalogs: StateFlow<List<ListType>> get() = _catalogs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> get() = _isError

    init {
        viewModelScope.launch {
            _isLoading.value = true
            networkRepo.userOptions.collect {
                _documents.value = it.document
                _catalogs.value = it.catalog

                _isLoading.value = false
                _isError.value = it.isEmpty
            }
        }
    }

    fun tryReconnect() {
        _isError.value = false
        _isLoading.value = true

        viewModelScope.launch {
            delay(3000)
            networkRepo.reconnect()
            _isLoading.value = false
        }
    }
}
