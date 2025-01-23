package ua.com.programmer.simpleremote.deprecated

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.deprecated.settings.Constants
import ua.com.programmer.simpleremote.deprecated.specialItems.Cache
import ua.com.programmer.simpleremote.deprecated.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.deprecated.utility.Utils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.Exception

class ItemEditScreen : AppCompatActivity() {
    private var item: DataBaseItem? = null
    private var editQuantity: EditText? = null
    private var editNotes: EditText? = null
    private var attachImage: String? = null
    private var outputDirectory: File? = null
    private var workingMode: String? = null

    private val utils = Utils()

    private val openCameraScreen =
        registerForActivityResult<Intent?, ActivityResult?>(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? ->
                val data = result!!.data
                if (data == null) return@ActivityResultCallback
                val dataBaseItem =
                    Cache.Companion.getInstance().get(data.getStringExtra("cacheKey"))
                val newImage = dataBaseItem.getString("image")
                if (!newImage.isEmpty()) {
                    if (!attachImage!!.isEmpty()) {
                        val file = File(outputDirectory, attachImage ?: "")
                        if (file.exists()) {
                            if (file.delete()) utils.debug("Item edit: File deleted: $attachImage")
                        }
                    }
                    attachImage = newImage
                    item!!.put("image", newImage)
                    item!!.put("encodedImage", encodeImage())
                    showImage()
                }
            })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_item_edit)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.item_edit)

        outputDirectory = this.applicationContext.filesDir

        val intent = getIntent()
        item = Cache.Companion.getInstance().get(intent.getStringExtra("cacheKey"))
        item!!.put("type", Constants.DOCUMENT_LINE)
        workingMode = item!!.getString("workingMode")

        editQuantity = findViewById<EditText>(R.id.edit_quantity)
        editQuantity!!.setOnEditorActionListener(TextView.OnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            onEditTextAction(
                actionId
            )
        })
        val window = getWindow()
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editQuantity!!.requestFocus()

        editNotes = findViewById<EditText>(R.id.edit_notes)
        editNotes!!.setText(item!!.getString("notes"))

        setText(R.id.item_description, item!!.getString("description"))
        setText(R.id.item_code, item!!.getString("art"))
        setText(R.id.collect_edit, item!!.getString("quantity"))
        setText(R.id.rest_edit, item!!.getString("rest"))

        val buttonCancel = findViewById<TextView>(R.id.button_cancel)
        buttonCancel.setOnClickListener(View.OnClickListener { v: View? -> onBackPressed() })
        val buttonOK = findViewById<TextView>(R.id.button_yes)
        buttonOK.setOnClickListener(View.OnClickListener { v: View? -> saveValuesAndExit() })

        val cameraButton = findViewById<ImageView>(R.id.camera_icon)
        cameraButton.setOnClickListener(View.OnClickListener { v: View? -> openCamera() })

        attachImage = item!!.getString("image")
        showImage()
    }

    private fun setText(id: Int, text: String?) {
        val textView = findViewById<TextView?>(id)
        textView?.text = text
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    private fun onEditTextAction(actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
            saveValuesAndExit()
            return true
        }
        return false
    }

    private fun saveValuesAndExit() {
        val enteredQuantity = editQuantity!!.text.toString()
        if (!enteredQuantity.isEmpty()) {
            if (workingMode == Constants.MODE_COLLECT) {
                item!!.put("collect", enteredQuantity)
            } else {
                item!!.put("quantity", enteredQuantity)
            }
        }

        val enteredNotes = editNotes!!.text.toString()
        item!!.put("notes", enteredNotes)

        val intent = getIntent()
        intent.putExtra("cacheKey", Cache.Companion.getInstance().put(item))
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra("cacheKey", Cache.Companion.getInstance().put(item))
        openCameraScreen.launch(intent)
    }

    private fun showImage() {
        if (attachImage!!.isEmpty()) return
        val imageView = findViewById<ImageView>(R.id.item_image)
        Glide.with(this)
            .load(File(outputDirectory, attachImage ?: ""))
            .into(imageView)
    }

    private fun encodeImage(): String? {
        if (attachImage!!.isEmpty()) return ""
        var encodedImage: String? = ""
        val image = File(outputDirectory, attachImage ?: "")
        if (image.exists()) {
            val imageBytes = ByteArray(image.length().toInt())
            try {
                val stream = BufferedInputStream(FileInputStream(image))
                if (stream.read(imageBytes, 0, imageBytes.size) == imageBytes.size) {
                    encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                }
                stream.close()
            } catch (e: Exception) {
                utils.error("Item edit: file encode: $e")
            }
        }
        return encodedImage
    }
}