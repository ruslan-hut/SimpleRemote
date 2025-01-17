package ua.com.programmer.simpleremote;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class SqliteDatabaseHelper extends SQLiteOpenHelper {

    SqliteDatabaseHelper (Context context){
        super(context, "simple_remote_database", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table connections(" +
                "_id integer primary key autoincrement," +
                "alias text," +
                "server_address text," +
                "server_port integer," +
                "database_name text," +
                "user_name text," +
                "user_password text);");

        sqLiteDatabase.execSQL("create table cache(" +
                "_id integer primary key autoincrement," +
                "guid text," +
                "connection_id integer," +
                "time text," +
                "data text);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion == 1){
            sqLiteDatabase.execSQL("create table cache(" +
                    "_id integer primary key autoincrement," +
                    "guid text," +
                    "time text," +
                    "data text);");
        }
        if (oldVersion <= 2){
            sqLiteDatabase.execSQL("alter table cache add column connection_id integer");
        }
    }
}
