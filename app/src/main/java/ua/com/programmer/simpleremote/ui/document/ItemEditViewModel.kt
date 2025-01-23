package ua.com.programmer.simpleremote.ui.document

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Product
import javax.inject.Inject

@HiltViewModel
class ItemEditViewModel @Inject constructor(): ViewModel() {

    var content: List<Content> = listOf()

    fun loadContent(content: List<Content>) {
        this.content = content
    }

    fun confirmQuantity(product: Product?, newQty: String): List<Content> {
        if (product == null) return content
        val list = content.toMutableList()
        val item = list.find { it.code == product.code }
        item?.apply {
            collect = newQty
            modified = true
            checked = newQty.toDoubleOrNull() == quantity.toDoubleOrNull()
        }
        return list
    }
}