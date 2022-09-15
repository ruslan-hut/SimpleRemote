package ua.com.programmer.simpleremote;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Objects;

import ua.com.programmer.simpleremote.settings.AppSettings;
import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.specialItems.DocumentField;
import ua.com.programmer.simpleremote.utility.ImageLoader;
import ua.com.programmer.simpleremote.utility.Utils;

public class DocumentActivity extends AppCompatActivity implements DataLoader.DataLoaderListener {

    private final Utils utils = new Utils();
    private final Cache cache = Cache.getInstance();

    private ContentAdapter contentAdapter;
    private RecyclerView recyclerView;
    private boolean isEditable = false;
    private boolean isModified = false;
    private boolean loadImages;
    private ProgressBar progressBar;
    private DataBaseItem documentDataItem;
    private SqliteDB database;
    private ImageLoader imageLoader;

    private String documentGUID;
    private String documentDataString="";
    private boolean isCachedDocument;
    private String barcode="";
    private boolean checkedFlagEnabled;
    private String currency;
    private String workingMode;

    private DocumentField field1;
    private DocumentField field2;
    private DocumentField field3;
    private DocumentField field4;

    private final ActivityResultLauncher<Intent> openNextScreen = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (data == null) return;

                DataBaseItem dataBaseItem = cache.get(data.getStringExtra("cacheKey"));
                String itemCode = dataBaseItem.getString("code");
                String type = dataBaseItem.getString("type");
                String value = dataBaseItem.getString("description");

                if (!itemCode.isEmpty() && !type.isEmpty()) {

                    if (type.equals(field1.type)) {

                        field1.code = itemCode;
                        field1.value = value;
                        documentDataItem.put("field1", field1.asString());

                    }else if (type.equals(field2.type)) {

                        field2.code = itemCode;
                        field2.value = value;
                        documentDataItem.put("field2", field2.asString());

                    }else if (type.equals(Constants.DOCUMENT_LINE)){

                        contentAdapter.setItemProperties(dataBaseItem);
//                                dataBaseItem.getString("quantity"),
//                                dataBaseItem.getString("price"),
//                                dataBaseItem.getBoolean("checked"));
                        recyclerView.scrollToPosition(contentAdapter.getPosition(dataBaseItem));

                        if (!contentAdapter.hasUncheckedItems()) documentDataItem.put("checked", true);

                    }else if (type.equals(Constants.GOODS)){
                        addGoodsItem(dataBaseItem);
                    }

                    showDocumentHeader();

                } else {
                    Toast.makeText(this, R.string.no_data, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_coordinator);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.document);

        AppSettings appSettings = AppSettings.getInstance(this);

        database = SqliteDB.getInstance(this);
        loadImages = appSettings.isLoadImages();
        workingMode = appSettings.getWorkingMode();
        imageLoader = new ImageLoader(this);
        progressBar = findViewById(R.id.progress_bar);

        Intent intent = getIntent();
        documentDataItem = cache.get(intent.getStringExtra("cacheKey"));
        documentGUID = documentDataItem.getString("guid");
        isCachedDocument = documentDataItem.hasValue(Constants.CACHE_GUID);
        checkedFlagEnabled = documentDataItem.hasValue("checked");
        currency = documentDataItem.getString("currency");

        recyclerView = findViewById(R.id.document_content);
        //recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        contentAdapter = new ContentAdapter();
        recyclerView.setAdapter(contentAdapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT,ItemTouchHelper.RIGHT){
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (contentAdapter.getItemViewType(viewHolder.getBindingAdapterPosition()) == RecyclerView.NO_POSITION){
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                contentAdapter.onItemDismiss(viewHolder.getBindingAdapterPosition());
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return isEditable;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        };
        if (workingMode.equals(Constants.MODE_FULL)){
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }

        showDocumentHeader();
        updateContent();
        setEditableMode(false);
    }

