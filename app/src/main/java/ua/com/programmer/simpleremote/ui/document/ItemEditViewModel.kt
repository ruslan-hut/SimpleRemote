package ua.com.programmer.simpleremote.ui.document

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.getImage
import ua.com.programmer.simpleremote.ui.shared.FileManager
import javax.inject.Inject

@HiltViewModel
class ItemEditViewModel @Inject constructor(
    private val fileManager: FileManager
): ViewModel() {

    var content: List<Content> = listOf()

    fun loadContent(content: List<Content>) {
        this.content = content
    }

    fun confirmQuantity(product: Product?, newQty: String, editNotes: String): List<Content> {
        Log.d("RC_ItemEditViewModel", "confirmQuantity code: ${product?.code} qty: $newQty notes: $editNotes")
        if (product == null) return content
        val list = content.toMutableList()
        val item = list.find { it.code == product.code }
        item?.apply {
            collect = newQty
            modified = true
            notes = editNotes
            checked = (newQty.toDoubleOrNull() ?: 0.0) >= (quantity.toDoubleOrNull() ?: 0.0)
            userImage = product.getImage()
            encodedImage = fileManager.getFileData(product.getImage())
        }
        return list
    }
}