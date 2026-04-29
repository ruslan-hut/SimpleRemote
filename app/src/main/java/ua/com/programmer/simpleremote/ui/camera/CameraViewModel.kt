package ua.com.programmer.simpleremote.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {

    data class ScanResult(val barcode: String, val product: Product)

    private val _scanMode = MutableStateFlow(false)
    val scanMode: StateFlow<Boolean> get() = _scanMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _scanResult = Channel<ScanResult>(Channel.BUFFERED)
    val scanResult: Flow<ScanResult> = _scanResult.receiveAsFlow()

    private val _scanPaused = MutableStateFlow(false)
    val scanPaused: StateFlow<Boolean> get() = _scanPaused

    fun pauseScan() {
        _scanPaused.value = true
    }

    fun resumeScan() {
        _scanPaused.value = false
    }

    private var _permissionGranted = false
    val permissionGranted get() = _permissionGranted

    private var _document: Document? = null

    @Volatile
    private var lookupInProgress = false

    fun setMode(mode: String?) {
        _scanMode.value = mode == "barcode"
    }

    fun setPermissionGranted(granted: Boolean) {
        _permissionGranted = granted
    }

    fun setDocument(document: Document) {
        _document = document
    }

    fun onBarcodeScanned(barcode: String?) {
        if (barcode.isNullOrEmpty() || _document == null) return
        if (_scanPaused.value) return
        if (lookupInProgress) return
        lookupInProgress = true
        _isLoading.value = true
        val type = _document?.type ?: ""
        val guid = _document?.guid ?: ""
        viewModelScope.launch {
            networkRepository.barcode(type, guid, barcode).collect { product ->
                _isLoading.value = false
                lookupInProgress = false
                _scanResult.trySend(ScanResult(barcode, product))
            }
        }
    }
}
