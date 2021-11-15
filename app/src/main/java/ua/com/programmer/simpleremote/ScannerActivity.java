package ua.com.programmer.simpleremote;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.utility.BarcodeFoundListener;
import ua.com.programmer.simpleremote.utility.BarcodeImageAnalyzer;
import ua.com.programmer.simpleremote.utility.Utils;

public class ScannerActivity extends AppCompatActivity implements DataLoader.DataLoaderListener{

    private PreviewView cameraView;
    private ListenableFuture<ProcessCameraProvider> cameraProvider;
    private ExecutorService cameraExecutor;

    private TextView textValue;
    private TextView textDescription;
    private ProgressBar progressBar;
    private DataBaseItem dataBaseItem;
    private String documentGUID;

    private final Utils utils = new Utils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.header_scanner);

        Intent intent = getIntent();
        documentGUID = intent.getStringExtra("document");

        textValue = findViewById(R.id.text_value);
        textDescription = findViewById(R.id.text_item_description);
        textDescription.setText("");
        progressBar = findViewById(R.id.progress_bar);

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
        buttonConfirm.setOnClickListener((View v) -> confirmAddItem());

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            finish();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupCamera(){
        if (cameraProvider == null) {
            //textView.setText(R.string.error_detector);
            return;
        }

        BarcodeImageAnalyzer barcodeImageAnalyzer = new BarcodeImageAnalyzer(new BarcodeFoundListener() {
            @Override
            public void onBarcodeFound(String barCode, int format) {
                onBarcodeReceived(barCode);
            }

            @Override
            public void onCodeNotFound(String error) {
                utils.debug("on code not found: "+error);
            }
        });

        cameraProvider.addListener(() -> {
            try {
                ProcessCameraProvider provider = cameraProvider.get();
                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(cameraView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, barcodeImageAnalyzer);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try{
                    provider.unbindAll();
                    provider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
                }catch (Exception e){
                    utils.debug("bind camera provider error; "+e.getMessage());
                }

            } catch (Exception e) {
                utils.debug("Error starting camera " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void startCamera(){
        dataBaseItem = null;
        textValue.setText("");
        textDescription.setText("");
        cameraProvider = null;
        try {
            textValue.setText(R.string.scanning);
            setupCamera();
        }catch (SecurityException se) {
            textValue.setText(R.string.error_no_permission);
        }catch (Exception e){
            textValue.setText(e.toString());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataLoaded(ArrayList<DataBaseItem> dataItems) {
        progressBar.setVisibility(View.GONE);
        if (dataItems.size() > 0) {
            dataBaseItem = dataItems.get(0);
            textDescription.setText(dataBaseItem.getString("description"));
            textDescription.setTextColor(ContextCompat.getColor(this,R.color.colorPrimaryDark));
            confirmAddItem();
        }else {
            dataBaseItem = null;
            textDescription.setText(R.string.warn_no_barcode);
            textDescription.setTextColor(ContextCompat.getColor(this,R.color.colorAccent));
        }

    }

    @Override
    public void onDataLoaderError(String error) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show();
    }

    private void onBarcodeReceived(String barcodeValue){
        textValue.setText(barcodeValue);
        progressBar.setVisibility(View.VISIBLE);

        DataBaseItem barcodeParameters = new DataBaseItem();
        barcodeParameters.put("value",barcodeValue);
        barcodeParameters.put("guid",documentGUID);

        DataLoader dataLoader = new DataLoader(this);
        dataLoader.getItemWithBarcode(barcodeParameters);
    }

    private void confirmAddItem(){
        Intent intent = getIntent();
        if (dataBaseItem != null) {
            intent.putExtra("cacheKey", Cache.getInstance().put(dataBaseItem));
        }else{
            intent.putExtra("cacheKey", "");
        }
        setResult(RESULT_OK, intent);
        finish();
    }
}
