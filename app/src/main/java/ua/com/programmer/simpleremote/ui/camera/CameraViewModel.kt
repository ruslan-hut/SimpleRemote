package ua.com.programmer.simpleremote.ui.camera

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
): ViewModel() {
}