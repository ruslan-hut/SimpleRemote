package ua.com.programmer.simpleremote.ui.selector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
        viewModelScope.launch {
            networkRepo.userOptions.collect {
                _documents.value = it.document
                _catalogs.value = it.catalog
            }
        }
    }
}