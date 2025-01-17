package ua.com.programmer.simpleremote.specialItems

import android.content.ContentValues
import android.database.Cursor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ua.com.programmer.simpleremote.utility.Utils
import java.util.Objects
import java.util.UUID

class DataBaseItem {
    private val itemValues: ContentValues
    private val utils = Utils()

    constructor(values: ContentValues) {
        itemValues = values
    }

    constructor() {
        itemValues = ContentValues()
    }

    constructor(cursor: Cursor) {
        var columnIndex: Int
        val columnNames = cursor.getColumnNames()
        val values = ContentValues()
        for (columnName in columnNames) {
            columnIndex = cursor.getColumnIndex(columnName)
            when (cursor.getType(columnIndex)) {
                Cursor.FIELD_TYPE_STRING, Cursor.FIELD_TYPE_NULL, Cursor.FIELD_TYPE_BLOB -> values.put(
                    columnName,
                    cursor.getString(columnIndex)
                )

                Cursor.FIELD_TYPE_INTEGER -> values.put(columnName, cursor.getLong(columnIndex))
                Cursor.FIELD_TYPE_FLOAT -> values.put(columnName, cursor.getDouble(columnIndex))
            }
        }
        itemValues = values
    }

    constructor(jsonObject: JSONObject) {
        val columnNames = jsonObject.names()
        val values = ContentValues()
        for (i in 0 until Objects.requireNonNull<JSONArray?>(columnNames).length()) {
            try {
                val columnName = columnNames!!.getString(i)
                val obj = jsonObject.get(columnName)
                if (obj is String) {
                    values.put(columnName, jsonObject.getString(columnName))
                } else if (obj is Int) {
                    values.put(columnName, jsonObject.getInt(columnName))
                } else if (obj is Long) {
                    values.put(columnName, jsonObject.getLong(columnName))
                } else if (obj is Double) {
                    values.put(columnName, jsonObject.getDouble(columnName))
                } else if (obj is Boolean) {
                    values.put(columnName, jsonObject.getBoolean(columnName))
                } else {
                    values.put(columnName, obj.toString())
                }
            } catch (ex: JSONException) {
                utils.log("e", "DatabaseItem init from JSON: " + ex)
            }
        }
        itemValues = values
    }

    fun newDocumentDataItem(type: String?) {
        val guid = "!" + UUID.randomUUID().toString()
        itemValues.put("number", "000000")
        itemValues.put("type", type)
        itemValues.put("date", utils.currentDate())
        itemValues.put("guid", guid)
        itemValues.put("contractor", "")
    }

    /**
     * Read string value
     *
     * @param valueName name of the parameter
     * @return parameter value
     */
    fun getString(valueName: String?): String {
        var value: String? = ""
        if (valueName == null) return value!!
        if (itemValues.containsKey(valueName)) {
            value = itemValues.getAsString(valueName)
        }
        if (value == null) value = ""
        if (value == "null") value = ""
        return value
    }

    fun hasValue(valueName: String?): Boolean {
        return !getString(valueName).isEmpty()
    }

    fun getDouble(valueName: String?): Double {
        var value: Double = 0.0
        if (valueName == null) return value
        if (getString(valueName).isEmpty()) return value
        value = itemValues.getAsDouble(valueName)
        return value
    }

    fun getInt(valueName: String?): Int {
        var value = 0
        if (valueName == null) return value
        if (getString(valueName).isEmpty()) return value
        value = itemValues.getAsInteger(valueName)
        return value
    }

    fun getBoolean(valueName: String?): Boolean {
        var value = false
        if (itemValues.containsKey(valueName) && itemValues.get(valueName) != null) {
            value = itemValues.getAsBoolean(valueName)
        }
        return value
    }

    fun put(valueName: String?, value: String?) {
        itemValues.put(valueName, value)
    }

    fun put(valueName: String?, value: Int) {
        itemValues.put(valueName, value)
    }

    fun put(valueName: String?, value: Double?) {
        itemValues.put(valueName, value)
    }

    fun put(valueName: String?, value: Long?) {
        itemValues.put(valueName, value)
    }

    fun put(valueName: String?, value: Boolean) {
        itemValues.put(valueName, value)
    }

    fun getValues(): ContentValues {
        return itemValues
    }

    fun getAsJSON(): JSONObject {
        val document = JSONObject()
        try {
            //document.put("item_type", "databaseItem");
            val keys = itemValues.keySet()
            for (key in keys) {
                document.put(key, itemValues.get(key))
            }
        } catch (ex: JSONException) {
            utils.log("e", "DatabaseItem.getAsJSON: " + ex)
        }
        return document
    }
}
