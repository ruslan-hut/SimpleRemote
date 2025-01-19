package ua.com.programmer.simpleremote.deprecated

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SqliteDatabaseHelper(context: Context?) :
    SQLiteOpenHelper(context, "simple_remote_database", null, 3) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(
            "create table connections(" +
                    "_id integer primary key autoincrement," +
                    "alias text," +
                    "server_address text," +
                    "server_port integer," +
                    "database_name text," +
                    "user_name text," +
                    "user_password text);"
        )

        sqLiteDatabase.execSQL(
            "create table cache(" +
                    "_id integer primary key autoincrement," +
                    "guid text," +
                    "connection_id integer," +
                    "time text," +
                    "data text);"
        )
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1) {
            sqLiteDatabase.execSQL(
                "create table cache(" +
                        "_id integer primary key autoincrement," +
                        "guid text," +
                        "time text," +
                        "data text);"
            )
        }
        if (oldVersion <= 2) {
            sqLiteDatabase.execSQL("alter table cache add column connection_id integer")
        }
    }
}
