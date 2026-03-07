package ua.com.programmer.simpleremote.ui.shared


import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FileManager @Inject constructor(context: Context) {

    private val fileDir: File? = context.filesDir

    suspend fun getFileData(fileName: String): String = withContext(Dispatchers.IO) {
        if (fileName.isEmpty()) return@withContext ""
        val file = File(fileDir, fileName)
        if (!file.exists()) return@withContext ""
        val fileContent = file.readBytes()
        android.util.Base64.encodeToString(fileContent, android.util.Base64.DEFAULT)
    }
}