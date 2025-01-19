package ua.com.programmer.simpleremote.deprecated.specialItems

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ua.com.programmer.simpleremote.deprecated.utility.Utils
import java.util.Objects

class DocumentField {
    private var meta: String? = null
    var type: String? = null
    var name: String? = null
    var description: String? = null
    var code: String? = null
    var value: String? = null

    private val utils = Utils()

    constructor(init: String) {
        setDefaults()

        if (init == "") return

        try {
            val jsonObject = JSONObject(init)
            initializeFromJSON(jsonObject)
        } catch (ex: JSONException) {
            utils.log("e", "DocumentField init from string: $ex")
        }
    }

    constructor(jsonObject: JSONObject) {
        setDefaults()
        initializeFromJSON(jsonObject)
    }

    private fun setDefaults() {
        meta = "unknown"
        type = ""
        name = ""
        description = ""
        code = ""
        value = ""
    }

    private fun initializeFromJSON(jsonObject: JSONObject) {
        val columnNames = jsonObject.names()
        for (i in 0 until Objects.requireNonNull<JSONArray?>(columnNames).length()) {
            try {
                val columnName = columnNames!!.getString(i)
                val elementValue = jsonObject.getString(columnName)

                if (columnName == "meta") meta = elementValue
                if (columnName == "name") name = elementValue
                if (columnName == "type") type = elementValue
                if (columnName == "description") description = elementValue
                if (columnName == "code") code = elementValue
                if (columnName == "value") value = elementValue
            } catch (ex: JSONException) {
                utils.log("e", "DocumentField init from JSON: $ex")
            }
        }
    }

    fun getNamedValue(): String {
        return "$description: $value"
    }

    fun hasValue(): Boolean {
        return value != ""
    }

    fun isReal(): Boolean {
        return name != ""
    }

    fun asString(): String {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("meta", meta)
            jsonObject.put("type", type)
            jsonObject.put("name", name)
            jsonObject.put("description", description)
            jsonObject.put("code", code)
            jsonObject.put("value", value)
        } catch (ex: JSONException) {
            utils.log("e", "DocumentField:asString: $ex")
        }
        return jsonObject.toString()
    }

    fun isCatalog(): Boolean {
        return meta == "reference"
    }

    fun isDate(): Boolean {
        return meta == "date"
    }
}
