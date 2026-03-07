package ua.com.programmer.simpleremote.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.Catalog
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class CatalogListViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {

    private val _elements = MutableStateFlow<List<Catalog>>(emptyList())
    val elements: StateFlow<List<Catalog>> get() = _elements

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    var title: String = ""
    var type: String = ""
    private var group: String = ""
    var docGuid: String = ""
    var docType: String = ""

    private var searchFilter: String = ""
    private var fetchJob: Job? = null

    private fun fetchElements() {
        fetchJob?.cancel()
        _isLoading.value = true
        fetchJob = viewModelScope.launch {
            networkRepository.catalog(type, group, docGuid, searchFilter).collect { catalog ->
                _elements.value = catalog
                _isLoading.value = false
            }
        }
    }

    fun setCatalogType(type: String?, title: String?, group: String?) {
        this.title = title ?: ""
        this.type = type ?: ""
        this.group = group ?: ""
        fetchElements()
    }

    fun search(query: String) {
        searchFilter = query
        fetchElements()
    }

    fun clearSearch() {
        if (searchFilter.isNotEmpty()) {
            searchFilter = ""
            fetchElements()
        }
    }
}
