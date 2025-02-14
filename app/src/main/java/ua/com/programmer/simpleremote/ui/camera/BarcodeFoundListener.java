package ua.com.programmer.simpleremote.ui.camera;

public interface BarcodeFoundListener {
    void onBarcodeFound(String barCode, int format);
    void onCodeNotFound(String error);
}
