package ua.com.programmer.simpleremote.http.entity

import ua.com.programmer.simpleremote.entity.Product

data class ProductResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<Product?> = emptyList()
)

fun ProductResponse.isSuccessful(): Boolean {
    return result == "ok"
}

