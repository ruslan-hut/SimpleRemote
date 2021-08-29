package ua.com.programmer.simpleremote.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.specialItems.DocumentField;
import ua.com.programmer.simpleremote.utility.Utils;

public class AppSettings {

    private static SharedPreferences sharedPreferences;
    private static String userID = "";

    private static final ArrayList<DataBaseItem> allowedDocuments = new ArrayList<>();
    private static final ArrayList<DataBaseItem> allowedCatalogs = new ArrayList<>();

    private static final ArrayList<DocumentField> documentFilter = new ArrayList<>();

    private static AppSettings appSettings;

    private AppSettings(){}

    public static AppSettings getInstance(Context context){
        if (appSettings == null) appSettings = new AppSettings();
        if (sharedPreferences == null) sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appSettings;
    }

    public String getUserID(){
        if (userID.equals("")){
            userID = sharedPreferences.getString("userID","");
        }
        if (userID == null || userID.equals("")){
            //generate new random userID
            userID = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("userID",userID);
            editor.apply();
        }
        if (userID.length() < 8) {
            userID = "00000000";
        }
        return userID;
    }

    public String getAuthToken(){
        return sharedPreferences.getString("authToken","");
    }

    public void setAuthToken(String token){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("authToken", token);
        editor.apply();
    }

    public void setDemoMode(boolean demoMode){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("demoMode", demoMode);
        editor.apply();
    }

    public void setAutoConnectMode(boolean autoConnect){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("autoConnect", autoConnect);
        editor.apply();
    }

    public boolean autoConnect() { return sharedPreferences.getBoolean("autoConnect", false); }

    public boolean demoMode(){
        String server = getServerAddress();
        if (server != null){
            return server.equals("demo");
        }
        return false;
    }

    public String getServerAddress() {
        return sharedPreferences.getString("serverAddress", "");
    }

    public String getDatabaseName() {
        return sharedPreferences.getString("databaseName", "");
    }

    public String getUserName() {
        return sharedPreferences.getString("userName", "");
    }

    public String getUserPassword() {
        return sharedPreferences.getString("password", "");
    }

    public int getConnectionID() {
        return sharedPreferences.getInt("connectionID", 0);
    }

    public void setConnectionID(int id){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("connectionID", id);
        editor.apply();
    }

    public String getCurrentConnectionAlias() {
        return sharedPreferences.getString("currentConnection", "");
    }

    public void setCurrentConnectionAlias(String alias){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("currentConnection", alias);
        editor.apply();
    }

    public void setDatabaseName(String databaseName){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("databaseName", databaseName);
        editor.apply();
    }

    public void setServerAddress(String serverAddress){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("serverAddress", serverAddress);
        editor.apply();
    }

    public void setUserName(String userName){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userName", userName);
        editor.apply();
    }

    public void setPassword(String password){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("password", password);
        editor.apply();
    }

    public void setAllowedDocuments(ArrayList<DataBaseItem> arrayList){
        allowedDocuments.clear();
        allowedDocuments.addAll(arrayList);
    }

    public void setAllowedCatalogs(ArrayList<DataBaseItem> arrayList){
        allowedCatalogs.clear();
        allowedCatalogs.addAll(arrayList);
    }

    public static ArrayList<DataBaseItem> getAllowedDocuments(){
        return allowedDocuments;
    }

    public static ArrayList<DataBaseItem> getAllowedCatalogs() {
        return allowedCatalogs;
    }

    public void setDocumentFilter(ArrayList<DocumentField> elements){
        documentFilter.clear();
        documentFilter.addAll(elements);
    }

    public static ArrayList<DocumentField> getDocumentFilter(){
        return documentFilter;
    }

    public String getDocumentFilterAsString(){
        Utils utils = new Utils();
        JSONObject jsonObject = new JSONObject();
        JSONArray filter = new JSONArray();
        try {
            for (int i = 0; i < documentFilter.size(); i++) {
                JSONObject element = new JSONObject(documentFilter.get(i).asString());
                filter.put(element);
            }
            jsonObject.put("filter",filter);
        }catch (JSONException ex){
            utils.log("e","getDocumentFilterAsString: "+ex.toString());
        }
        return jsonObject.toString();
    }
}
