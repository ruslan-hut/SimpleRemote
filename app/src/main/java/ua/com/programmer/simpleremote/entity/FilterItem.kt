package ua.com.programmer.simpleremote.entity

data class FilterItem(
    val meta: String = "",
    val type: String = "",
    val name: String = "",
    val description: String = "",
    var code: String = "",
    var value: String = "",
) {
    fun isSet(): Boolean = code.isNotEmpty() || value.isNotEmpty()

    fun displayValue(): String = value.ifEmpty { code }

    fun clearValue() {
        code = ""
        value = ""
    }
}

fun List<FilterItem>.isAnyFilterSet(): Boolean = any { it.isSet() }

fun List<FilterItem>.getFilterDisplayString(): String =
    filter { it.isSet() }
        .joinToString(", ") { "${it.name}: ${it.displayValue()}" }
