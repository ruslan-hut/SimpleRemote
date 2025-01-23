package ua.com.programmer.simpleremote.entity

data class Document(
    val guid: String = "",
    val isProcessed: Int = 0,
    val isDeleted: Int = 0,
    val title: String = "",
    val number: String = "",
    val date: String = "",
    val company: String = "",
    val contractor: String = "",
    val warehouse: String = "",
    val sum: String = "",
    val checked: Boolean = false,
    val notes: String = "",
    val field1: String = "",
    val field2: String = "",
    val field3: String = "",
    val field4: String = "",
    val repeated: String = "",
    val currency: String = "",
    val cacheGUID: String = ""
)
