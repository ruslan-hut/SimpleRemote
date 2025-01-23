package ua.com.programmer.simpleremote.entity

data class Content(
    val line: Int = 0,
    val code: String = "",
    val code2: String = "",
    val code3: String = "",
    val art: String = "",
    val description: String = "",
    val notes: String = "",
    var checked: Boolean = false,
    var modified: Boolean = false,
    val unit: String = "",
    val quantity: String = "",
    var collect: String = "",
    val rest: String = "",
    val price: String = "",
    val sum: String = "",
    val image: String = ""
)
