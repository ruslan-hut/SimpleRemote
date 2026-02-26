package ua.com.programmer.simpleremote.http.entity

import ua.com.programmer.simpleremote.entity.Content

data class DocumentContentResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<Content?> = emptyList()
)

fun DocumentContentResponse.isSuccessful(): Boolean {
    return result == "ok"
}