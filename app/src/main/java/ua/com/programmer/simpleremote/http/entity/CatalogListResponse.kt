package ua.com.programmer.simpleremote.http.entity

import ua.com.programmer.simpleremote.entity.Catalog

data class CatalogListResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<Catalog?> = emptyList()
)

fun CatalogListResponse.isSuccessful(): Boolean {
    return result == "ok"
}

