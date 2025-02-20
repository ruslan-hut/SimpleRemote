package ua.com.programmer.simpleremote.ui.shared


import android.content.Context
import java.io.File
import javax.inject.Inject

class FileManager @Inject constructor(context: Context) {

    private val fileDir: File? = context.filesDir

    fun getFileData(fileName: String): String {
        if (fileName.isEmpty()) return ""
        val file = File(fileDir, fileName)
        if (!file.exists()) return ""
        val fileContent = file.readBytes()
        return android.util.Base64.encodeToString(fileContent, android.util.Base64.DEFAULT)
    }
}