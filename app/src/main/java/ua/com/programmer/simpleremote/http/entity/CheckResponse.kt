package ua.com.programmer.simpleremote.http.entity

import ua.com.programmer.simpleremote.entity.UserOptions

data class CheckResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<UserOptions?> = emptyList()
)