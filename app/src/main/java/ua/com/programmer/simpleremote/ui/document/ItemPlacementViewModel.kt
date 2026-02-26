package ua.com.programmer.simpleremote.ui.document

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Place
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.getImage
import ua.com.programmer.simpleremote.ui.shared.FileManager
import javax.inject.Inject


@HiltViewModel
class ItemPlacementViewModel @Inject constructor(
    private val fileManager: FileManager
): ViewModel() {

    var content: List<Content> = listOf()

    fun loadContent(content: List<Content>) {
        this.content = content
    }

    fun confirmPlace(product: Product?, places: List<Place>): List<Content> {
        Log.d("RC_ItemEditViewModel", "confirmQuantity code: ${product?.code} places: $places")
        if (product == null) return content
        val list = content.toMutableList()
        val item = list.find { it.code == product.code }
        val totalQuantity = places.sumOf { it.quantity }
        item?.apply {
            modified = true
            place = places
            checked = totalQuantity == (quantity.toIntOrNull() ?: 0)
            userImage = product.getImage()
            encodedImage = fileManager.getFileData(product.getImage())
        }
        return list
    }

}