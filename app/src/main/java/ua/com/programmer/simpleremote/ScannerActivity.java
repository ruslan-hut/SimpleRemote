package ua.com.programmer.simpleremote;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.ArrayList;

import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;

public class ScannerActivity extends AppCompatActivity implements DataLoader.DataLoaderListener{

    private BarcodeDetector detector;
    private CameraSource cameraSource;
    private SurfaceView cameraView;
    private TextView textValue;
    private TextView textDescription;
    private String barcodeValue="";
    private ProgressBar progressBar;
    private DataBaseItem dataBaseItem;
    private String documentGUID;

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

        cameraView = findViewById(R.id.camera_view);

        detector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        detector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(@NonNull Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0) {
                    textValue.post(() -> {
                            cameraSource.stop();

                            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,100);
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP);

                            barcodeValue = barcodes.valueAt(0).displayValue;
                            //barcodeFormatInt = barcodes.valueAt(0).format;

                            onBarcodeReceived();
                        });

                }
            }
        });

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
        if (!detector.isOperational()) {
            //textView.setText(R.string.error_detector);
            return;
        }

        CameraSource.Builder builder = new CameraSource.Builder(this, detector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                //.setRequestedPreviewSize(1600,1200)
                .setAutoFocusEnabled(true)
                .setRequestedFps(15.0f);

        cameraSource = builder.build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                startCamera();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

    }

    private void startCamera(){
        dataBaseItem = null;
        textValue.setText("");
        textDescription.setText("");
        try {
            textValue.setText(R.string.scanning);
            cameraSource.start(cameraView.getHolder());
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
            textDescription.setText(R.string.no_data);
            textDescription.setTextColor(ContextCompat.getColor(this,R.color.colorAccent));
        }

    }

    @Override
    public void onDataLoaderError(String error) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show();
    }

    private void onBarcodeReceived(){
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
