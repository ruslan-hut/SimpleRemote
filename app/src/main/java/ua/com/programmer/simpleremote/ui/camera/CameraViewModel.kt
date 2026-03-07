package ua.com.programmer.simpleremote.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {

    private val _scanMode = MutableStateFlow(false)
    val scanMode: StateFlow<Boolean> get() = _scanMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private var _permissionGranted = false
    val permissionGranted get() = _permissionGranted

    private var _document: Document? = null

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
        if (barcode.isNullOrEmpty() || _document == null) {
            return
        }
        _isLoading.value = true
        val type = _document?.type ?: ""
        val guid = _document?.guid ?: ""
        viewModelScope.launch {
            networkRepository.barcode(type, guid, barcode).collect { product ->
                _isLoading.value = false
            }
        }
    }
}