    private String getDocumentTitle(){
        String type = documentDataItem.getString(Constants.TYPE);
        ArrayList<DataBaseItem> allowedDocuments = AppSettings.getAllowedDocuments();
        for (DataBaseItem doc: allowedDocuments){
            if (doc.getString("code").equals(type)){
                return doc.getString("description");
            }
        }
        return getString(R.string.document);
    }

    private void showDocumentHeader(){
        TextView textTitle = findViewById(R.id.document_title);
        textTitle.setText(getDocumentTitle());
        TextView textNumber = findViewById(R.id.document_number);
        textNumber.setText(documentDataItem.getString(Constants.DOCUMENT_NUMBER));
        TextView textDate = findViewById(R.id.document_date);
        textDate.setText(documentDataItem.getString(Constants.DOCUMENT_DATE));

        documentGUID = documentDataItem.getString(Constants.GUID);

        ImageView documentIcon = findViewById(R.id.document_icon);
        if (documentGUID.contains("!")) {
            documentIcon.setImageResource(R.drawable.sharp_help_outline_24);
            setEditableMode(true);
        }else if (isCachedDocument){
            documentIcon.setImageResource(R.drawable.sharp_help_outline_24);
        }else if (documentDataItem.getInt(Constants.DOCUMENT_IS_DELETED) == 1) {
            documentIcon.setImageResource(R.drawable.twotone_close_24);
        }else if (documentDataItem.getInt(Constants.DOCUMENT_IS_PROCESSED) == 1) {
            documentIcon.setImageResource(R.drawable.twotone_check_box_24);
        }else {
            documentIcon.setImageResource(R.drawable.twotone_check_box_outline_blank_24);
        }

        LinearLayout contractorLine = findViewById(R.id.document_header_contractor);
        String contractor = documentDataItem.getString("contractor");
        if (contractor.isEmpty()) {
            contractorLine.setVisibility(View.GONE);
        }else{
            contractorLine.setVisibility(View.VISIBLE);
            TextView tvContractor = findViewById(R.id.document_contractor);
            tvContractor.setText(contractor);
        }

        field1 = new DocumentField(documentDataItem.getString("field1"));
        field2 = new DocumentField(documentDataItem.getString("field2"));
        field3 = new DocumentField(documentDataItem.getString("field3"));
        field4 = new DocumentField(documentDataItem.getString("field4"));

        LinearLayout layoutField1 = findViewById(R.id.document_header_field1);
        if (field1.isReal()) {
            layoutField1.setVisibility(View.VISIBLE);
            TextView field1Name = findViewById(R.id.document_field1_name);
            field1Name.setText(field1.description);
            TextView field1Value = findViewById(R.id.document_field1_value);
            field1Value.setText(field1.value);
            layoutField1.setOnClickListener((View v) -> onSpecialFieldClick("field1",field1));
        } else {
            layoutField1.setVisibility(View.GONE);
        }

        LinearLayout layoutField2 = findViewById(R.id.document_header_field2);
        if (field2.isReal()) {
            layoutField2.setVisibility(View.VISIBLE);
            TextView field2Name = findViewById(R.id.document_field2_name);
            field2Name.setText(field2.description);
            TextView field2Value = findViewById(R.id.document_field2_value);
            field2Value.setText(field2.value);
            layoutField2.setOnClickListener((View v) -> onSpecialFieldClick("field2",field2));
        } else {
            layoutField2.setVisibility(View.GONE);
        }

        LinearLayout layoutField3 = findViewById(R.id.document_header_field3);
        if (field3.isReal()) {
            layoutField3.setVisibility(View.VISIBLE);
            TextView field3Name = findViewById(R.id.document_field3_name);
            field3Name.setText(field3.description);
            TextView field3Value = findViewById(R.id.document_field3_value);
            field3Value.setText(field3.value);
            layoutField3.setOnClickListener((View v) -> onSpecialFieldClick("field3",field3));
        } else {
            layoutField3.setVisibility(View.GONE);
        }

        LinearLayout layoutField4 = findViewById(R.id.document_header_field4);
        if (field4.isReal()) {
            layoutField4.setVisibility(View.VISIBLE);
            TextView field4Name = findViewById(R.id.document_field4_name);
            field4Name.setText(field4.description);
            TextView field4Value = findViewById(R.id.document_field4_value);
            field4Value.setText(field4.value);
            layoutField4.setOnClickListener((View v) -> onSpecialFieldClick("field4",field4));
        } else {
            layoutField4.setVisibility(View.GONE);
        }

        LinearLayout documentCheckedLine = findViewById(R.id.document_header_checked);
        if (checkedFlagEnabled) {
            documentCheckedLine.setVisibility(View.VISIBLE);
            CheckBox documentIsChecked = findViewById(R.id.document_is_checked);
            documentIsChecked.setChecked(documentDataItem.getBoolean("checked"));
            documentIsChecked.setOnClickListener((View v) -> {
                documentDataItem.put("checked", documentIsChecked.isChecked());
                isModified = true;
            });
        }else{
            documentCheckedLine.setVisibility(View.GONE);
        }

        TextView textViewNotes = findViewById(R.id.document_header_notes);
        String notes = documentDataItem.getString("notes");
        if (notes.equals("")) {
            textViewNotes.setHint(R.string.notes);
        }else {
            textViewNotes.setText(notes);
        }
        textViewNotes.setOnClickListener((View v) -> openTextEditDialog("notes",null));
    }

