package ua.com.programmer.simpleremote.http

data class CheckRequest(
    val userID: String = "",
    val type: String = "",
    val data: String = "", // for compatibility, always empty
)
