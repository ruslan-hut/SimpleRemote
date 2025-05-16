package ua.com.programmer.simpleremote.entity

data class Content(
    val line: Int = 0,
    val code: String = "",
    val code2: String = "",
    val code3: String = "",
    val art: String = "",
    val description: String = "",
    val unit: String = "",
    val quantity: String = "",
    val rest: String = "",
    val price: String = "",
    val sum: String = "",

    // editable fields
    var collect: String = "",
    var notes: String = "",
    var checked: Boolean = false,
    var modified: Boolean = false,
    var image: String = "",
    var encodedImage: String = "",
    var place: List<Place> = emptyList(),
)

data class Place(
    var quantity: Int = 0,
    var code: String = "",
)

fun Content.isEquals(other: Content): Boolean {
    return this.code == other.code
            && this.modified == other.modified
            && this.checked == other.checked
            && this.collect == other.collect
            && this.notes == other.notes
}