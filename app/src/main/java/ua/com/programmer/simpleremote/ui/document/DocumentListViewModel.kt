package ua.com.programmer.simpleremote.ui.document

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.FilterItem
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {

    private val _documents = MutableLiveData<List<Document>>()
    val documents: LiveData<List<Document>> get() = _documents

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _filterSchema = MutableLiveData<List<FilterItem>>(emptyList())
    val filterSchema: LiveData<List<FilterItem>> get() = _filterSchema

    private val _activeFilters = mutableListOf<FilterItem>()

    var title: String = ""
    var type: String = ""

    fun setDocumentType(type: String?, title: String?) {
        this.title = title ?: ""
        this.type = type ?: ""
    }

    fun getActiveFilters(): List<FilterItem> = _activeFilters.toList()

    fun setActiveFilters(filters: List<FilterItem>) {
        _activeFilters.clear()
        _activeFilters.addAll(filters)
    }

    fun updateFilterValue(name: String, code: String, value: String) {
        _activeFilters.find { it.name == name }?.apply {
            this.code = code
            this.value = value
        }
    }

    fun clearAllFilters() {
        _activeFilters.forEach { it.clearValue() }
    }

    fun loadDocuments() {
        _isLoading.value = true
        if (type.isEmpty()) {
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            networkRepository.documents(type, _activeFilters).collect { result ->
                _documents.value = result.documents
                if (result.filterSchema.isNotEmpty() && _filterSchema.value.isNullOrEmpty()) {
                    _filterSchema.value = result.filterSchema
                    if (_activeFilters.isEmpty()) {
                        _activeFilters.addAll(result.filterSchema.map { it.copy() })
                    }
                }
                _isLoading.value = false
            }
        }
    }
}
