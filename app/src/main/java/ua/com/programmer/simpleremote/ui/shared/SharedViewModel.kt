package ua.com.programmer.simpleremote.ui.shared

import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.dao.entity.getAuthToken
import ua.com.programmer.simpleremote.dao.entity.getBaseImageUrl
import ua.com.programmer.simpleremote.entity.Catalog
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.UserOptions
import ua.com.programmer.simpleremote.entity.setImage
import ua.com.programmer.simpleremote.entity.toContent
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository,
    private val networkRepository: NetworkRepository,
    private val imageLoader: ImageLoader,
) : ViewModel() {

    private val _connection = MutableStateFlow<ConnectionSettings?>(null)
    val connection: StateFlow<ConnectionSettings?> get() = _connection

    private val _userOptions = MutableStateFlow<UserOptions?>(null)

    private val _document = MutableStateFlow<Document?>(null)
    val document: StateFlow<Document?> get() = _document

    private val _content = MutableStateFlow<List<Content>>(emptyList())
    val content: StateFlow<List<Content>> get() = _content

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> get() = _product

    private val _barcode = MutableStateFlow("")
    val barcode: StateFlow<String> get() = _barcode

    private val _selectedCatalogItem = MutableStateFlow<Catalog?>(null)
    val selectedCatalogItem: StateFlow<Catalog?> get() = _selectedCatalogItem

    fun setSelectedCatalogItem(catalog: Catalog) {
        _selectedCatalogItem.value = catalog
    }

    fun consumeSelectedCatalogItem(): Catalog? {
        val item = _selectedCatalogItem.value
        _selectedCatalogItem.value = null
        return item
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            connectionRepo.checkAvailableConnection()
        }
        viewModelScope.launch {
            connectionRepo.currentConnection.collect {
                val conn = it ?: ConnectionSettings.buildDemo()
                _connection.value = conn
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
            val list = _content.value.toMutableList()
            val index = list.indexOfFirst { it.code == product.code }
            if (index >= 0) {
                val item = list[index]
                val newCollect = (item.collect.toIntOrNull() ?: 0).plus(1).toString()
                list[index] = item.copy(
                    collect = newCollect,
                    modified = true,
                    checked = (newCollect.toDoubleOrNull() ?: 0.0) >= (item.quantity.toDoubleOrNull() ?: 0.0)
                )
            }
            _content.value = list
            checkContent()
        } else if (editMode()) {
            val list = _content.value.toMutableList()
            val index = list.indexOfFirst { it.code == product.code }
            if (index >= 0) {
                val item = list[index]
                val newCollect = (item.collect.toIntOrNull() ?: 0).plus(1).toString()
                list[index] = item.copy(
                    collect = newCollect,
                    modified = true,
                    checked = (newCollect.toDoubleOrNull() ?: 0.0) >= (item.quantity.toDoubleOrNull() ?: 0.0)
                )
            } else {
                val newContent = Content(
                    line = list.size + 1,
                    code = product.code,
                    code2 = product.barcode,
                    code3 = product.id,
                    art = product.art,
                    description = product.description,
                    unit = product.unit,
                    quantity = "1",
                    rest = product.rest.toString(),
                    price = product.price.toString(),
                    sum = product.price.toString(),
                    collect = "1",
                    notes = product.notes,
                    checked = false,
                    modified = true,
                    image = product.art,
                    encodedImage = "",
                    place = emptyList()
                )
                list.add(newContent)
            }
            _content.value = list
            checkContent()
        }
    }

    fun addProduct(catalog: Catalog, onResult: () -> Unit){
        if (!editMode()) return
        val list = _content.value.toMutableList()
        val index = list.indexOfFirst { it.code == catalog.code }
        if (index >= 0) {
            val item = list[index]
            val newCollect = (item.collect.toIntOrNull() ?: 0).plus(1).toString()
            list[index] = item.copy(
                collect = newCollect,
                modified = true,
                checked = (newCollect.toDoubleOrNull() ?: 0.0) >= (item.quantity.toDoubleOrNull() ?: 0.0)
            )
        } else {
            val newContent = catalog.toContent(list.size+1)
            list.add(newContent)
        }
        _content.value = list
        checkContent()
        onResult()
    }

    fun setDocumentContent(content: List<Content>) {
        setDocumentModified(true)
        _content.value = content
        checkContent()
    }

    fun setDocumentModified(modified: Boolean) {
        _document.value = _document.value?.copy(modified = modified)
    }


    private var loadContentJob: Job? = null

    fun loadDocumentContent(type:String, guid: String) {
        loadContentJob?.cancel()
        loadContentJob = viewModelScope.launch {
            networkRepository.documentContent(type, guid).collect {
                _content.value = it
            }
        }
    }

    fun setDocumentPlacesCollected(places: String) {
        _document.value = _document.value?.copy(placesCollected = places)
        setDocumentModified(true)
    }

    fun setDocumentNotes(notes: String) {
        _document.value = _document.value?.copy(notes = notes)
    }

    fun setDocumentChecked(checked: Boolean) {
        _document.value = _document.value?.copy(checked = checked)
        if (checked){
            setDocumentModified(true)
        }
    }

    fun setItemChecked(code: String, isChecked: Boolean) {
        val list = _content.value.toMutableList()
        val index = list.indexOfFirst { it.code == code }
        if (index >= 0) {
            list[index] = list[index].copy(checked = isChecked, modified = true)
        }
        _content.value = list
        checkContent()
        if (isChecked){
            setDocumentModified(true)
        }
    }

    fun checkContent() {
        val list = _content.value
        if (list.isEmpty()) return
        val checked = list.all { it.checked }
        _document.value = _document.value?.copy(checked = checked)
    }

    fun getDocument(): Document {
        return _document.value?.copy(
            lines = _content.value
        ) ?: Document()
    }

    fun onBarcodeRead(value: String) {
        if (value.isBlank() || value.length < 10) return
        _barcode.value = value
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

    fun editMode(): Boolean {
        val mode = _userOptions.value?.mode ?: ""
        return mode == "edit" || mode.isEmpty()
    }

    fun clearBarcode() {
        _barcode.value = ""
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
