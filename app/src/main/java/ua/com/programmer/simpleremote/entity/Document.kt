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
    val field1: DocumentField = DocumentField(),
    val field2: DocumentField = DocumentField(),
    val field3: DocumentField = DocumentField(),
    val field4: DocumentField = DocumentField(),
    val repeated: String = "",
    val currency: String = "",
    val cacheGUID: String = ""
)
