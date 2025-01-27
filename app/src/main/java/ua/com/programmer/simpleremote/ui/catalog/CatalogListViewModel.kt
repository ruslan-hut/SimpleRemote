package ua.com.programmer.simpleremote.ui.catalog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.Catalog
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class CatalogListViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {

    private val _elements = MutableLiveData<List<Catalog>>()
    val elements: LiveData<List<Catalog>> get() = _elements

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    var title: String = ""
    var type: String = ""
    private var group: String = ""
    var docGuid: String = ""

    private fun fetchElements() {
        _isLoading.value = true
        viewModelScope.launch {
            networkRepository.catalog(type, group, docGuid).collect { catalog ->
                _elements.value = catalog
                _isLoading.value = false
            }
        }
    }

    fun setCatalogType(type: String?, title: String?, group: String?, docGuid: String?) {
        this.title = title ?: ""
        this.type = type ?: ""
        this.group = group ?: ""
        this.docGuid = docGuid ?: ""
        fetchElements()
    }
}