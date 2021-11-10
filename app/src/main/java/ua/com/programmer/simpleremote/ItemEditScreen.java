package ua.com.programmer.simpleremote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;

public class ItemEditScreen extends AppCompatActivity {

    private DataBaseItem item;
    private EditText editQuantity;
    private EditText editNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_edit_screen);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.item_edit);

        Intent intent = getIntent();
        item = Cache.getInstance().get(intent.getStringExtra("cacheKey"));
        item.put("type", Constants.DOCUMENT_LINE);

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
        if (!enteredQuantity.isEmpty()) item.put("quantity", enteredQuantity);

        String enteredNotes = editNotes.getText().toString();
        item.put("notes", enteredNotes);

        Intent intent = getIntent();
        intent.putExtra("cacheKey", Cache.getInstance().put(item));
        setResult(RESULT_OK, intent);
        finish();
    }
}