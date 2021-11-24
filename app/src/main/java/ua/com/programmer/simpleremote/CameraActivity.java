package ua.com.programmer.simpleremote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ua.com.programmer.simpleremote.utility.Utils;

public class CameraActivity extends AppCompatActivity {

    private PreviewView cameraView;
    private ListenableFuture<ProcessCameraProvider> cameraProvider;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;

    private final Utils utils = new Utils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.header_camera);

        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraView = findViewById(R.id.camera_view);
        cameraProvider = ProcessCameraProvider.getInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }else {
                setupCamera();
            }
        }else {
            setupCamera();
        }

        TextView buttonRepeat = findViewById(R.id.button_repeat);
        buttonRepeat.setOnClickListener((View v) -> startCamera());

        TextView buttonConfirm = findViewById(R.id.button_confirm);
        buttonConfirm.setOnClickListener((View v) -> takePhoto());

    }

    private void setupCamera(){

        //View view = findViewById(R.id.camera_view);
        imageCapture = new ImageCapture.Builder()
                        //.setTargetRotation(view.getDisplay().getRotation())
                        .build();

        cameraProvider.addListener(() -> {
            try {
                ProcessCameraProvider provider = cameraProvider.get();
                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(cameraView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try{
                    provider.unbindAll();
                    provider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
                }catch (Exception e){
                    utils.error("provider.bindToLifecycle; "+e.getMessage());
                }

            } catch (Exception e) {
                utils.error("cameraProvider.addListener; " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void startCamera(){
        try {
            setupCamera();
        }catch (SecurityException se) {
            Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show();
            utils.error("start camera: "+e.getMessage());
        }
    }

    private void stopCamera(){
        try {
            cameraProvider.get().unbindAll();
        }catch (Exception e){
            utils.error("stop camera: "+e.getMessage());
        }
    }

    private void takePhoto(){

        ImageCapture.OutputFileOptions fileOptions = new ImageCapture.OutputFileOptions.Builder(
                new File("photo","tmp_picture.jpg")
        ).build();

        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,100);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);

        imageCapture.takePicture(fileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                utils.debug("image saved");
                stopCamera();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                utils.error("take picture: "+exception.getMessage());
            }
        });

    }
}