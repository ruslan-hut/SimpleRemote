package ua.com.programmer.simpleremote.ui.camera

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.lang.Exception

class BarcodeImageAnalyzer(barcodeFoundListener: BarcodeFoundListener) : ImageAnalysis.Analyzer {
    private val listener: BarcodeFoundListener = barcodeFoundListener
    private val scanner: BarcodeScanner

    init {
        val options =
            BarcodeScannerOptions.Builder() //.setBarcodeFormats(com.google.mlkit.vision.barcode.Barcode.FORMAT_EAN_13, com.google.mlkit.vision.barcode.Barcode.FORMAT_QR_CODE)
                .build()
        scanner = BarcodeScanning.getClient(options)
    }

    override fun analyze(imageProxy: ImageProxy) {
        @SuppressLint("UnsafeOptInUsageError") val mediaImage = imageProxy.getImage()
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            scanner.process(inputImage)
                .addOnSuccessListener(
                    OnSuccessListener { barcodes: MutableList<Barcode>? ->
                        val barcode = barcodes?.firstOrNull()
                        if (barcode != null) {
                            listener.onBarcodeFound(barcode.rawValue, barcode.format)
                        }
                    }
                )
                .addOnFailureListener(OnFailureListener { e: Exception? -> listener.onCodeNotFound(e!!.message) })
                .addOnCompleteListener(OnCompleteListener { task: Task<MutableList<Barcode>?>? ->
                    imageProxy.close()
                    mediaImage.close()
                })
        } else {
            imageProxy.close()
        }
    }
}
