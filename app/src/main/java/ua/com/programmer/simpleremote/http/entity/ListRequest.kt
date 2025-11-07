package ua.com.programmer.simpleremote.http.entity

data class ListRequest(
    val userID: String = "",
    val type: String = "",
    val data: String = "",
    val filter: String = "",
)