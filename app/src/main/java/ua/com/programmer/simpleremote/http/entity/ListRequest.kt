package ua.com.programmer.simpleremote.http.entity

import ua.com.programmer.simpleremote.entity.FilterItem

data class ListRequest(
    val userID: String = "",
    val type: String = "",
    val data: Any? = null,
    val filter: List<FilterItem>? = null,
)