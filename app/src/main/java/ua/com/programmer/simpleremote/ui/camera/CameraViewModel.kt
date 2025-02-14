package ua.com.programmer.simpleremote.ui.camera

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {

    private val _scanMode = MutableLiveData<Boolean>(false)
    val scanMode get() = _scanMode

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading get() = _isLoading

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