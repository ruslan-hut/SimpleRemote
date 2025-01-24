package ua.com.programmer.simpleremote.ui.selector

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.ListType
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class SelectorViewModel @Inject constructor(
    private val networkRepo: NetworkRepository
): ViewModel() {

    private val _documents = MutableLiveData<List<ListType>>()
    val documents: LiveData<List<ListType>> get() = _documents

    private val _catalogs = MutableLiveData<List<ListType>>()
    val catalogs: LiveData<List<ListType>> get() = _catalogs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        viewModelScope.launch {
            _isLoading.value = true
            networkRepo.userOptions.collect {
                _documents.value = it.document
                _catalogs.value = it.catalog
                _isLoading.value = false
            }
        }
    }

    fun tryReconnect() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(3000)
            networkRepo.reconnect()
            _isLoading.value = false
        }
    }
}