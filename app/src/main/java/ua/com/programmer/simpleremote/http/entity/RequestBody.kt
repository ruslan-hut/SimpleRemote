package ua.com.programmer.simpleremote.http.entity

data class RequestBody(
    val userID: String = "",
    val type: String = "",
    val data: String = "",
)