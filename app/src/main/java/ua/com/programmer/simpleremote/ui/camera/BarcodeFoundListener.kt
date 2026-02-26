package ua.com.programmer.simpleremote.ui.camera

interface BarcodeFoundListener {
    fun onBarcodeFound(barCode: String?, format: Int)
    fun onCodeNotFound(error: String?)
}
