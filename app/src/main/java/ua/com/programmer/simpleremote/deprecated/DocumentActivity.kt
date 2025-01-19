package ua.com.programmer.simpleremote.deprecated

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import ua.com.programmer.simpleremote.repository.DataLoader.DataLoaderListener
import ua.com.programmer.simpleremote.deprecated.DocumentActivity.ContentAdapter
import ua.com.programmer.simpleremote.deprecated.DocumentActivity.ContentViewHolder
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.deprecated.SqliteDB.Companion.getInstance
import ua.com.programmer.simpleremote.repository.DataLoader
import ua.com.programmer.simpleremote.deprecated.settings.AppSettings
import ua.com.programmer.simpleremote.deprecated.settings.Constants
import ua.com.programmer.simpleremote.deprecated.specialItems.Cache
import ua.com.programmer.simpleremote.deprecated.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.deprecated.specialItems.DocumentField
import ua.com.programmer.simpleremote.deprecated.utility.ImageLoader
import ua.com.programmer.simpleremote.deprecated.utility.Utils
import java.lang.Exception
import java.util.ArrayList
import java.util.Objects

class DocumentActivity : AppCompatActivity(), DataLoaderListener {
    private val utils = Utils()
    private val cache: Cache = Cache.getInstance()

    private var contentAdapter: ContentAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var isEditable = false
    private var isModified = false
    private var loadImages = false
    private var progressBar: ProgressBar? = null
    private var documentDataItem: DataBaseItem? = null
    private var database: SqliteDB? = null
    private var imageLoader: ImageLoader? = null

    private var documentGUID: String? = null
    private var documentDataString = ""
    private var isCachedDocument = false
    private var barcode = ""
    private var checkedFlagEnabled = false
    private var currency: String? = null
    private var workingMode: String? = null

    private var field1: DocumentField? = null
    private var field2: DocumentField? = null
    private var field3: DocumentField? = null
    private var field4: DocumentField? = null

