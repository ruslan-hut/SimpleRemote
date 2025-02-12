package ua.com.programmer.simpleremote.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.FragmentCameraBinding
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel
import java.io.File
import java.lang.Exception
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.getValue

@AndroidEntryPoint
class CameraFragment: Fragment() {

    private val viewModel: CameraViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var cameraView: PreviewView? = null
    private var imageCapture: ImageCapture? = null

    private var outputDirectory: File? = null
    private var imageFileName: String? = null

    private val cameraProvider: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(requireContext())
    }
    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Check camera permission
        if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }

        outputDirectory = requireContext().filesDir

        binding.buttonRepeat.setOnClickListener {
            startCamera()
        }
        binding.buttonConfirm.setOnClickListener {
            takePhoto()
        }
    }

    private fun setupCamera() {
        cameraView = binding.cameraView
        cameraProvider.addListener(Runnable {
            val cameraProvider = cameraProvider.get()
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            preview.surfaceProvider = cameraView?.surfaceProvider
            imageCapture = ImageCapture.Builder().build()
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startCamera() {
        try {
            setupCamera()
        } catch (se: SecurityException) {
            Log.e("RC_CameraFragment", "SecurityException: ${se.message}")
            Toast.makeText(requireContext(), R.string.error_no_permission, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("RC_CameraFragment", "Exception: ${e.message}")
            Toast.makeText(requireContext(), R.string.toast_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopCamera() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(Runnable {
            try {
                cameraProvider.get().unbindAll()
//                if (!imageFileName!!.isEmpty()) {
//                    saveAndExit()
//                }
            } catch (e: Exception) {
                Log.e("RC_CameraFragment", "stopCamera: $e")
            }
        })
    }

    private fun takePhoto() {
        imageFileName = UUID.randomUUID().toString() + ".jpg"

        val fileOptions = ImageCapture.OutputFileOptions.Builder(
            File(outputDirectory, imageFileName ?: "image.jpg")
        ).build()

        val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP)

        imageCapture!!.takePicture(fileOptions, cameraExecutor!!, object :
            ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d("RC_CameraFragment", "Image saved: ${outputFileResults.savedUri}")
                stopCamera()
            }

            override fun onError(exception: ImageCaptureException) {
                imageFileName = ""
                Log.e("RC_CameraFragment", "Image capture failed: ${exception.message}")
            }
        })
    }
}

