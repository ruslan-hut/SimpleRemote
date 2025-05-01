package ua.com.programmer.simpleremote.ui.document

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    private val _content = MutableLiveData<List<Content>>(emptyList())
    val content: LiveData<List<Content>> get() = _content

    private val _count = MutableLiveData<Int>(0)
    val count: LiveData<Int> get() = _count

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _isEditable = MutableLiveData<Boolean>(false)
    val isEditable: LiveData<Boolean> get() = _isEditable

    private var scrollPosition = 0

    private var title = ""
    private var type = ""
    private var guid = ""

    fun setDocumentType(type: String?, title: String?) {
        this.type = type ?: ""
        this.title = title ?: ""
    }

    fun setDocumentId(id: String) {
        if (_content.value?.isNotEmpty() == true) return
        this.guid = id
        loadDocumentContent()
    }

    fun getTitle(): String {
        return title
    }

    private fun loadDocumentContent() {
        viewModelScope.launch {
            _isLoading.value = true
            networkRepository.documentContent(type, guid).collect {
                _content.value = it
                _count.value = it.size
                _isLoading.value = false
            }
        }
    }

    fun onItemClicked(item: Content, openProduct: (Product) -> Unit) {
        if (_isEditable.value == false) return
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
        _isEditable.value = true
    }

    fun saveDocument(document: Document, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isEditable.value = false
        viewModelScope.launch {
            _isLoading.value = true
            val msg = networkRepository.saveDocument(document.copy(
                type = type,
            ))
            withContext(Dispatchers.Main) {
                if (msg == "OK") {
                    onSuccess()
                }else{
                    onError(msg)
                }
            }
            _isLoading.value = false
        }
    }

    fun refresh() {
        loadDocumentContent()
    }

    fun deleteDocument() {
        //
    }

    fun onBarcodeRead(barcode: String, onResult: (Product) -> Unit) {
        if (_isEditable.value == false) return
        viewModelScope.launch {
            _isLoading.value = true
            networkRepository.barcode(type, guid, barcode).collect { found ->
                // find product in content
                Log.d("RC_DocumentViewModel", "onBarcodeRead: code=${found.code} ${found.description}")
                val list = _content.value?.toMutableList() ?: mutableListOf()
                val item = list.find { found.code == it.code }
                val product = found.copy(
                    contentItem = item
                )
                _isLoading.value = false
                onResult(product)
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