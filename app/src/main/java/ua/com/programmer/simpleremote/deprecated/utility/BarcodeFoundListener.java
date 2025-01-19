package ua.com.programmer.simpleremote.deprecated.utility;

public interface BarcodeFoundListener {
    void onBarcodeFound(String barCode, int format);
    void onCodeNotFound(String error);
}
