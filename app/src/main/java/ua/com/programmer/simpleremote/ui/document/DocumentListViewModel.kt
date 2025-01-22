package ua.com.programmer.simpleremote.ui.document

import android.util.Log
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

    fun setDocumentType(type: String) {
        viewModelScope.launch {
            networkRepository.documents(type).collect {
                _documents.value = it
            }
        }
    }
}