package ua.com.programmer.simpleremote.deprecated

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.repository.DataLoader
import ua.com.programmer.simpleremote.deprecated.specialItems.Cache
import ua.com.programmer.simpleremote.deprecated.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.deprecated.utility.BarcodeFoundListener
import ua.com.programmer.simpleremote.deprecated.utility.BarcodeImageAnalyzer
import ua.com.programmer.simpleremote.deprecated.utility.Utils
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity(), DataLoader.DataLoaderListener {
    private var cameraView: PreviewView? = null
    private var cameraProvider: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraExecutor: ExecutorService? = null

    private var textValue: TextView? = null
    private var textDescription: TextView? = null
    private var progressBar: ProgressBar? = null
    private var dataBaseItem: DataBaseItem? = null
    private var documentGUID: String? = null

    private val utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.header_scanner)

        val intent = getIntent()
        documentGUID = intent.getStringExtra("document")

        textValue = findViewById<TextView>(R.id.text_value)
        textDescription = findViewById<TextView>(R.id.text_item_description)
        textDescription!!.text = ""
        progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraView = findViewById<PreviewView>(R.id.camera_view)
        try {
            cameraProvider = ProcessCameraProvider.Companion.getInstance(this)
        } catch (e: Exception) {
            utils.debug("ScannerActivity: create camera provider: $e")
        }

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf<String>(Manifest.permission.CAMERA), 1)
        } else {
            setupCamera()
        }

        val buttonRepeat = findViewById<TextView>(R.id.button_repeat)
        buttonRepeat.setOnClickListener(View.OnClickListener { v: View? -> startCamera() })

        val buttonConfirm = findViewById<TextView>(R.id.button_confirm)
        buttonConfirm.setOnClickListener(View.OnClickListener { v: View? -> confirmAddItem() })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setupCamera() {
        if (cameraProvider == null) {
            Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val barcodeImageAnalyzer = BarcodeImageAnalyzer(object : BarcodeFoundListener {
            override fun onBarcodeFound(barCode: String?, format: Int) {
                onBarcodeReceived(barCode)
            }

            override fun onCodeNotFound(error: String?) {
                utils.debug("BarcodeImageAnalyzer onCodeNotFound; $error")
            }
        })

        cameraProvider!!.addListener(Runnable {
            try {
                val provider = cameraProvider!!.get()
                val preview = Preview.Builder()
                    .build()
                preview.surfaceProvider = cameraView!!.getSurfaceProvider()

                val imageAnalysis = ImageAnalysis.Builder()
                    .build()
                imageAnalysis.setAnalyzer(cameraExecutor!!, barcodeImageAnalyzer)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)
                } catch (e: Exception) {
                    utils.error("provider.bindToLifecycle; " + e.message)
                }
            } catch (e: Exception) {
                utils.error("cameraProvider.addListener; " + e.message)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera() {
        dataBaseItem = null
        textValue!!.text = ""
        textDescription!!.text = ""
        try {
            textValue!!.setText(R.string.scanning)
            setupCamera()
        } catch (_: SecurityException) {
            textValue!!.setText(R.string.error_no_permission)
        } catch (e: Exception) {
            textValue!!.text = e.toString()
        }
    }

    private fun stopCamera() {
        try {
            cameraProvider!!.get().unbindAll()
        } catch (e: Exception) {
            utils.debug("Unbind camera provider; " + e.message)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    override fun onDataLoaded(dataItems: java.util.ArrayList<DataBaseItem?>?) {
        progressBar!!.visibility = View.GONE
        val items = dataItems ?: ArrayList()
        if (items.isNotEmpty()) {
            dataBaseItem = items[0]
            textDescription!!.text = dataBaseItem!!.getString("description")
            textDescription!!.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
            confirmAddItem()
        } else {
            dataBaseItem = null
            textDescription!!.setText(R.string.warn_no_barcode)
            textDescription!!.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        }
    }

    override fun onDataLoaderError(error: String?) {
        progressBar!!.visibility = View.GONE
        Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show()
    }

    private fun onBarcodeReceived(barcodeValue: String?) {
        stopCamera()

        val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP)

        textValue!!.text = barcodeValue
        progressBar!!.visibility = View.VISIBLE

        val barcodeParameters = DataBaseItem()
        barcodeParameters.put("value", barcodeValue)
        barcodeParameters.put("guid", documentGUID)

        val dataLoader = DataLoader(this)
        dataLoader.getItemWithBarcode(barcodeParameters)
    }

    private fun confirmAddItem() {
        val intent = getIntent()
        if (dataBaseItem != null) {
            intent.putExtra("cacheKey", Cache.Companion.getInstance().put(dataBaseItem))
        } else {
            intent.putExtra("cacheKey", "")
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}