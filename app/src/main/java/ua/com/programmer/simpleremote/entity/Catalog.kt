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

fun Catalog.toContent(lineNumber: Int = 1): Content {
    return Content(
        line = lineNumber,
        code = this.code,
        code2 = "", // no barcode in Catalog
        code3 = this.id,
        art = this.art,
        description = this.description,
        unit = this.unit,
        quantity = "1", // default scanned quantity
        rest = this.getRest(),
        price = this.getPrice(),
        sum = this.getPrice(), // assuming quantity = 1
        collect = "1",
        notes = "",
        checked = true, // 1 >= 1
        modified = true,
        image = this.art,
        encodedImage = "",
        place = emptyList()
    )
}
