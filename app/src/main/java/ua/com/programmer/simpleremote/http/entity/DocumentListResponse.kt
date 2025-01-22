package ua.com.programmer.simpleremote.http.entity

import ua.com.programmer.simpleremote.entity.Document

data class DocumentListResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<Document?> = emptyList()
)

fun DocumentListResponse.isSuccessful(): Boolean {
    return result == "ok"
}