    private void openTextEditDialog (String fieldName, @Nullable DocumentField field) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_text,null);

        final EditText editText = view.findViewById(R.id.edit_text);
        String title;
        if (field != null) {
            editText.setText(field.value);
            title = field.description;
        }else {
            editText.setText(documentDataItem.getString(fieldName));
            title = getResources().getString(R.string.notes);
        }

        builder.setView(view);
        builder.setMessage("")
                .setTitle(title)
                .setPositiveButton(getResources().getString(R.string.action_save), (dialogInterface, i) -> {
                    if (field != null) {
                        field.value = editText.getText().toString();
                        documentDataItem.put(fieldName,field.asString());
                    }else {
                        documentDataItem.put(fieldName, editText.getText().toString());
                    }
                    showDocumentHeader();
                })
                .setNegativeButton(getResources().getString(R.string.action_cancel), null);
        final AlertDialog dialog = builder.create();
        try {
            Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }catch (Exception ex){
            utils.log("e","Set soft input mode: "+ex);
        }
        dialog.show();
    }

    private void onSaveError(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (!documentDataString.equals("")) {
            database.cacheData(documentGUID, documentDataString);
            builder.setMessage(R.string.saved_in_cache_warn)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.ok, (DialogInterface di, int i) ->
                            continueOnBackPressed())
                    .create()
                    .show();
        }else {
            builder.setMessage(R.string.save_error_warn)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.ok, null)
                    .create()
                    .show();
        }

    }

    private void showMessage(String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    private void saveDocument(){

        if (contentAdapter.hasUncheckedItems()){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.warn_document_has_unchecked_items)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.ok, null)
                    .create()
                    .show();
        }

        documentDataString="";
        JSONObject document = new JSONObject();
        try {
            document.put(Constants.DOCUMENT_NUMBER, documentDataItem.getString(Constants.DOCUMENT_NUMBER));
            document.put(Constants.TYPE, documentDataItem.getString(Constants.TYPE));
            document.put(Constants.GUID, documentDataItem.getString(Constants.GUID));

            //extra data fo cached document
            document.put(Constants.DOCUMENT_DATE, documentDataItem.getString(Constants.DOCUMENT_DATE));
            document.put("company", documentDataItem.getString("company"));
            document.put("field1",field1.asString());
            document.put("field2",field2.asString());
            document.put("field3",field3.asString());
            document.put("field4",field4.asString());
            document.put("notes", documentDataItem.getString("notes"));
            document.put("sum", documentDataItem.getString("sum"));
            document.put("checked", documentDataItem.getBoolean("checked"));

            JSONArray lines = new JSONArray();
            ArrayList<DataBaseItem> items = contentAdapter.getListItems();
            for (DataBaseItem item: items){
                lines.put(item.getAsJSON());
            }
            document.put("lines", lines);
            documentDataString = document.toString();
        }catch (Exception ex){
            utils.error("Save document: "+ex);
            Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        DataLoader dataLoader = new DataLoader(this);
        dataLoader.saveDocument(documentDataString);
    }

    @Override
    public void onDataLoaded(ArrayList<DataBaseItem> dataItems) {
        progressBar.setVisibility(View.INVISIBLE);

        //received special data item or document content ??
        boolean isSpecial = false;
        if (dataItems.size() == 1){
            DataBaseItem item = dataItems.get(0);
            isSpecial = item.getInt("line") == 0;
        }

        if (isSpecial){

            DataBaseItem item = dataItems.get(0);
            String savedFlag = item.getString("saved");
            String type = item.getString(Constants.TYPE);

            //received goods item by barcode scanner
            if (type.equals(Constants.GOODS)) {
                addGoodsItem(item);
            }else if (type.equals(Constants.MESSAGE)){
                showMessage(item.getString("text"));
            }else if (!item.getString(Constants.DOCUMENT_NUMBER).isEmpty()){
                documentDataItem = item;
                showDocumentHeader();
                //dataItems.clear();
            }

            if (savedFlag.equals("ok")) {
                isModified = false;
                Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();

                //delete cached document if any
                database.deleteCachedData(documentGUID);

                //if document is loaded from cache, exit
                if (isCachedDocument) {
                    continueOnBackPressed();
                }else {
                    updateContent();
                }

            }else if (savedFlag.length() > 0){
                Toast.makeText(this, getResources().getString(R.string.toast_error)+" "+item.getString("error"), Toast.LENGTH_LONG).show();
            }

        } else if (dataItems.size() > 0){
            contentAdapter.loadListItems(dataItems);
        }else{
            Toast.makeText(this, R.string.no_data, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDataLoaderError(String error) {
        progressBar.setVisibility(View.INVISIBLE);
        if (contentAdapter.hasEditedItems()) {
            onSaveError();
        }else {
            Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        //updateContent();
        super.onResume();
    }

    void continueOnBackPressed(){
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (contentAdapter.hasEditedItems() || isModified) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.unsaved_document_warn)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.yes, (DialogInterface di, int i) ->
                            continueOnBackPressed())
                    .setNegativeButton(R.string.action_cancel, null)
                    .create()
                    .show();
        }else {
            continueOnBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) onBackPressed();
        if (id == R.id.edit_document) setEditableMode(!isEditable);
        if (id == R.id.save_document) saveDocument();
        if (id == R.id.refresh) checkAndUpdateContent();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void addGoodsItem(DataBaseItem dataBaseItem){
        if (!contentAdapter.contains(dataBaseItem)){
            contentAdapter.addItem(dataBaseItem);
            //recyclerView.scrollToPosition(contentAdapter.getItemCount()-1);
        }
        DataBaseItem listItem = contentAdapter.findItem(dataBaseItem);
        if (listItem != null){
            //onListItemClick(listItem);

            if (workingMode.equals(Constants.MODE_COLLECT)) {
                listItem.put("collect", listItem.getDouble("collect") + 1);
            }else{
                listItem.put("quantity", listItem.getDouble("quantity") + 1);
            }
            contentAdapter.setItemProperties(listItem);
            recyclerView.scrollToPosition(contentAdapter.getPosition(listItem));

        }

    }

    private void updateContent(){
        progressBar.setVisibility(View.VISIBLE);
        DataLoader dataLoader = new DataLoader(this);
        dataLoader.getDocumentContent(documentDataItem);
    }

    private void checkAndUpdateContent(){
        if (contentAdapter.hasEditedItems()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.unsaved_document_warn)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.yes, (DialogInterface di, int i) ->
                            updateContent())
                    .setNegativeButton(R.string.action_cancel, null)
                    .create()
                    .show();
        }else{
            updateContent();
        }
    }

    private void openScanner(){
        Intent intent = new Intent(this, ScannerActivity.class);
        intent.putExtra("document",documentGUID);
        openNextScreen.launch(intent);
    }

    private void setEditableMode(Boolean newMode){
        isEditable = newMode;
        View bottomBar  = findViewById(R.id.bottom_bar);
        TextView addItemButton = findViewById(R.id.add_item_button);
        TextView scannerButton = findViewById(R.id.scanner_button);
        if (isEditable) {
            bottomBar.setVisibility(View.VISIBLE);
            scannerButton.setOnClickListener((View v) -> openScanner());
            if (workingMode.equals(Constants.MODE_COLLECT)) {
                addItemButton.setVisibility(View.GONE);
            }else{
                addItemButton.setOnClickListener((View v) -> onAddButtonClick());
            }
        }else {
            bottomBar.setVisibility(View.GONE);
        }
    }

    private void openItemEditDialog(DataBaseItem item){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.document_line_edit_dialog,null);

        String restText = item.getString("rest");
        if (restText.equals("")) {
            LinearLayout restLine = view.findViewById(R.id.rest_line);
            restLine.setVisibility(View.INVISIBLE);
        }else {
            final TextView rest = view.findViewById(R.id.rest);
            rest.setText(utils.format(item.getDouble("rest"), 3));
        }

        final EditText editQuantity = view.findViewById(R.id.edit_quantity);
        editQuantity.setHint(item.getString("quantity"));

        final EditText editPrice = view.findViewById(R.id.edit_price);
        editPrice.setHint(item.getString("price"));
        //editPrice.setEnabled(false);

        final CheckBox checkedCheckBox = view.findViewById(R.id.checked_box);
        if (checkedFlagEnabled) {
            //checkedCheckBox.setVisibility(View.VISIBLE);
            checkedCheckBox.setChecked(item.getBoolean("checked"));
        }else{
            LinearLayout checkedLine = view.findViewById(R.id.checked_line);
            checkedLine.setVisibility(View.GONE);
        }

        builder.setView(view);
        builder.setMessage(item.getString("art"))
                .setTitle(item.getString("description"))
                .setPositiveButton(R.string.action_save, (DialogInterface dialogInterface, int i) ->
                {
                    String enteredQuantity = editQuantity.getText().toString();
                    if (!enteredQuantity.isEmpty()) {
                        item.put("quantity", utils.round(enteredQuantity, 3));
                    }
                    String enteredPrice = editPrice.getText().toString();
                    if (enteredPrice.isEmpty()) {
                        item.put("price", utils.round(enteredPrice, 2));
                    }
                    item.put("checked", checkedCheckBox.isChecked());
                    contentAdapter.setItemProperties(item);
                    recyclerView.scrollToPosition(contentAdapter.getPosition(item));
                })
                .setNegativeButton(R.string.action_cancel, (DialogInterface dialog, int i) -> {});
        final AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null){
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
        editQuantity.requestFocus();

        editQuantity.setOnEditorActionListener((TextView textView, int i, KeyEvent keyEvent) ->
        {
            if (i == EditorInfo.IME_ACTION_NEXT || i == EditorInfo.IME_ACTION_DONE) {
                String enteredQuantity = editQuantity.getText().toString();
                if (!enteredQuantity.isEmpty()) {
                    item.put("quantity", utils.round(enteredQuantity, 3));
                }
                String enteredPrice = editPrice.getText().toString();
                if (enteredPrice.isEmpty()) {
                    item.put("price", utils.round(enteredPrice, 2));
                }
                item.put("checked", checkedCheckBox.isChecked());
                contentAdapter.setItemProperties(item);
                recyclerView.scrollToPosition(contentAdapter.getPosition(item));
                dialog.dismiss();
            }
            return false;
        });
    }

    private void  onListItemClick(DataBaseItem item){
        if (!isEditable){
            return;
        }
        item.put("workingMode", workingMode);

        if (workingMode.equals(Constants.MODE_COLLECT)) {

            Intent intent = new Intent(this, ItemEditScreen.class);
            intent.putExtra("cacheKey", Cache.getInstance().put(item));
            openNextScreen.launch(intent);

        }else{

            openItemEditDialog(item);

        }

    }

    private void onAddButtonClick(){
        if (isEditable){
            Intent intent = new Intent(this, CatalogListActivity.class);
            intent.putExtra("catalogType", Constants.GOODS);
            intent.putExtra("itemSelectionMode", true);
            intent.putExtra("documentGUID",documentGUID);
            openNextScreen.launch(intent);
        }
    }

    private void onSpecialFieldClick(String fieldName, DocumentField specialField){
        if (isEditable){
            if (specialField.isCatalog()) {
                Intent intent = new Intent(this, CatalogListActivity.class);
                intent.putExtra("catalogType", specialField.type);
                intent.putExtra("itemSelectionMode", true);
                intent.putExtra("documentGUID", documentGUID);
                openNextScreen.launch(intent);
            }else{
                openTextEditDialog(fieldName,specialField);
            }
            isModified = true;
        }
    }

    private void onBarcodeReceived() {
        if (barcode.length() > 0) {
            progressBar.setVisibility(View.VISIBLE);

            DataBaseItem barcodeParameters = new DataBaseItem();
            barcodeParameters.put("value",barcode);
            barcodeParameters.put("guid",documentGUID);

            DataLoader dataLoader = new DataLoader(this);
            dataLoader.getItemWithBarcode(barcodeParameters);
        }
        barcode = "";
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP){
            return true;
        }
        int keyCode = event.getKeyCode();
        //utils.debug("KEY: "+keyCode+"; "+barcode);
        if (keyCode == KeyEvent.KEYCODE_BACK){
            onBackPressed();
            return true;
        }
        if (!isEditable) {
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB) {
            onBarcodeReceived();
        }else{
            char key = (char) event.getUnicodeChar();
            if (Character.isDigit(key) || Character.isLetter(key)) {
                barcode += key;
            }else{
                barcode = "";
            }
        }
        return true;
    }

    class ContentViewHolder extends RecyclerView.ViewHolder{

        TextView tvCode;
        TextView tvCode2;
        TextView tvCode3;
        TextView tvDescription;
        TextView tvLineNumber;
        TextView tvQuantity;
        TextView tvRest;
        TextView tvCollect;
        LinearLayout tvRestTitle;
        LinearLayout descriptionLine;
        TextView tvUnit;
        TextView tvPrice;
        TextView tvSum;
        TextView tvNotes;
        CardView cardView;
        ImageView iconStar;
        ImageView image;
        CheckBox isChecked;

        ContentViewHolder(View view){
            super(view);
            cardView = view.findViewById(R.id.item_card);
            tvCode = view.findViewById(R.id.item_code);
            tvCode2 = view.findViewById(R.id.item_code2);
            tvCode3 = view.findViewById(R.id.item_code3);
            tvDescription = view.findViewById(R.id.item_description);
            tvLineNumber = view.findViewById(R.id.item_line_number);
            tvNotes = view.findViewById(R.id.item_notes);
            tvQuantity = view.findViewById(R.id.item_quantity);
            tvRest = view.findViewById(R.id.item_rest);
            tvCollect = view.findViewById(R.id.item_collect);
            tvRestTitle = view.findViewById(R.id.item_rest_title);
            tvUnit = view.findViewById(R.id.item_unit);
            tvPrice = view.findViewById(R.id.item_price);
            tvSum = view.findViewById(R.id.item_sum);
            iconStar = view.findViewById(R.id.icon_star);
            isChecked = view.findViewById(R.id.is_checked);
            image = view.findViewById(R.id.item_image);
            descriptionLine = view.findViewById(R.id.description_line);
        }

        void setCode(String str) {
            this.tvCode.setText(str);
        }

        void setCode2(String str) {
            this.tvCode2.setText(str);
            if (str.isEmpty()) tvCode2.setVisibility(View.GONE);
        }

        void setCode3(String str) {
            this.tvCode3.setText(str);
            if (str.isEmpty()) tvCode3.setVisibility(View.GONE);
        }

        void setDescription(String str) {
            this.tvDescription.setText(str);
        }

        void setNotes(String str) {
            this.tvNotes.setText(str);
            if (str.isEmpty()) tvNotes.setVisibility(View.GONE);
        }

        void  setRest(String str,String unit) {
            double rest = utils.round(str, 3);
            if (rest == 0.0) {
                tvRestTitle.setVisibility(View.INVISIBLE);
                tvRest.setVisibility(View.INVISIBLE);
            }else {
                String qtyText = utils.formatAsInteger(rest)+" "+unit;
                tvRestTitle.setVisibility(View.VISIBLE);
                tvRest.setVisibility(View.VISIBLE);
                tvRest.setText(qtyText);
            }
        }

        void setQuantity(String str) {
            str = utils.formatAsInteger(str);
            this.tvQuantity.setText(str);
        }

        void setCollect(String str) {
            str = utils.formatAsInteger(str);
            if (str.equals("0")) str = "--";
            this.tvCollect.setText(str);
        }

        void setUnit(String str) {
            this.tvUnit.setText(str);
        }

        void setPrice(String str) {
            str = str+" "+currency;
            this.tvPrice.setText(str);
        }

        void setSum(String str) {
            str = "= "+str+" "+currency;
            this.tvSum.setText(str);
        }

        void setLineNumber(String lineNumber){
            if (tvLineNumber != null){
                tvLineNumber.setText(lineNumber);
            }
        }

        void setStared(boolean isStared){
            if (isStared) {
                iconStar.setVisibility(View.VISIBLE);
            }else {
                iconStar.setVisibility(View.INVISIBLE);
            }
        }

        void setChecked(boolean checkedFlag){
            if (checkedFlagEnabled) {
                isChecked.setVisibility(View.VISIBLE);
                isChecked.setChecked(checkedFlag);
            }else{
                isChecked.setVisibility(View.GONE);
            }
        }

        void showImage(String code){
            if (loadImages) {
                image.setVisibility(View.VISIBLE);
                imageLoader.load(code,image);
            }else{
                image.setVisibility(View.GONE);
            }
        }
    }

    class ContentAdapter extends RecyclerView.Adapter<ContentViewHolder>{

        private final ArrayList<DataBaseItem> listItems = new ArrayList<>();
        private final int colorRed = getResources().getColor(R.color.backgroundRed);
        private final int colorYellow = getResources().getColor(R.color.backgroundYellow);
        private final int colorWhite = getResources().getColor(R.color.colorWhite);

        @SuppressLint("NotifyDataSetChanged")
        void loadListItems(ArrayList<DataBaseItem> values){
            listItems.clear();
            listItems.addAll(values);
            notifyDataSetChanged();
        }

        void addItem(DataBaseItem item){
            listItems.add(item);
            notifyItemInserted(listItems.size()-1);
            //notifyDataSetChanged();
        }

        void setItemProperties(DataBaseItem item){
            //utils.debug(item.getAsJSON().toString());
            double qty = utils.round(item.getString("quantity"),3);
            item.put("edited", 1);

            if (workingMode.equals(Constants.MODE_COLLECT)) {
                double collect = utils.round(item.getString("collect"), 3);
                item.put("checked", collect <= qty);
            }else{
                double prc = utils.round(item.getString("price"),2);
                double sum = prc * qty;
                item.put("sum",utils.round(sum,2));
            }

            contentAdapter.notifyItemChanged(getPosition(item));
        }

        ArrayList<DataBaseItem> getListItems(){
            return listItems;
        }

        boolean contains(DataBaseItem item){
            return findItem(item) != null;
        }

        boolean hasEditedItems(){
            for (DataBaseItem listItem: listItems){
                if (listItem.getInt("edited")==1){
                    return true;
                }
            }
            return false;
        }

        boolean hasUncheckedItems(){
            if (checkedFlagEnabled){
                for (DataBaseItem listItem: listItems){
                    if (!listItem.getBoolean("checked")){
                        return true;
                    }
                }
            }
            return false;
        }

        DataBaseItem findItem(DataBaseItem item){
            for (DataBaseItem listItem: listItems){
                if (listItem.getString("code").equals(item.getString("code"))){
                    return listItem;
                }
            }
            return null;
        }

        int getPosition(DataBaseItem item){
            return listItems.indexOf(item);
        }

        @NonNull
        @Override
        public ContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int viewID = R.layout.document_content_item;
            final View view = LayoutInflater.from(parent.getContext()).inflate(viewID, parent,false);
            return new ContentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ContentViewHolder holder, int position) {
            DataBaseItem dataBaseItem = listItems.get(position);
            holder.itemView.setOnClickListener((View v) -> onListItemClick(dataBaseItem));

            boolean checked = dataBaseItem.getBoolean("checked");
            String rest = dataBaseItem.getString("rest");
            String unit = dataBaseItem.getString("unit");

            holder.showImage(dataBaseItem.getString("code"));
            holder.setCode(dataBaseItem.getString("art"));
            holder.setCode2(dataBaseItem.getString("code2"));
            holder.setCode3(dataBaseItem.getString("code3"));
            holder.setDescription(dataBaseItem.getString("description"));
            holder.setLineNumber(dataBaseItem.getString("line"));
            holder.setQuantity(dataBaseItem.getString("quantity"));
            holder.setCollect(dataBaseItem.getString("collect"));
            holder.setRest(rest,unit);
            holder.setUnit(unit);
            holder.setPrice(dataBaseItem.getString("price"));
            holder.setSum(dataBaseItem.getString("sum"));
            holder.setNotes(dataBaseItem.getString("notes"));
            holder.setStared(dataBaseItem.getInt("edited") == 1);
            holder.setChecked(checked);

            holder.isChecked.setOnClickListener((View v) -> {
                dataBaseItem.put("checked",holder.isChecked.isChecked());
                isModified = true;
                notifyItemChanged(position);
            });

            if (checkedFlagEnabled){
                double restValue = utils.round(rest,3);
                if (workingMode.equals(Constants.MODE_COLLECT)) {
                    if (checked) {
                        holder.descriptionLine.setBackgroundColor(colorWhite);
                    } else {
                        holder.descriptionLine.setBackgroundColor(colorYellow);
                    }
                }else{
                    if (restValue>0 && !checked) {
                        holder.descriptionLine.setBackgroundColor(colorRed);
                    }else if (restValue<=0 && !checked) {
                        holder.descriptionLine.setBackgroundColor(colorYellow);
                    }else {
                        holder.descriptionLine.setBackgroundColor(colorWhite);
                    }
                }
            }
        }

        void onItemDismiss(int position){
            if (getItemViewType(position) == 0){
                listItems.remove(position);
                notifyItemRemoved(position);
            }
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageLoader.stop();
    }
}
