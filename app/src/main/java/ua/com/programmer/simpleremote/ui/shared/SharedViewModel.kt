package ua.com.programmer.simpleremote.ui.shared

import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.dao.entity.getAuthToken
import ua.com.programmer.simpleremote.dao.entity.getBaseImageUrl
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.UserOptions
import ua.com.programmer.simpleremote.entity.setImage
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository,
    private val networkRepository: NetworkRepository,
    private val imageLoader: ImageLoader,
) : ViewModel() {

    private val _connection = MutableLiveData<ConnectionSettings>()
    val connection get() = _connection

    private val _userOptions = MutableLiveData<UserOptions>()
    //val userOptions: UserOptions get() = _userOptions.value ?: UserOptions(isEmpty = true)

    private val _document = MutableLiveData<Document>()
    val document get() = _document

    private val _content = MutableLiveData<List<Content>>(emptyList())
    val content: LiveData<List<Content>> get() = _content

    private val _product = MutableLiveData<Product>()
    val product get() = _product

    val barcode = MutableLiveData<String>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            connectionRepo.checkAvailableConnection()
        }
        viewModelScope.launch {
            connectionRepo.currentConnection.collect {
                val conn = it ?: ConnectionSettings.buildDemo()
                withContext(Dispatchers.Main) {
                    _connection.value = conn
                }
                withContext(Dispatchers.IO) {
                    connectionRepo.updateUserData(conn)
                }
                imageLoader.setBaseImageURL(conn.getBaseImageUrl())
                imageLoader.setToken(conn.getAuthToken())
            }
        }
        viewModelScope.launch {
            networkRepository.userOptions.collect {
                _userOptions.value = it
                imageLoader.setLoadImages(it.loadImages)
            }
        }
    }

    fun setDocument(doc: Document) {
        _document.value = doc
    }

    fun setProduct(prod: Product) {
        _product.value = prod
    }

    fun setProductOnScan(product: Product) {
        _product.value = product
        if (collectMode()) {
            // find product in content an increase quantity
            val list = _content.value?.toMutableList() ?: mutableListOf()
            val item = list.find { it.code == product.code }
            item?.apply {
                collect = (collect.toIntOrNull() ?: 0).plus(1).toString()
                modified = true
                checked = (collect.toDoubleOrNull() ?: 0.0) >= (quantity.toDoubleOrNull() ?: 0.0)
            }
            _content.value = list
            checkContent()
        }
    }

    fun setDocumentContent(content: List<Content>) {
        _content.value = content
        checkContent()
    }

    fun setDocumentNotes(notes: String) {
        _document.value = _document.value?.copy(notes = notes)
    }

    fun setDocumentChecked(checked: Boolean) {
        _document.value = _document.value?.copy(checked = checked)
    }

    fun setItemChecked(code: String, isChecked: Boolean) {
        val originalList = _content.value ?: emptyList()
        val list = originalList.map { it.copy() }.toMutableList()
        val item = list.find { it.code == code }
        item?.apply {
            checked = isChecked
            modified = true
        }
        _content.value = list
        checkContent()
    }

    // Check if all content items are checked and update the document's checked status
    private fun checkContent() {
        val list = _content.value?.toMutableList() ?: mutableListOf()
        if (list.isEmpty()) return
        val checked = list.all { it.checked }
        _document.value = _document.value?.copy(checked = checked)
    }

    fun getDocument(): Document {
        return _document.value?.copy(
            lines = _content.value ?: emptyList()
        ) ?: Document()
    }

    fun onBarcodeRead(value: String) {
        if (value.isBlank() || value.length < 10) return
        barcode.value = value
    }

    fun confirmWithScan(): Boolean {
        return _userOptions.value?.confirmWithScan == true
    }

    fun collectMode(): Boolean {
        return _userOptions.value?.mode == "collect"
    }

    fun placementMode(): Boolean {
        return _userOptions.value?.mode == "placement"
    }

    fun clearBarcode() {
        barcode.value = ""
    }

    fun onImageCaptured(imagePath: String) {
        _product.value = _product.value?.setImage(imagePath)
    }

    fun loadImage(imageGUID: String, view: ImageView) {
        imageLoader.load(imageGUID, view)
    }

    fun loadLocalImage(file: String, view: ImageView) {
        imageLoader.loadFile(file, view)
    }
}