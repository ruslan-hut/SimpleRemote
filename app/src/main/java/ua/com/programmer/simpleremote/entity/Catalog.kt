package ua.com.programmer.simpleremote.entity

import java.util.Locale

data class Catalog(
    val id: String = "",
    val type: String = "",
    val isGroup: Int = 0,
    val code: String = "",
    val description: String = "",
    val art: String = "",
    val unit: String = "",
    val groupName: String = "",
    val groupCode: String = "",
    val rest: Double = 0.0,
    val price: Double = 0.0,
)

fun Catalog.getRest(): String {
    return if (this.rest != 0.0) {
        this.rest.toString()
    } else {
        ""
    }
}

fun Catalog.getPrice(): String {
    return if (this.price > 0) {
        String.format(Locale.getDefault(), "%.2f", this.price)
    } else {
        ""
    }
}