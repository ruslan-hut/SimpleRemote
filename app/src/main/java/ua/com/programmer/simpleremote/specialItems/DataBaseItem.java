package ua.com.programmer.simpleremote.specialItems;

import android.content.ContentValues;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import ua.com.programmer.simpleremote.Utils;

public class DataBaseItem {

    private final ContentValues itemValues;
    private final Utils utils = new Utils();

    public DataBaseItem(ContentValues values){
        itemValues = values;
    }

    public DataBaseItem(){ itemValues = new ContentValues(); }

    public DataBaseItem(Cursor cursor) {
        int columnIndex;
        String[] columnNames = cursor.getColumnNames();
        ContentValues values = new ContentValues();
        for (String columnName: columnNames) {
            columnIndex = cursor.getColumnIndex(columnName);
            switch (cursor.getType(columnIndex)){
                case Cursor.FIELD_TYPE_STRING:
                    values.put(columnName, cursor.getString(columnIndex));
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(columnName, cursor.getLong(columnIndex));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values.put(columnName, cursor.getDouble(columnIndex));
                    break;
            }
        }
        itemValues = values;
    }

    public DataBaseItem(JSONObject jsonObject) {
        JSONArray columnNames = jsonObject.names();
        ContentValues values = new ContentValues();
        for (int i = 0; i < Objects.requireNonNull(columnNames).length(); i++) {
            try {
                String columnName = columnNames.getString(i);
                Object obj = jsonObject.get(columnName);
                if (obj instanceof String) {
                    values.put(columnName, jsonObject.getString(columnName));
                }else if (obj instanceof Integer){
                    values.put(columnName, jsonObject.getInt(columnName));
                }else if (obj instanceof Long){
                    values.put(columnName, jsonObject.getLong(columnName));
                }else if (obj instanceof Double){
                    values.put(columnName, jsonObject.getDouble(columnName));
                }else if (obj instanceof Boolean) {
                    values.put(columnName, jsonObject.getBoolean(columnName));
                }else {
                    values.put(columnName, obj.toString());
                }
            }catch (JSONException ex){
                utils.log("e", "DatabaseItem init from JSON: "+ex.toString());
            }
        }
        itemValues = values;
    }

    public void newDocumentDataItem(String type){
        String guid = "!"+ UUID.randomUUID().toString();
        itemValues.put("number", "000000");
        itemValues.put("type", type);
        itemValues.put("date", utils.currentDate());
        itemValues.put("guid", guid);
        itemValues.put("contractor", "");
    }

    public String getString(String valueName){
        String value = "";
        if (itemValues.containsKey(valueName)){
            value = itemValues.getAsString(valueName);
        }
        return value;
    }

    public boolean hasValue(String valueName){
        if (itemValues.containsKey(valueName)){
            String value = itemValues.getAsString(valueName);
            return value != null && !value.equals("");
        }
        return false;
    }

    public Double getDouble(String valueName){
        Double value = 0.0;
        if (itemValues.containsKey(valueName) && itemValues.get(valueName) != null){
            value = itemValues.getAsDouble(valueName);
        }
        return value;
    }

    public int getInt(String valueName){
        int value = 0;
        if (itemValues.containsKey(valueName) && itemValues.get(valueName) != null){
            if (itemValues.getAsString(valueName).equals("")) return value;
            value = itemValues.getAsInteger(valueName);
        }
        return value;
    }

    public long getLong(String valueName){
        long value = 0;
        if (itemValues.containsKey(valueName) && itemValues.get(valueName) != null){
            if (itemValues.getAsString(valueName).equals("")) return value;
            value = itemValues.getAsLong(valueName);
        }
        return value;
    }

    public boolean getBoolean(String valueName){
        boolean value = false;
        if (itemValues.containsKey(valueName) && itemValues.get(valueName) != null){
            value = itemValues.getAsBoolean(valueName);
        }
        return value;
    }

    public void put(String valueName, String value){
        itemValues.put(valueName, value);
    }

    public void put(String valueName, int value){
        itemValues.put(valueName, value);
    }

    public void put(String valueName, Double value){
        itemValues.put(valueName, value);
    }

    public void put(String valueName, Long value){
        itemValues.put(valueName, value);
    }

    public void put(String valueName, boolean value) { itemValues.put(valueName, value); }

    public ContentValues getValues(){
        return itemValues;
    }

    public JSONObject getAsJSON() {
        JSONObject document = new JSONObject();
        try {
            //document.put("item_type", "databaseItem");
            Set<String> keys = itemValues.keySet();
            for (String key: keys) {
                document.put(key, itemValues.get(key));
            }
        }catch (JSONException ex){
            utils.log("e", "DatabaseItem.getAsJSON: "+ex.toString());
        }
        return document;
    }
}
