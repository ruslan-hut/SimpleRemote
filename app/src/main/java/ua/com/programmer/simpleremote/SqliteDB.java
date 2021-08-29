package ua.com.programmer.simpleremote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import ua.com.programmer.simpleremote.settings.AppSettings;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.utility.Utils;

public class SqliteDB {

    private static SQLiteDatabase database;

    private static SqliteDB db;
    private static int connectionID;

    private final Utils utils = new Utils();

    private SqliteDB(){}

    public static SqliteDB getInstance(Context context){
        if (db == null){
            db = new SqliteDB();
            connectionID = AppSettings.getInstance(context).getConnectionID();
        }
        if (database == null){
            SqliteDatabaseHelper sqliteDatabaseHelper = new SqliteDatabaseHelper(context);
            database = sqliteDatabaseHelper.getWritableDatabase();
        }
        return db;
    }

    ArrayList<DataBaseItem> getConnections(){
        ArrayList<DataBaseItem> resultArray = new ArrayList<>();
        Cursor cursor = database.query("connections",null,null,null,null,null,null);
        if (cursor != null && cursor.moveToFirst()){
            do {
                resultArray.add(new DataBaseItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return resultArray;
    }

    public void updateSettings(DataBaseItem item){
        String alias = item.getString("alias");
        if (alias.equals("")){
            return;
        }
        Cursor cursor = database.query("connections", null, "alias=?",new String[]{alias}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            database.update("connections", item.getValues(), "alias=?", new String[]{alias});
            cursor.close();
        }else {
            database.insert("connections", null, item.getValues());
        }
    }

    public void deleteSettings(String alias){
        database.delete("connections","alias=?",new String[]{alias});
    }

    void cacheData(String guid,String data){
        ContentValues cv = new ContentValues();
        cv.put("guid",guid);
        cv.put("data",data);
        cv.put("time",utils.currentDate());
        cv.put("connection_id",connectionID);
        String[] whereArgs = new String[]{guid};
        Cursor cursor = database.query("cache",null,"guid=?",whereArgs,null,null,null);
        if (cursor != null && cursor.moveToFirst()) {
            database.update("cache", cv, "guid=?", whereArgs);
            cursor.close();
        }else {
            database.insert("cache", null,cv);
        }
    }

    void deleteCachedData(String guid){
        database.delete("cache","guid=?",new String[]{guid});
    }

    String getCachedData(String guid){
        String result="";
        String[] whereArgs = new String[]{guid};
        Cursor cursor = database.query("cache",null,"guid=?",whereArgs,null,null,null);
        if (cursor != null && cursor.moveToFirst()) {
            result = cursor.getString(cursor.getColumnIndex("data"));
            cursor.close();
        }
        return result;
    }

    ArrayList<DataBaseItem> getCachedDataList(){
        ArrayList<DataBaseItem> resultArray = new ArrayList<>();
        Cursor cursor = database.query("cache",null,"connection_id="+connectionID,null,null,null,null);
        if (cursor != null && cursor.moveToFirst()){
            do {
                resultArray.add(new DataBaseItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return resultArray;
    }
}
