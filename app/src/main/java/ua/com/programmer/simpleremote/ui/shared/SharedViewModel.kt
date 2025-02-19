package ua.com.programmer.simpleremote.ui.shared

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
    val userOptions: UserOptions get() = _userOptions.value ?: UserOptions(isEmpty = true)

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
                val conn = it ?: ConnectionSettings.Builder.buildDemo()
                withContext(Dispatchers.Main) {
                    _connection.value = conn
                }
                withContext(Dispatchers.IO) {
                    connectionRepo.updateUserData(conn)
                }
                imageLoader.setBaseImageURL(conn.getBaseImageUrl())
            }
        }
        viewModelScope.launch {
            networkRepository.userOptions.collect {
                _userOptions.value = it
                imageLoader.setLoadImages(it.loadImages)
                imageLoader.setToken(it.token)
            }
        }
    }

    fun setDocument(doc: Document) {
        _document.value = doc
    }

    fun setProduct(prod: Product) {
        _product.value = prod
    }

    fun setDocumentContent(content: List<Content>) {
        _content.value = content
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

    fun clearBarcode() {
        barcode.value = ""
    }

    fun onImageCaptured(imagePath: String) {
        _product.value = _product.value?.setImage(imagePath)
    }

    fun loadImage(imageGUID: String, view: ImageView) {
        imageLoader.load(imageGUID, view)
    }
}