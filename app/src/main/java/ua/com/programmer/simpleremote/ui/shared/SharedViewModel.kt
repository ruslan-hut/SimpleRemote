package ua.com.programmer.simpleremote.ui.shared

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository
) : ViewModel() {

    private val _connection = MutableLiveData<ConnectionSettings>()
    val connection get() = _connection

    private val _document = MutableLiveData<Document>()
    val document get() = _document

    private val _product = MutableLiveData<Product>()
    val product get() = _product

    val barcode = MutableLiveData<String>()

    init {
        viewModelScope.launch {
            connectionRepo.currentConnection.collect {
                _connection.value = it ?: ConnectionSettings.Builder.buildDemo()
            }
        }
    }

    fun setDocument(doc: Document) {
        _document.value = doc
    }

    fun setProduct(prod: Product) {
        _product.value = prod
    }

    fun onBarcodeRead(value: String) {
        if (value.isBlank() || value.length < 10) return
        barcode.value = value
    }

    fun clearBarcode() {
        barcode.value = ""
    }
}