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
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.utility.Utils;

public class CameraActivity extends AppCompatActivity {

    private PreviewView cameraView;
    private ListenableFuture<ProcessCameraProvider> cameraProvider;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;

    private DataBaseItem item;
    private File outputDirectory;
    private String imageFileName;

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

        Intent intent = getIntent();
        item = Cache.getInstance().get(intent.getStringExtra("cacheKey"));
        item.put("type", Constants.DOCUMENT_LINE);

        outputDirectory = this.getApplicationContext().getFilesDir();
        imageFileName = "";

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private void setupCamera(){

        //View view = findViewById(R.id.camera_view);
        imageCapture = new ImageCapture.Builder()
                        //.setTargetRotation(view.getDisplay().getRotation())
                        .setTargetResolution(new Size(768, 1024))
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
            utils.error("Camera: start: "+e.getMessage());
        }
    }

    private void stopCamera(){

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            try {
                cameraProvider.get().unbindAll();
                if (!imageFileName.isEmpty()){
                    saveAndExit();
                }
            }catch (Exception e){
                utils.error("Camera: stop: "+e.toString());
            }
        });

    }

    private void saveAndExit(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.save_photo_and_exit)
                .setTitle(R.string.action_save)
                .setPositiveButton(getResources().getString(R.string.action_save), (dialogInterface, i) -> {
                    item.put("image", imageFileName);
                    Intent intent = getIntent();
                    intent.putExtra("cacheKey", Cache.getInstance().put(item));
                    setResult(RESULT_OK, intent);
                    finish();
                })
                .setNegativeButton(getResources().getString(R.string.action_cancel), (dialogInterface, i) -> {
                    File image = new File(outputDirectory, imageFileName);
                    try {
                        if (image.exists()) {
                            if (image.delete()) utils.debug("Camera: file deleted: "+imageFileName);
                        }
                    }catch (Exception e){
                        utils.error("Camera: file delete: "+e.toString());
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void takePhoto(){

        imageFileName = UUID.randomUUID().toString()+".jpg";

        ImageCapture.OutputFileOptions fileOptions = new ImageCapture.OutputFileOptions.Builder(
                new File(outputDirectory,imageFileName)
        ).build();

        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,100);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);

        imageCapture.takePicture(fileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                utils.debug("Camera: image saved: "+imageFileName);
                stopCamera();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                imageFileName = "";
                utils.error("Camera: take picture: "+exception.getMessage());
            }
        });

    }
}