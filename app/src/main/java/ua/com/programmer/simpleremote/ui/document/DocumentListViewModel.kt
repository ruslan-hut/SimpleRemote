package ua.com.programmer.simpleremote.ui.document

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {

    private val _documents = MutableLiveData<List<Document>>()
    val documents: LiveData<List<Document>> get() = _documents

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    var title: String = ""
    var type: String = ""

    fun setDocumentType(type: String?, title: String?) {
        this.title = title ?: ""
        type?.let {
            this.type = it
            _isLoading.value = true
            viewModelScope.launch {
                networkRepository.documents(it).collect {
                    _documents.value = it
                    _isLoading.value = false
                }
            }
        }
    }

}