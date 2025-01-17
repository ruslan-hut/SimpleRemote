package ua.com.programmer.simpleremote

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import ua.com.programmer.simpleremote.settings.Constants
import ua.com.programmer.simpleremote.specialItems.Cache
import ua.com.programmer.simpleremote.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.utility.Utils
import java.io.File
import java.lang.Exception
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private var cameraView: PreviewView? = null
    private var cameraProvider: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraExecutor: ExecutorService? = null
    private var imageCapture: ImageCapture? = null

    private var item: DataBaseItem? = null
    private var outputDirectory: File? = null
    private var imageFileName: String? = null

    private val utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.header_camera)

        val intent = getIntent()
        item = Cache.getInstance().get(intent.getStringExtra("cacheKey"))
        item!!.put("type", Constants.DOCUMENT_LINE)

        outputDirectory = this.getApplicationContext().getFilesDir()
        imageFileName = ""

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraView = findViewById<PreviewView>(R.id.camera_view)
        cameraProvider = ProcessCameraProvider.getInstance(this)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf<String>(Manifest.permission.CAMERA), 1)
        } else {
            setupCamera()
        }

        val buttonRepeat = findViewById<TextView>(R.id.button_repeat)
        buttonRepeat.setOnClickListener(View.OnClickListener { v: View? -> startCamera() })

        val buttonConfirm = findViewById<TextView>(R.id.button_confirm)
        buttonConfirm.setOnClickListener(View.OnClickListener { v: View? -> takePhoto() })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    private fun setupCamera() {
        //View view = findViewById(R.id.camera_view);

        imageCapture = ImageCapture.Builder() //.setTargetRotation(view.getDisplay().getRotation())
            .setTargetResolution(Size(768, 1024))
            .build()

        cameraProvider!!.addListener(Runnable {
            try {
                val provider = cameraProvider!!.get()
                val preview = Preview.Builder()
                    .build()
                preview.setSurfaceProvider(cameraView!!.getSurfaceProvider())

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(this, cameraSelector, imageCapture, preview)
                } catch (e: Exception) {
                    utils.error("provider.bindToLifecycle; " + e.message)
                }
            } catch (e: Exception) {
                utils.error("cameraProvider.addListener; " + e.message)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera() {
        try {
            setupCamera()
        } catch (se: SecurityException) {
            Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show()
            utils.error("Camera: start: " + e.message)
        }
    }

    private fun stopCamera() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(Runnable {
            try {
                cameraProvider!!.get().unbindAll()
                if (!imageFileName!!.isEmpty()) {
                    saveAndExit()
                }
            } catch (e: Exception) {
                utils.error("Camera: stop: $e")
            }
        })
    }

    private fun saveAndExit() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.save_photo_and_exit)
            .setTitle(R.string.action_save)
            .setPositiveButton(
                getResources().getString(R.string.action_save),
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                    item!!.put("image", imageFileName)
                    val intent = getIntent()
                    intent.putExtra("cacheKey", Cache.getInstance().put(item))
                    setResult(RESULT_OK, intent)
                    finish()
                })
            .setNegativeButton(
                getResources().getString(R.string.action_cancel),
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                    val image = File(outputDirectory, imageFileName ?: "image.jpg")
                    try {
                        if (image.exists()) {
                            if (image.delete()) utils.debug("Camera: file deleted: $imageFileName")
                        }
                    } catch (e: Exception) {
                        utils.error("Camera: file delete: $e")
                    }
                })
        val dialog = builder.create()
        dialog.show()
    }

    private fun takePhoto() {
        imageFileName = UUID.randomUUID().toString() + ".jpg"

        val fileOptions = OutputFileOptions.Builder(
            File(outputDirectory, imageFileName ?: "image.jpg")
        ).build()

        val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP)

        imageCapture!!.takePicture(fileOptions, cameraExecutor!!, object : OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: OutputFileResults) {
                utils.debug("Camera: image saved: $imageFileName")
                stopCamera()
            }

            override fun onError(exception: ImageCaptureException) {
                imageFileName = ""
                utils.error("Camera: take picture: " + exception.message)
            }
        })
    }
}