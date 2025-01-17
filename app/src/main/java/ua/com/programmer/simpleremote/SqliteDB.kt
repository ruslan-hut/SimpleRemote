package ua.com.programmer.simpleremote

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import ua.com.programmer.simpleremote.settings.AppSettings
import ua.com.programmer.simpleremote.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.utility.Utils
import java.util.ArrayList

class SqliteDB private constructor() {
    private val utils = Utils()

    fun getConnections(): ArrayList<DataBaseItem?> {
        val resultArray = ArrayList<DataBaseItem?>()
        val cursor = database!!.query("connections", null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                resultArray.add(DataBaseItem(cursor))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return resultArray
    }

    fun updateSettings(item: DataBaseItem) {
        val alias = item.getString("alias")
        if (alias == "") {
            return
        }
        val cursor = database!!.query(
            "connections",
            null,
            "alias=?",
            arrayOf<String>(alias),
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            database!!.update("connections", item.getValues(), "alias=?", arrayOf<String>(alias))
            cursor.close()
        } else {
            database!!.insert("connections", null, item.getValues())
        }
    }

    fun deleteSettings(alias: String?) {
        database!!.delete("connections", "alias=?", arrayOf<String?>(alias))
    }

    fun cacheData(guid: String?, data: String?) {
        val cv = ContentValues()
        cv.put("guid", guid)
        cv.put("data", data)
        cv.put("time", utils.currentDate())
        cv.put("connection_id", connectionID)
        val whereArgs = arrayOf<String?>(guid)
        val cursor = database!!.query("cache", null, "guid=?", whereArgs, null, null, null)
        if (cursor.moveToFirst()) {
            database!!.update("cache", cv, "guid=?", whereArgs)
            cursor.close()
        } else {
            database!!.insert("cache", null, cv)
        }
    }

    fun deleteCachedData(guid: String?) {
        database!!.delete("cache", "guid=?", arrayOf<String?>(guid))
    }

    fun getCachedDataList(): ArrayList<DataBaseItem?> {
        val resultArray = ArrayList<DataBaseItem?>()
        val cursor =
            database!!.query("cache", null, "connection_id=$connectionID", null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                resultArray.add(DataBaseItem(cursor))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return resultArray
    }

    companion object {
        private var database: SQLiteDatabase? = null

        private var db: SqliteDB? = null
        private var connectionID = 0

        @JvmStatic
        fun getInstance(context: Context?): SqliteDB? {
            if (db == null) {
                db = SqliteDB()
                connectionID = AppSettings.getInstance(context)?.getConnectionID() ?: 0
            }
            if (database == null) {
                val sqliteDatabaseHelper = SqliteDatabaseHelper(context)
                database = sqliteDatabaseHelper.writableDatabase
            }
            return db
        }
    }
}
