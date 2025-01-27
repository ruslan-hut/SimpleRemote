package ua.com.programmer.simpleremote.entity

data class DocumentField(
    val meta: String = "",
    val type: String = "",
    val name: String = "",
    val description: String = "",
    val code: String = "",
    val value: String = "",
)

fun DocumentField.isEmpty(): Boolean {
    return code.isEmpty() && value.isEmpty()
}