    private val openNextScreen =
        registerForActivityResult<Intent?, ActivityResult?>(StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? ->
                val data = result!!.data
                if (data == null) return@ActivityResultCallback

                val dataBaseItem = cache.get(data.getStringExtra("cacheKey"))
                val itemCode = dataBaseItem.getString("code")
                val type = dataBaseItem.getString("type")
                val value = dataBaseItem.getString("description")
                if (!itemCode.isEmpty() && !type.isEmpty()) {
                    if (type == field1!!.type) {
                        field1!!.code = itemCode
                        field1!!.value = value
                        documentDataItem!!.put("field1", field1!!.asString())
                    } else if (type == field2!!.type) {
                        field2!!.code = itemCode
                        field2!!.value = value
                        documentDataItem!!.put("field2", field2!!.asString())
                    } else if (type == Constants.DOCUMENT_LINE) {
                        contentAdapter!!.setItemProperties(dataBaseItem)
                        //                                dataBaseItem.getString("quantity"),
//                                dataBaseItem.getString("price"),
//                                dataBaseItem.getBoolean("checked"));
                        recyclerView!!.scrollToPosition(contentAdapter!!.getPosition(dataBaseItem))

                        if (!contentAdapter!!.hasUncheckedItems()) documentDataItem!!.put(
                            "checked",
                            true
                        )
                    } else if (type == Constants.GOODS) {
                        addGoodsItem(dataBaseItem)
                    }

                    showDocumentHeader()
                } else {
                    Toast.makeText(this, R.string.no_data, Toast.LENGTH_SHORT).show()
                }
            })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_coordinator)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.document)

        val appSettings = AppSettings.getInstance(this)

        database = getInstance(this)
        loadImages = appSettings?.isLoadImages() == true
        workingMode = appSettings?.getWorkingMode()
        imageLoader = ImageLoader(this)
        progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        val intent = getIntent()
        documentDataItem = cache.get(intent.getStringExtra("cacheKey"))
        documentGUID = documentDataItem!!.getString("guid")
        isCachedDocument = documentDataItem!!.hasValue(Constants.CACHE_GUID)
        checkedFlagEnabled = documentDataItem!!.hasValue("checked")
        currency = documentDataItem!!.getString("currency")

        recyclerView = findViewById<RecyclerView>(R.id.document_content)
        //recyclerView.setHasFixedSize(true);
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView!!.setLayoutManager(linearLayoutManager)
        contentAdapter = ContentAdapter()
        recyclerView!!.setAdapter(contentAdapter)

        val simpleCallback: ItemTouchHelper.SimpleCallback =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    if (contentAdapter!!.getItemViewType(viewHolder.getBindingAdapterPosition()) == RecyclerView.NO_POSITION) {
                        return 0
                    }
                    return super.getSwipeDirs(recyclerView, viewHolder)
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    contentAdapter!!.onItemDismiss(viewHolder.getBindingAdapterPosition())
                }

                override fun isItemViewSwipeEnabled(): Boolean {
                    return isEditable
                }

                override fun isLongPressDragEnabled(): Boolean {
                    return false
                }
            }
        if (workingMode == Constants.MODE_FULL) {
            val itemTouchHelper = ItemTouchHelper(simpleCallback)
            itemTouchHelper.attachToRecyclerView(recyclerView)
        }

        showDocumentHeader()
        updateContent()
        setEditableMode(false)
    }

    private fun getDocumentTitle(): String? {
        val type = documentDataItem!!.getString(Constants.TYPE)
        val allowedDocuments = AppSettings.getAllowedDocuments()
        for (doc in allowedDocuments) {
            if (doc?.getString("code") == type) {
                return doc.getString("description")
            }
        }
        return getString(R.string.document)
    }

    private fun showDocumentHeader() {
        val textTitle = findViewById<TextView>(R.id.document_title)
        textTitle.text = getDocumentTitle()
        val textNumber = findViewById<TextView>(R.id.document_number)
        textNumber.text = documentDataItem!!.getString(Constants.DOCUMENT_NUMBER)
        val textDate = findViewById<TextView>(R.id.document_date)
        textDate.text = documentDataItem!!.getString(Constants.DOCUMENT_DATE)

        documentGUID = documentDataItem!!.getString(Constants.GUID)

        val documentIcon = findViewById<ImageView>(R.id.document_icon)
        if (documentGUID!!.contains("!")) {
            documentIcon.setImageResource(R.drawable.sharp_help_outline_24)
            setEditableMode(true)
        } else if (isCachedDocument) {
            documentIcon.setImageResource(R.drawable.sharp_help_outline_24)
        } else if (documentDataItem!!.getInt(Constants.DOCUMENT_IS_DELETED) == 1) {
            documentIcon.setImageResource(R.drawable.twotone_close_24)
        } else if (documentDataItem!!.getInt(Constants.DOCUMENT_IS_PROCESSED) == 1) {
            documentIcon.setImageResource(R.drawable.twotone_check_box_24)
        } else {
            documentIcon.setImageResource(R.drawable.twotone_check_box_outline_blank_24)
        }

        val contractorLine = findViewById<LinearLayout>(R.id.document_header_contractor)
        val contractor = documentDataItem!!.getString("contractor")
        if (contractor.isEmpty()) {
            contractorLine.visibility = View.GONE
        } else {
            contractorLine.visibility = View.VISIBLE
            val tvContractor = findViewById<TextView>(R.id.document_contractor)
            tvContractor.text = contractor
        }

        field1 = DocumentField(documentDataItem!!.getString("field1"))
        field2 = DocumentField(documentDataItem!!.getString("field2"))
        field3 = DocumentField(documentDataItem!!.getString("field3"))
        field4 = DocumentField(documentDataItem!!.getString("field4"))

        val layoutField1 = findViewById<LinearLayout>(R.id.document_header_field1)
        if (field1!!.isReal()) {
            layoutField1.visibility = View.VISIBLE
            val field1Name = findViewById<TextView>(R.id.document_field1_name)
            field1Name.text = field1!!.description
            val field1Value = findViewById<TextView>(R.id.document_field1_value)
            field1Value.text = field1!!.value
            layoutField1.setOnClickListener(View.OnClickListener { v: View? ->
                onSpecialFieldClick(
                    "field1",
                    field1!!
                )
            })
        } else {
            layoutField1.visibility = View.GONE
        }

        val layoutField2 = findViewById<LinearLayout>(R.id.document_header_field2)
        if (field2!!.isReal()) {
            layoutField2.visibility = View.VISIBLE
            val field2Name = findViewById<TextView>(R.id.document_field2_name)
            field2Name.text = field2!!.description
            val field2Value = findViewById<TextView>(R.id.document_field2_value)
            field2Value.text = field2!!.value
            layoutField2.setOnClickListener(View.OnClickListener { v: View? ->
                onSpecialFieldClick(
                    "field2",
                    field2!!
                )
            })
        } else {
            layoutField2.visibility = View.GONE
        }

        val layoutField3 = findViewById<LinearLayout>(R.id.document_header_field3)
        if (field3!!.isReal()) {
            layoutField3.visibility = View.VISIBLE
            val field3Name = findViewById<TextView>(R.id.document_field3_name)
            field3Name.text = field3!!.description
            val field3Value = findViewById<TextView>(R.id.document_field3_value)
            field3Value.text = field3!!.value
            layoutField3.setOnClickListener(View.OnClickListener { v: View? ->
                onSpecialFieldClick(
                    "field3",
                    field3!!
                )
            })
        } else {
            layoutField3.visibility = View.GONE
        }

        val layoutField4 = findViewById<LinearLayout>(R.id.document_header_field4)
        if (field4!!.isReal()) {
            layoutField4.visibility = View.VISIBLE
            val field4Name = findViewById<TextView>(R.id.document_field4_name)
            field4Name.text = field4!!.description
            val field4Value = findViewById<TextView>(R.id.document_field4_value)
            field4Value.text = field4!!.value
            layoutField4.setOnClickListener(View.OnClickListener { v: View? ->
                onSpecialFieldClick(
                    "field4",
                    field4!!
                )
            })
        } else {
            layoutField4.visibility = View.GONE
        }

        val documentCheckedLine = findViewById<LinearLayout>(R.id.document_header_checked)
        if (checkedFlagEnabled) {
            documentCheckedLine.visibility = View.VISIBLE
            val documentIsChecked = findViewById<CheckBox>(R.id.document_is_checked)
            documentIsChecked.isChecked = documentDataItem!!.getBoolean("checked")
            documentIsChecked.setOnClickListener(View.OnClickListener { v: View? ->
                documentDataItem!!.put("checked", documentIsChecked.isChecked)
                isModified = true
            })
        } else {
            documentCheckedLine.visibility = View.GONE
        }

        val textViewNotes = findViewById<TextView>(R.id.document_header_notes)
        val notes = documentDataItem!!.getString("notes")
        if (notes.isEmpty()) {
            textViewNotes.setHint(R.string.notes)
        } else {
            textViewNotes.text = notes
        }
        textViewNotes.setOnClickListener(View.OnClickListener { v: View? ->
            openTextEditDialog(
                "notes",
                null
            )
        })
    }

    private fun openTextEditDialog(fieldName: String?, field: DocumentField?) {
        val builder = AlertDialog.Builder(this)

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_edit_text, null)

        val editText = view.findViewById<EditText>(R.id.edit_text)
        var title: String?
        if (field != null) {
            editText.setText(field.value)
            title = field.description
        } else {
            editText.setText(documentDataItem!!.getString(fieldName))
            title = getResources().getString(R.string.notes)
        }

        builder.setView(view)
        builder.setMessage("")
            .setTitle(title)
            .setPositiveButton(
                getResources().getString(R.string.action_save),
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                    if (field != null) {
                        field.value = editText.text.toString()
                        documentDataItem!!.put(fieldName, field.asString())
                    } else {
                        documentDataItem!!.put(fieldName, editText.text.toString())
                    }
                    showDocumentHeader()
                })
            .setNegativeButton(getResources().getString(R.string.action_cancel), null)
        val dialog = builder.create()
        try {
            Objects.requireNonNull<Window?>(dialog.window)
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        } catch (ex: Exception) {
            utils.log("e", "Set soft input mode: $ex")
        }
        dialog.show()
    }

    private fun onSaveError() {
        val builder = AlertDialog.Builder(this)
        if (!documentDataString.isEmpty()) {
            database!!.cacheData(documentGUID, documentDataString)
            builder.setMessage(R.string.saved_in_cache_warn)
                .setTitle(R.string.warning)
                .setPositiveButton(
                    R.string.ok,
                    DialogInterface.OnClickListener { di: DialogInterface?, i: Int -> onBackPressed() })
                .create()
                .show()
        } else {
            builder.setMessage(R.string.save_error_warn)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show()
        }
    }

    private fun showMessage(text: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
            .setTitle(R.string.warning)
            .setPositiveButton(R.string.ok, null)
            .create()
            .show()
    }

    private fun saveDocument() {
        if (contentAdapter!!.hasUncheckedItems()) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.warn_document_has_unchecked_items)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show()
        }

        documentDataString = ""
        val document = JSONObject()
        try {
            document.put(
                Constants.DOCUMENT_NUMBER,
                documentDataItem!!.getString(Constants.DOCUMENT_NUMBER)
            )
            document.put(Constants.TYPE, documentDataItem!!.getString(Constants.TYPE))
            document.put(Constants.GUID, documentDataItem!!.getString(Constants.GUID))

            //extra data fo cached document
            document.put(
                Constants.DOCUMENT_DATE,
                documentDataItem!!.getString(Constants.DOCUMENT_DATE)
            )
            document.put("company", documentDataItem!!.getString("company"))
            document.put("field1", field1!!.asString())
            document.put("field2", field2!!.asString())
            document.put("field3", field3!!.asString())
            document.put("field4", field4!!.asString())
            document.put("notes", documentDataItem!!.getString("notes"))
            document.put("sum", documentDataItem!!.getString("sum"))
            document.put("checked", documentDataItem!!.getBoolean("checked"))

            val lines = JSONArray()
            val items = contentAdapter!!.getListItems()
            for (item in items) {
                lines.put(item.getAsJSON())
            }
            document.put("lines", lines)
            documentDataString = document.toString()
        } catch (ex: Exception) {
            utils.error("Save document: $ex")
            Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show()
            return
        }
        progressBar!!.visibility = View.VISIBLE
        val dataLoader = DataLoader(this)
        dataLoader.saveDocument(documentDataString)
    }

    override fun onDataLoaded(dataItems: ArrayList<DataBaseItem?>?) {
        progressBar!!.visibility = View.INVISIBLE
        if (dataItems == null) {
            Toast.makeText(this, R.string.no_data, Toast.LENGTH_SHORT).show()
            return
        }

        val items : ArrayList<DataBaseItem> = ArrayList()
        for (item in dataItems) {
            if (item != null) {
                items.add(item)
            }
        }
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.no_data, Toast.LENGTH_SHORT).show()
            return
        }

        //received special data item or document content ??
        var isSpecial = false
        if (items.size == 1) {
            val item = items[0]
            isSpecial = item.getInt("line") == 0
        }

        if (isSpecial) {
            val item = items[0]
            val savedFlag = item.getString("saved")
            val type = item.getString(Constants.TYPE)

            //received goods item by barcode scanner
            if (type == Constants.GOODS) {
                addGoodsItem(item)
            } else if (type == Constants.MESSAGE) {
                showMessage(item.getString("text"))
            } else if (!item.getString(Constants.DOCUMENT_NUMBER).isEmpty()) {
                documentDataItem = item
                showDocumentHeader()
                //dataItems.clear();
            }

            if (savedFlag == "ok") {
                isModified = false
                Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()

                //delete cached document if any
                database!!.deleteCachedData(documentGUID)

                //if document is loaded from cache, exit
                if (isCachedDocument) {
                    onBackPressed()
                } else {
                    updateContent()
                }
            } else if (!savedFlag.isEmpty()) {
                Toast.makeText(
                    this,
                    getResources().getString(R.string.toast_error) + " " + item.getString("error"),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            contentAdapter!!.loadListItems(items)
        }
    }

    override fun onDataLoaderError(error: String?) {
        progressBar!!.visibility = View.INVISIBLE
        if (contentAdapter!!.hasEditedItems()) {
            onSaveError()
        } else {
            Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("")
    override fun onBackPressed() {
        if (contentAdapter!!.hasEditedItems() || isModified) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.unsaved_document_warn)
                .setTitle(R.string.warning)
                .setPositiveButton(
                    R.string.yes,
                    DialogInterface.OnClickListener { di: DialogInterface?, i: Int -> super.onBackPressed() })
                .setNegativeButton(R.string.action_cancel, null)
                .create()
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) onBackPressed()
        if (id == R.id.edit_document) setEditableMode(!isEditable)
        if (id == R.id.save_document) saveDocument()
        if (id == R.id.refresh) checkAndUpdateContent()
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.document_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun addGoodsItem(dataBaseItem: DataBaseItem) {
        if (!contentAdapter!!.contains(dataBaseItem)) {
            contentAdapter!!.addItem(dataBaseItem)
            //recyclerView.scrollToPosition(contentAdapter.getItemCount()-1);
        }
        val listItem = contentAdapter!!.findItem(dataBaseItem)
        if (listItem != null) {
            //onListItemClick(listItem);

            if (workingMode == Constants.MODE_COLLECT) {
                listItem.put("collect", listItem.getDouble("collect") + 1)
            } else {
                listItem.put("quantity", listItem.getDouble("quantity") + 1)
            }
            contentAdapter!!.setItemProperties(listItem)
            recyclerView!!.scrollToPosition(contentAdapter!!.getPosition(listItem))

            if (!contentAdapter!!.hasUncheckedItems()) documentDataItem!!.put("checked", true)
        }
    }

    private fun updateContent() {
        documentDataItem?.let {
            progressBar!!.visibility = View.VISIBLE
            val dataLoader = DataLoader(this)
            dataLoader.getDocumentContent(it)
        }
    }

    private fun checkAndUpdateContent() {
        if (contentAdapter!!.hasEditedItems()) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.unsaved_document_warn)
                .setTitle(R.string.warning)
                .setPositiveButton(
                    R.string.yes,
                    DialogInterface.OnClickListener { di: DialogInterface?, i: Int -> updateContent() })
                .setNegativeButton(R.string.action_cancel, null)
                .create()
                .show()
        } else {
            updateContent()
        }
    }

    private fun openScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        intent.putExtra("document", documentGUID)
        openNextScreen.launch(intent)
    }

    private fun setEditableMode(newMode: Boolean) {
        isEditable = newMode
        val bottomBar = findViewById<View>(R.id.bottom_bar)
        val addItemButton = findViewById<TextView>(R.id.add_item_button)
        val scannerButton = findViewById<TextView>(R.id.scanner_button)
        if (isEditable) {
            bottomBar.visibility = View.VISIBLE
            scannerButton.setOnClickListener(View.OnClickListener { v: View? -> openScanner() })
            if (workingMode == Constants.MODE_COLLECT) {
                addItemButton.visibility = View.GONE
            } else {
                addItemButton.setOnClickListener(View.OnClickListener { v: View? -> onAddButtonClick() })
            }
        } else {
            bottomBar.visibility = View.GONE
        }
    }

    private fun openItemEditDialog(item: DataBaseItem) {
        val builder = AlertDialog.Builder(this)

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.document_line_edit_dialog, null)

        val restText = item.getString("rest")
        if (restText.isEmpty()) {
            val restLine = view.findViewById<LinearLayout>(R.id.rest_line)
            restLine.visibility = View.INVISIBLE
        } else {
            val rest = view.findViewById<TextView>(R.id.rest)
            rest.text = utils.format(item.getDouble("rest"), 3)
        }

        val editQuantity = view.findViewById<EditText>(R.id.edit_quantity)
        editQuantity.hint = item.getString("quantity")

        val editPrice = view.findViewById<EditText>(R.id.edit_price)
        editPrice.hint = item.getString("price")

        //editPrice.setEnabled(false);
        val checkedCheckBox = view.findViewById<CheckBox>(R.id.checked_box)
        if (checkedFlagEnabled) {
            //checkedCheckBox.setVisibility(View.VISIBLE);
            checkedCheckBox.isChecked = item.getBoolean("checked")
        } else {
            val checkedLine = view.findViewById<LinearLayout>(R.id.checked_line)
            checkedLine.visibility = View.GONE
        }

        builder.setView(view)
        builder.setMessage(item.getString("art"))
            .setTitle(item.getString("description"))
            .setPositiveButton(
                R.string.action_save,
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                    val enteredQuantity = editQuantity.text.toString()
                    if (!enteredQuantity.isEmpty()) {
                        item.put("quantity", utils.round(enteredQuantity, 3))
                    }
                    val enteredPrice = editPrice.text.toString()
                    if (enteredPrice.isEmpty()) {
                        item.put("price", utils.round(enteredPrice, 2))
                    }
                    item.put("checked", checkedCheckBox.isChecked)
                    contentAdapter!!.setItemProperties(item)
                    recyclerView!!.scrollToPosition(contentAdapter!!.getPosition(item))
                })
            .setNegativeButton(
                R.string.action_cancel,
                DialogInterface.OnClickListener { dialog: DialogInterface?, i: Int -> })
        val dialog = builder.create()
        val window = dialog.window
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        editQuantity.requestFocus()

        editQuantity.setOnEditorActionListener(OnEditorActionListener { textView: TextView?, i: Int, keyEvent: KeyEvent? ->
            if (i == EditorInfo.IME_ACTION_NEXT || i == EditorInfo.IME_ACTION_DONE) {
                val enteredQuantity = editQuantity.text.toString()
                if (!enteredQuantity.isEmpty()) {
                    item.put("quantity", utils.round(enteredQuantity, 3))
                }
                val enteredPrice = editPrice.text.toString()
                if (enteredPrice.isEmpty()) {
                    item.put("price", utils.round(enteredPrice, 2))
                }
                item.put("checked", checkedCheckBox.isChecked)
                contentAdapter!!.setItemProperties(item)
                recyclerView!!.scrollToPosition(contentAdapter!!.getPosition(item))
                dialog.dismiss()
            }
            false
        })
    }

    private fun onListItemClick(item: DataBaseItem) {
        if (!isEditable) {
            return
        }
        item.put("workingMode", workingMode)

        if (workingMode == Constants.MODE_COLLECT) {
            val intent = Intent(this, ItemEditScreen::class.java)
            intent.putExtra("cacheKey", Cache.getInstance().put(item))
            openNextScreen.launch(intent)
        } else {
            openItemEditDialog(item)
        }
    }

    private fun onListItemChecked(isChecked: Boolean, item: DataBaseItem) {
        item.put("checked", isChecked)
        if (!contentAdapter!!.hasUncheckedItems()) {
            documentDataItem!!.put("checked", true)
            showDocumentHeader()
        } else if (documentDataItem!!.getBoolean("checked")) {
            documentDataItem!!.put("checked", false)
            showDocumentHeader()
        }
    }

    private fun onAddButtonClick() {
        if (isEditable) {
            val intent = Intent(this, CatalogListActivity::class.java)
            intent.putExtra("catalogType", Constants.GOODS)
            intent.putExtra("itemSelectionMode", true)
            intent.putExtra("documentGUID", documentGUID)
            openNextScreen.launch(intent)
        }
    }

    private fun onSpecialFieldClick(fieldName: String?, specialField: DocumentField) {
        if (isEditable) {
            if (specialField.isCatalog()) {
                val intent = Intent(this, CatalogListActivity::class.java)
                intent.putExtra("catalogType", specialField.type)
                intent.putExtra("itemSelectionMode", true)
                intent.putExtra("documentGUID", documentGUID)
                openNextScreen.launch(intent)
            } else {
                openTextEditDialog(fieldName, specialField)
            }
            isModified = true
        }
    }

    private fun onBarcodeReceived() {
        if (!barcode.isEmpty()) {
            //utils.debug("onBarcodeReceived: "+barcode);
            progressBar!!.visibility = View.VISIBLE

            val barcodeParameters = DataBaseItem()
            barcodeParameters.put("value", barcode)
            barcodeParameters.put("guid", documentGUID)

            val dataLoader = DataLoader(this)
            dataLoader.getItemWithBarcode(barcodeParameters)
        }
        barcode = ""
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            return true
        }
        val keyCode = event.keyCode
        //utils.debug("KEY: "+keyCode+"; "+barcode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed()
            return true
        }
        if (!isEditable) {
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB) {
            onBarcodeReceived()
        } else {
            val key = event.unicodeChar.toChar()
            if (Character.isDigit(key) || Character.isLetter(key)) {
                barcode += key
            } else {
                barcode = ""
            }
        }
        return true
    }

    internal inner class ContentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvCode: TextView = view.findViewById<TextView>(R.id.item_code)
        var tvCode2: TextView = view.findViewById<TextView>(R.id.item_code2)
        var tvCode3: TextView = view.findViewById<TextView>(R.id.item_code3)
        var tvDescription: TextView = view.findViewById<TextView>(R.id.item_description)
        var tvLineNumber: TextView? = view.findViewById<TextView?>(R.id.item_line_number)
        var tvQuantity: TextView = view.findViewById<TextView>(R.id.item_quantity)
        var tvRest: TextView = view.findViewById<TextView>(R.id.item_rest)
        var tvCollect: TextView = view.findViewById<TextView>(R.id.item_collect)
        var tvRestTitle: LinearLayout = view.findViewById<LinearLayout>(R.id.item_rest_title)
        var descriptionLine: LinearLayout
        var tvUnit: TextView = view.findViewById<TextView>(R.id.item_unit)
        var tvPrice: TextView = view.findViewById<TextView>(R.id.item_price)
        var tvSum: TextView = view.findViewById<TextView>(R.id.item_sum)
        var tvNotes: TextView = view.findViewById<TextView>(R.id.item_notes)
        var cardView: CardView? = view.findViewById<CardView?>(R.id.item_card)
        var iconStar: ImageView = view.findViewById<ImageView>(R.id.icon_star)
        var image: ImageView
        var isChecked: CheckBox

        init {
            isChecked = view.findViewById<CheckBox>(R.id.is_checked)
            image = view.findViewById<ImageView>(R.id.item_image)
            descriptionLine = view.findViewById<LinearLayout>(R.id.description_line)
        }

        fun setCode(str: String?) {
            this.tvCode.text = str
        }

        fun setCode2(str: String) {
            this.tvCode2.text = str
            if (str.isEmpty()) tvCode2.visibility = View.GONE
        }

        fun setCode3(str: String) {
            this.tvCode3.text = str
            if (str.isEmpty()) tvCode3.visibility = View.GONE
        }

        fun setDescription(str: String?) {
            this.tvDescription.text = str
        }

        fun setNotes(str: String) {
            this.tvNotes.text = str
            if (str.isEmpty()) tvNotes.visibility = View.GONE
        }

        fun setRest(str: String?, unit: String?) {
            val rest = utils.round(str, 3)
            if (rest == 0.0) {
                tvRestTitle.visibility = View.INVISIBLE
                tvRest.visibility = View.INVISIBLE
            } else {
                val qtyText = utils.formatAsInteger(rest) + " " + unit
                tvRestTitle.visibility = View.VISIBLE
                tvRest.visibility = View.VISIBLE
                tvRest.text = qtyText
            }
        }

        fun setQuantity(str: String?) {
            var str = str
            str = utils.formatAsInteger(str)
            this.tvQuantity.text = str
        }

        fun setCollect(str: String) {
            var str = str
            str = utils.formatAsInteger(str)
            if (str == "0") str = "--"
            this.tvCollect.text = str
        }

        fun setUnit(str: String?) {
            this.tvUnit.text = str
        }

        fun setPrice(str: String?) {
            var str = str
            str = "$str $currency"
            this.tvPrice.text = str
        }

        fun setSum(str: String?) {
            var str = str
            str = "= $str $currency"
            this.tvSum.text = str
        }

        fun setLineNumber(lineNumber: String?) {
            if (tvLineNumber != null) {
                tvLineNumber!!.text = lineNumber
            }
        }

        fun setStared(isStared: Boolean) {
            if (isStared) {
                iconStar.visibility = View.VISIBLE
            } else {
                iconStar.visibility = View.INVISIBLE
            }
        }

        fun setChecked(checkedFlag: Boolean) {
            if (checkedFlagEnabled) {
                isChecked.visibility = View.VISIBLE
                isChecked.isChecked = checkedFlag
            } else {
                isChecked.visibility = View.GONE
            }
        }

        fun showImage(code: String?) {
            if (loadImages) {
                image.visibility = View.VISIBLE
                imageLoader!!.load(code, image)
            } else {
                image.visibility = View.GONE
            }
        }
    }

    internal inner class ContentAdapter : RecyclerView.Adapter<ContentViewHolder?>() {
        private val listItems = ArrayList<DataBaseItem>()
        private val colorRed = getResources().getColor(R.color.backgroundRed)
        private val colorYellow = getResources().getColor(R.color.backgroundYellow)
        private val colorWhite = getResources().getColor(R.color.colorWhite)

        @SuppressLint("NotifyDataSetChanged")
        fun loadListItems(values: ArrayList<DataBaseItem>) {
            listItems.clear()
            listItems.addAll(values)
            notifyDataSetChanged()
        }

        fun addItem(item: DataBaseItem?) {
            listItems.add(item!!)
            notifyItemInserted(listItems.size - 1)
            //notifyDataSetChanged();
        }

        fun setItemProperties(item: DataBaseItem) {
            //utils.debug(item.getAsJSON().toString());
            val qty = utils.round(item.getString("quantity"), 3)
            item.put("edited", 1)

            if (workingMode == Constants.MODE_COLLECT) {
                val collect = utils.round(item.getString("collect"), 3)
                item.put("checked", collect <= qty)
            } else {
                val prc = utils.round(item.getString("price"), 2)
                val sum = prc * qty
                item.put("sum", utils.round(sum, 2))
            }

            contentAdapter!!.notifyItemChanged(getPosition(item))
        }

        fun getListItems(): ArrayList<DataBaseItem> {
            return listItems
        }

        fun contains(item: DataBaseItem): Boolean {
            return findItem(item) != null
        }

        fun hasEditedItems(): Boolean {
            for (listItem in listItems) {
                if (listItem.getInt("edited") == 1) {
                    return true
                }
            }
            return false
        }

        fun hasUncheckedItems(): Boolean {
            if (checkedFlagEnabled) {
                for (listItem in listItems) {
                    if (!listItem.getBoolean("checked")) {
                        return true
                    }
                }
            }
            return false
        }

        fun findItem(item: DataBaseItem): DataBaseItem? {
            for (listItem in listItems) {
                if (listItem.getString("code") == item.getString("code")) {
                    return listItem
                }
            }
            return null
        }

        fun getPosition(item: DataBaseItem?): Int {
            return listItems.indexOf(item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
            val viewID = R.layout.document_content_item
            val view = LayoutInflater.from(parent.context).inflate(viewID, parent, false)
            return ContentViewHolder(view)
        }

        override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
            val dataBaseItem: DataBaseItem = listItems[position]
            holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
                onListItemClick(
                    dataBaseItem
                )
            })

            val checked = dataBaseItem.getBoolean("checked")
            val rest = dataBaseItem.getString("rest")
            val unit = dataBaseItem.getString("unit")

            holder.showImage(dataBaseItem.getString("code"))
            holder.setCode(dataBaseItem.getString("art"))
            holder.setCode2(dataBaseItem.getString("code2"))
            holder.setCode3(dataBaseItem.getString("code3"))
            holder.setDescription(dataBaseItem.getString("description"))
            holder.setLineNumber(dataBaseItem.getString("line"))
            holder.setQuantity(dataBaseItem.getString("quantity"))
            holder.setCollect(dataBaseItem.getString("collect"))
            holder.setRest(rest, unit)
            holder.setUnit(unit)
            holder.setPrice(dataBaseItem.getString("price"))
            holder.setSum(dataBaseItem.getString("sum"))
            holder.setNotes(dataBaseItem.getString("notes"))
            holder.setStared(dataBaseItem.getInt("edited") == 1)
            holder.setChecked(checked)

            holder.isChecked.setOnClickListener(View.OnClickListener { v: View? ->
                onListItemChecked(holder.isChecked.isChecked, dataBaseItem)
                isModified = true
                notifyItemChanged(position)
            })

            if (checkedFlagEnabled) {
                val restValue = utils.round(rest, 3)
                if (workingMode == Constants.MODE_COLLECT) {
                    if (checked) {
                        holder.descriptionLine.setBackgroundColor(colorWhite)
                    } else {
                        holder.descriptionLine.setBackgroundColor(colorYellow)
                    }
                } else {
                    if (restValue > 0 && !checked) {
                        holder.descriptionLine.setBackgroundColor(colorRed)
                    } else if (restValue <= 0 && !checked) {
                        holder.descriptionLine.setBackgroundColor(colorYellow)
                    } else {
                        holder.descriptionLine.setBackgroundColor(colorWhite)
                    }
                }
            }
        }

        fun onItemDismiss(position: Int) {
            if (getItemViewType(position) == 0) {
                listItems.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        override fun getItemCount(): Int {
            return listItems.size
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageLoader!!.stop()
    }
}
