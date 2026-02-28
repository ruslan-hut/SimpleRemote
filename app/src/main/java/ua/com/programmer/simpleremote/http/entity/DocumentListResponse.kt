package ua.com.programmer.simpleremote.http.entity

import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.FilterItem

data class DocumentListResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<Document?> = emptyList(),
    val filter: List<FilterItem> = emptyList(),
)

fun DocumentListResponse.isSuccessful(): Boolean {
    return result == "ok"
}