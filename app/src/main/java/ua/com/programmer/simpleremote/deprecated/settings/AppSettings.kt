package ua.com.programmer.simpleremote.deprecated.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Base64
import com.bumptech.glide.load.model.LazyHeaders
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ua.com.programmer.simpleremote.deprecated.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.deprecated.specialItems.DocumentField
import ua.com.programmer.simpleremote.deprecated.utility.Utils
import java.util.ArrayList
import java.util.UUID

class AppSettings private constructor() {
    fun getUserID(): String {
        if (userID.isEmpty()) {
            userID = sharedPreferences!!.getString("userID", "")!!
        }
        if (userID.isEmpty()) {
            //generate new random userID
            userID = UUID.randomUUID().toString()
            val editor = sharedPreferences!!.edit()
            editor.putString("userID", userID)
            editor.apply()
        }
        if (userID.length < 8) userID = "00000000"
        return userID
    }

    fun getAuthToken(): String {
        return sharedPreferences!!.getString("authToken", "")!!
    }

    fun setAuthToken(token: String?) {
        val editor = sharedPreferences!!.edit()
        editor.putString("authToken", token)
        editor.apply()
    }

    fun setDemoMode(demoMode: Boolean) {
        val editor = sharedPreferences!!.edit()
        editor.putBoolean("demoMode", demoMode)
        editor.apply()
    }

    fun setAutoConnectMode(autoConnect: Boolean) {
        val editor = sharedPreferences!!.edit()
        editor.putBoolean("autoConnect", autoConnect)
        editor.apply()
    }

    fun autoConnect(): Boolean {
        return sharedPreferences!!.getBoolean("autoConnect", false)
    }

    fun demoMode(): Boolean {
        val server: String? = getServerAddress()
        if (server != null) {
            return server == "demo"
        }
        return false
    }

    fun getServerAddress(): String {
        return sharedPreferences!!.getString("serverAddress", "")!!
    }

    fun getDatabaseName(): String {
        return sharedPreferences!!.getString("databaseName", "")!!
    }

    fun getUserName(): String {
        return sharedPreferences!!.getString("userName", "")!!
    }

    fun getUserPassword(): String {
        return sharedPreferences!!.getString("password", "")!!
    }

    fun getAuthHeaders(): LazyHeaders {
        val namePass = getUserName() + ":" + getUserPassword()
        val authToken = "Basic " + Base64.encodeToString(namePass.toByteArray(), Base64.NO_WRAP)
        return LazyHeaders.Builder()
            .addHeader("Authorization", authToken)
            .build()
    }

    fun getBaseUrl(): String {
        val server = getServerAddress()
        val database = getDatabaseName()
        if (demoMode()) return "http://hoot.com.ua/simple/"
        return "http://$server/$database/"
    }

    fun getBaseImageUrl(): String {
        return getBaseUrl() + "hs/rc/image/"
    }

    fun getConnectionID(): Int {
        return sharedPreferences!!.getInt("connectionID", 0)
    }

    fun setConnectionID(id: Int) {
        val editor = sharedPreferences!!.edit()
        editor.putInt("connectionID", id)
        editor.apply()
    }

    fun getCurrentConnectionAlias(): String {
        return sharedPreferences!!.getString("currentConnection", "")!!
    }

    fun setCurrentConnectionAlias(alias: String?) {
        val editor = sharedPreferences!!.edit()
        editor.putString("currentConnection", alias)
        editor.apply()
    }

    fun setDatabaseName(databaseName: String?) {
        val editor = sharedPreferences!!.edit()
        editor.putString("databaseName", databaseName)
        editor.apply()
    }

    fun setServerAddress(serverAddress: String?) {
        val editor = sharedPreferences!!.edit()
        editor.putString("serverAddress", serverAddress)
        editor.apply()
    }

    fun setUserName(userName: String?) {
        val editor = sharedPreferences!!.edit()
        editor.putString("userName", userName)
        editor.apply()
    }

    fun setPassword(password: String?) {
        val editor = sharedPreferences!!.edit()
        editor.putString("password", password)
        editor.apply()
    }

    fun setAllowedDocuments(arrayList: ArrayList<DataBaseItem?>) {
        allowedDocuments.clear()
        allowedDocuments.addAll(arrayList)
    }

    fun setAllowedCatalogs(arrayList: ArrayList<DataBaseItem?>) {
        allowedCatalogs.clear()
        allowedCatalogs.addAll(arrayList)
    }

    fun setDocumentFilter(elements: ArrayList<DocumentField?>) {
        documentFilter.clear()
        val items = ArrayList<DocumentField>()
        for (element in elements) {
            items.add(element!!)
        }
        documentFilter.addAll(items)
    }

    fun getDocumentFilterAsString(): String {
        val utils = Utils()
        val jsonObject = JSONObject()
        val filter = JSONArray()
        try {
            for (i in documentFilter.indices) {
                val element = JSONObject(documentFilter.get(i)!!.asString())
                filter.put(element)
            }
            jsonObject.put("filter", filter)
        } catch (ex: JSONException) {
            utils.log("e", "getDocumentFilterAsString: " + ex)
        }
        return jsonObject.toString()
    }

    fun setLoadImages(value: Boolean) {
        loadImages = value
    }

    fun isLoadImages(): Boolean {
        return loadImages
    }

    fun setWorkingMode(mode: String?) {
        workingMode = mode
    }

    fun getWorkingMode(): String {
        return workingMode ?: ""
    }

    companion object {
        private var sharedPreferences: SharedPreferences? = null
        private var userID = ""
        private var loadImages = false
        private var workingMode: String? = ""

        private val allowedDocuments = ArrayList<DataBaseItem?>()
        private val allowedCatalogs = ArrayList<DataBaseItem?>()
        private val documentFilter = ArrayList<DocumentField>()

        private var appSettings: AppSettings? = null

        @JvmStatic
        fun getInstance(context: Context?): AppSettings? {
            if (appSettings == null) appSettings = AppSettings()
            if (sharedPreferences == null) sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            return appSettings
        }

        fun getAllowedDocuments(): ArrayList<DataBaseItem?> {
            return allowedDocuments
        }

        fun getAllowedCatalogs(): ArrayList<DataBaseItem?> {
            return allowedCatalogs
        }

        fun getDocumentFilter(): ArrayList<DocumentField> {
            return documentFilter
        }
    }
}
