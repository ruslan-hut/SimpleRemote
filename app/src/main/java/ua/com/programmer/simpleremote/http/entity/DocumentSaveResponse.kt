package ua.com.programmer.simpleremote.http.entity

data class DocumentSaveResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<SaveResult?> = emptyList(),
)

fun DocumentSaveResponse.isSuccessful(): Boolean {
    val saveResult = data.firstOrNull()
    return saveResult?.saved == "ok"
}

fun DocumentSaveResponse.readError(): String {
    val saveResult = data.firstOrNull()
    return saveResult?.error ?: ""
}