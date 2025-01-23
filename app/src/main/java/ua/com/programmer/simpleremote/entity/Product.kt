package ua.com.programmer.simpleremote.entity

data class Product(
    val type: String = "goods",
    val id: String = "",
    val isGroup: Int = 0,
    val code: String = "",
    val description: String = "",
    val art: String = "",
    val unit: String = "",
    val groupName: String = "",
    val groupCode: String = "",
    val rest: Int = 0,
    val price: Int = 0,
    val barcode: String = ""
)
