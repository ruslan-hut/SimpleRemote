package ua.com.programmer.simpleremote.ui.document

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
    ): ViewModel() {

    private val _content = MutableLiveData<List<Content>>(emptyList())
    val content: LiveData<List<Content>> get() = _content

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _isEditable = MutableLiveData<Boolean>(false)
    val isEditable: LiveData<Boolean> get() = _isEditable

    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> get() = _product

    var title = ""
    var type = ""
    var guid = ""

    fun setDocumentType(type: String?, title: String?) {
        this.type = type ?: ""
        this.title = title ?: ""
    }

    fun setDocumentId(id: String) {
        if (_content.value?.isNotEmpty() == true) return
        this.guid = id
        viewModelScope.launch {
            _isLoading.value = true
            networkRepository.documentContent(type, id).collect {
                _content.value = it
                _isLoading.value = false
            }
        }
    }

    fun onItemClicked(item: Content) {
        if (_isEditable.value == false) return
        val product = Product(
            id = item.code,
            code = item.code,
            description = item.description,
            unit = item.unit,
            contentItem = item
        )
        _product.value = product
    }

    fun resetProduct() {
        _product.value = null
    }

    fun enableEdit() {
        _isEditable.value = true
    }

    fun saveDocument() {
        _isEditable.value = false
    }

    fun refresh() {
        //
    }

    fun deleteDocument() {
        //
    }

    fun onBarcodeRead(barcode: String) {
        if (_isEditable.value == false) return
        viewModelScope.launch {
            _isLoading.value = true
            networkRepository.barcode(type, guid, barcode).collect { found ->
                // find product in content
                val item = _content.value?.find { found.code == it.code }
                val product = found.copy(
                    contentItem = item
                )
                _product.value = product
                _isLoading.value = false
            }
        }
    }

}