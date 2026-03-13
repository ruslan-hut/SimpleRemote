package ua.com.programmer.simpleremote.ui.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.FilterItem
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> get() = _documents

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _filterSchema = MutableStateFlow<List<FilterItem>>(emptyList())
    val filterSchema: StateFlow<List<FilterItem>> get() = _filterSchema

    private val _newDocument = MutableStateFlow<Document?>(null)
    val newDocument: StateFlow<Document?> get() = _newDocument

    private val _activeFilters = mutableListOf<FilterItem>()
    private var fetchJob: Job? = null

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

    fun createNewDocument() {
        _isLoading.value = true
        viewModelScope.launch {
            val document = withContext(Dispatchers.IO) {
                networkRepository.newDocument(type)
            }
            _newDocument.value = document
            _isLoading.value = false
        }
    }

    fun consumeNewDocument(): Document? {
        val doc = _newDocument.value
        _newDocument.value = null
        return doc
    }

    fun loadDocuments() {
        _isLoading.value = true
        if (type.isEmpty()) {
            _isLoading.value = false
            return
        }
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            networkRepository.documents(type, _activeFilters).collect { result ->
                _documents.value = result.documents
                if (result.filterSchema.isNotEmpty() && _filterSchema.value.isEmpty()) {
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
