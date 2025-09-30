package ua.com.programmer.simpleremote.entity

data class Product(
    val type: String = "goods",
    val id: String = "",
    val isGroup: Int = 0,
    val code: String = "",
    val description: String = "",
    val notes: String = "",
    val art: String = "",
    val unit: String = "",
    val groupName: String = "",
    val groupCode: String = "",
    val rest: Double = 0.0,
    val price: Double = 0.0,
    val barcode: String = "",
    val contentItem: Content? = null,
)

fun Product.setImage(image: String): Product {
    return this.copy(contentItem = this.contentItem?.copy(userImage = image))
}

fun Product.getImage(): String {
    return this.contentItem?.userImage ?: ""
}
