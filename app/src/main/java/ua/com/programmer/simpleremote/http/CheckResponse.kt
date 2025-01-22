package ua.com.programmer.simpleremote.http

import ua.com.programmer.simpleremote.entity.UserOptions

data class CheckResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<UserOptions?> = emptyList()
)
