package ua.com.programmer.simpleremote.ui.document

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
    ): ViewModel() {

    private val _content = MutableStateFlow<List<Content>>(emptyList())
    val content: StateFlow<List<Content>> get() = _content

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> get() = _count

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _isEditable = MutableStateFlow(false)
    val isEditable: StateFlow<Boolean> get() = _isEditable

    private var scrollPosition = 0

    private var title = ""
    private var type = ""
    private var guid = ""

    fun setDocumentType(type: String?, title: String?) {
        this.type = type ?: ""
        this.title = title ?: ""
    }

    fun setDocumentId(id: String, onResult: (String, String) -> Unit) {
        if (this.guid == id) return
        this.guid = id
        onResult(type, guid)
    }

    fun getTitle(): String {
        return title
    }

    fun getType(): String {
        return type
    }

    fun getDocGuid(): String {
        return guid
    }

    fun onItemClicked(item: Content, openProduct: (Product) -> Unit) {
        if (!_isEditable.value) return
        val product = Product(
            id = item.code,
            code = item.code,
            description = item.description,
            unit = item.unit,
            notes = item.notes,
            contentItem = item,
        )
        openProduct(product)
    }

    fun enableEdit() {
        _isEditable.value = !_isEditable.value
    }

    fun requestEditLock(documentGuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                networkRepository.lockDocument(type, documentGuid)
            }
            if (result == "OK") {
                _isEditable.value = true
                onSuccess()
            } else {
                onError(result)
            }
        }
    }

    fun requestEditUnlock(documentGuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                networkRepository.unlockDocument(type, documentGuid)
            }
            if (result == "OK") {
                _isEditable.value = false
                onSuccess()
            } else {
                onError(result)
            }
        }
    }

    fun saveDocument(document: Document, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!_isEditable.value) {
            onError("Редагування не увімкнено")
            return
        }
        _isEditable.value = false
        viewModelScope.launch {
            _isLoading.value = true
            val msg = networkRepository.saveDocument(document.copy(
                type = type,
            ))
            if (msg == "OK") {
                onSuccess()
            } else {
                onError(msg)
            }
            _isLoading.value = false
        }
    }

    fun setDocumentContent(content: List<Content>, onResult: () -> Unit) {
        _content.value = content
        _count.value = content.size
        onResult()
    }

    fun deleteDocument() {
        //
    }

    fun onBarcodeRead(barcode: String, onResult: (Product) -> Unit) {
        if (!_isEditable.value) return
        viewModelScope.launch {
            _isLoading.value = true
            networkRepository.barcode(type, guid, barcode).collect { found ->
                // find product in content
                Log.d("RC_DocumentViewModel", "onBarcodeRead: code=${found.code} ${found.description}")
                val list = _content.value.toMutableList()
                val item = list.find { found.code == it.code }
                val product = found.copy(
                    contentItem = item
                )
                _isLoading.value = false
                onResult(product)
            }
        }
    }

    fun addProduct(barcode: String, onResult: (Product) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            networkRepository.barcode(type, guid, barcode).collect { found ->
                _isLoading.value = false
                onResult(found)
            }
        }
    }

    fun onListScrolled(position: Int) {
        scrollPosition = if (position > 0) position else 0
    }

    fun getScrollPosition(): Int {
        return scrollPosition
    }

}
