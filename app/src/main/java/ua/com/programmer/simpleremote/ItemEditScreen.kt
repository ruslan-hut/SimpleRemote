package ua.com.programmer.simpleremote;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.utility.Utils;

public class ItemEditScreen extends AppCompatActivity {

    private DataBaseItem item;
    private EditText editQuantity;
    private EditText editNotes;
    private String attachImage;
    private File outputDirectory;
    private String workingMode;

    private final Utils utils = new Utils();

    private final ActivityResultLauncher<Intent> openCameraScreen = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            Intent data = result.getData();
            if (data == null) return;
            DataBaseItem dataBaseItem = Cache.getInstance().get(data.getStringExtra("cacheKey"));
            String newImage = dataBaseItem.getString("image");
            if (!newImage.isEmpty()){
                if (!attachImage.isEmpty()){
                    File file = new File(outputDirectory,attachImage);
                    if (file.exists()){
                        if (file.delete()) utils.debug("Item edit: File deleted: "+attachImage);
                    }
                }
                attachImage = newImage;
                item.put("image",newImage);
                item.put("encodedImage",encodeImage());
                showImage();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_edit_screen);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.item_edit);

        outputDirectory = this.getApplicationContext().getFilesDir();

        Intent intent = getIntent();
        item = Cache.getInstance().get(intent.getStringExtra("cacheKey"));
        item.put("type", Constants.DOCUMENT_LINE);
        workingMode = item.getString("workingMode");

        editQuantity = findViewById(R.id.edit_quantity);
        editQuantity.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> onEditTextAction(actionId));
        Window window = getWindow();
        if (window != null){
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        editQuantity.requestFocus();

        editNotes = findViewById(R.id.edit_notes);
        editNotes.setText(item.getString("notes"));

        setText(R.id.item_description, item.getString("description"));
        setText(R.id.item_code, item.getString("art"));
        setText(R.id.collect_edit, item.getString("quantity"));
        setText(R.id.rest_edit, item.getString("rest"));

        TextView buttonCancel = findViewById(R.id.button_cancel);
        buttonCancel.setOnClickListener((View v) -> onBackPressed());
        TextView buttonOK = findViewById(R.id.button_yes);
        buttonOK.setOnClickListener((View v) -> saveValuesAndExit());

        ImageView cameraButton = findViewById(R.id.camera_icon);
        cameraButton.setOnClickListener((View v) -> openCamera());

        attachImage = item.getString("image");
        showImage();
    }

    private void setText(int id, String text){
        TextView textView = findViewById(id);
        if (textView != null) textView.setText(text);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private boolean onEditTextAction(int actionId){
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT){
            saveValuesAndExit();
            return true;
        }
        return false;
    }

    private void saveValuesAndExit(){
        String enteredQuantity = editQuantity.getText().toString();
        if (!enteredQuantity.isEmpty()) {
            if (workingMode.equals(Constants.MODE_COLLECT)) {
                item.put("collect", enteredQuantity);
            }else{
                item.put("quantity", enteredQuantity);
            }
        }

        String enteredNotes = editNotes.getText().toString();
        item.put("notes", enteredNotes);

        Intent intent = getIntent();
        intent.putExtra("cacheKey", Cache.getInstance().put(item));
        setResult(RESULT_OK, intent);
        finish();
    }

    private void openCamera(){
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("cacheKey", Cache.getInstance().put(item));
        openCameraScreen.launch(intent);
    }

    private void showImage(){
        if (attachImage.isEmpty()) return;
        ImageView imageView = findViewById(R.id.item_image);
        Glide.with(this)
                .load(new File(outputDirectory,attachImage))
                .into(imageView);
    }

    private String encodeImage(){
        if (attachImage.isEmpty()) return "";
        String encodedImage = "";
        File image = new File(outputDirectory, attachImage);
        if (image.exists()){
            byte[] imageBytes = new byte[(int) image.length()];
            try {
                BufferedInputStream stream = new BufferedInputStream(new FileInputStream(image));
                if (stream.read(imageBytes, 0, imageBytes.length) == imageBytes.length){
                    encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                }
                stream.close();
            }catch (Exception e){
                utils.error("Item edit: file encode: "+e.toString());
            }
        }
        return encodedImage;
    }
}