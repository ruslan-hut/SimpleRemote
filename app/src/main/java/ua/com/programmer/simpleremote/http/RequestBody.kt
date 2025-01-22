package ua.com.programmer.simpleremote.http

data class RequestBody(
    val userID: String = "",
    val type: String = "",
    val data: String = "",
)
