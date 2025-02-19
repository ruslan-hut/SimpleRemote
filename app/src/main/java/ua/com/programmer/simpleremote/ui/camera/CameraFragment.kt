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
import androidx.camera.core.ImageAnalysis
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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import javax.annotation.Nonnull
import kotlin.getValue

@AndroidEntryPoint
class CameraFragment: Fragment() {

    private val viewModel: CameraViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val navigationArgs: CameraFragmentArgs by navArgs()
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
        viewModel.setMode(navigationArgs.mode)
        viewModel.setPermissionGranted(checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        viewModel.setDocument(sharedViewModel.getDocument())
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
        if (!viewModel.permissionGranted) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }

        outputDirectory = requireContext().filesDir

        binding.buttonRepeat.setOnClickListener {
            resetCamera()
        }
        binding.buttonConfirm.setOnClickListener {
            takePhoto()
        }
        sharedViewModel.product.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.textItemDescription.text = it.description
                //binding.textValue.text = it.barcode
            }
        }
        viewModel.scanMode.observe(viewLifecycleOwner) {
            if (it) {
                binding.buttonConfirm.visibility = View.GONE
                binding.delimiter.visibility = View.GONE
                binding.textLines.visibility = View.GONE
            } else {
                binding.textLines.visibility = View.VISIBLE
            }
            setupCamera(it)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupCamera(scanMode: Boolean) {
        if (!viewModel.permissionGranted) {
            return
        }
        Log.d("RC_CameraFragment", "setupCamera: scan=$scanMode")
        cameraView = binding.cameraView
        cameraProvider.addListener(Runnable {
            val preview = Preview.Builder().build()
            preview.surfaceProvider = cameraView?.surfaceProvider
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val cameraProvider = cameraProvider.get()
            cameraProvider.unbindAll()

            if (scanMode) {
                val imageAnalysis = ImageAnalysis.Builder().build()
                imageAnalysis.setAnalyzer(
                    cameraExecutor,
                    BarcodeImageAnalyzer(object : BarcodeFoundListener {
                        override fun onBarcodeFound(barCode: String?, format: Int) {
                            val code = barCode ?: ""
                            makeBeep()
                            if (code.isNotEmpty()) {
                                sharedViewModel.onBarcodeRead(barCode ?: "")
                                stopCamera {
                                    findNavController().popBackStack()
                                }
                            }
                        }

                        override fun onCodeNotFound(error: String?) {
                            Log.d("RC_CameraFragment", "onCodeNotFound: $error")
                        }
                    })
                )
                try {
                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        imageAnalysis,
                        preview
                    )
                } catch (e: kotlin.Exception) {
                    Log.e("RC_CameraFragment", "image analysis: bind provider error: ${e.message}")
                }
            }else{
                imageCapture = ImageCapture.Builder().build()
                try {
                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: kotlin.Exception) {
                    Log.e("RC_CameraFragment", "image capture: bind provider error: ${e.message}")
                }
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun resetCamera() {
        stopCamera {
            try {
                setupCamera(viewModel.scanMode.value == true)
            } catch (se: SecurityException) {
                Log.e("RC_CameraFragment", "SecurityException: ${se.message}")
                Toast.makeText(requireContext(), R.string.error_no_permission, Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Log.e("RC_CameraFragment", "Exception: ${e.message}")
                Toast.makeText(requireContext(), R.string.toast_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopCamera(onStop: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        handler.post(Runnable {
            try {
                cameraProvider.get().unbindAll()
                onStop()
            } catch (e: Exception) {
                Log.e("RC_CameraFragment", "stopCamera: $e")
            }
        })
    }

    private fun makeBeep() {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP)
    }

    private fun takePhoto() {
        imageFileName = UUID.randomUUID().toString() + ".jpg"

        val fileOptions = ImageCapture.OutputFileOptions.Builder(
            File(outputDirectory, imageFileName ?: "image.jpg")
        ).build()

        makeBeep()

        imageCapture!!.takePicture(fileOptions, cameraExecutor, object :
            ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d("RC_CameraFragment", "Image saved: ${outputFileResults.savedUri}")
                stopCamera {
                    sharedViewModel.onImageCaptured(imageFileName ?: "")
                    findNavController().popBackStack()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                imageFileName = ""
                Log.e("RC_CameraFragment", "Image capture failed: ${exception.message}")
            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        @Nonnull permissions: Array<String>,
        @Nonnull grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.setPermissionGranted(true)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

