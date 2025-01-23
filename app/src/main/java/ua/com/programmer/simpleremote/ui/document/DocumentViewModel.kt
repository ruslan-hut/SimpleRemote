package ua.com.programmer.simpleremote.ui.document

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
    ): ViewModel() {

    var title = ""
    var type = ""

    fun setDocumentType(type: String?, title: String?) {
        this.type = type ?: ""
        this.title = title ?: ""
    }

    fun setDocumentId(id: String) {

    }
}