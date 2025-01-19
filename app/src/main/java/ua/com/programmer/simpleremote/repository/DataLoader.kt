package ua.com.programmer.simpleremote.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.deprecated.SqliteDB
import ua.com.programmer.simpleremote.deprecated.settings.AppSettings
import ua.com.programmer.simpleremote.deprecated.settings.Constants
import ua.com.programmer.simpleremote.deprecated.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.deprecated.specialItems.DocumentField
import ua.com.programmer.simpleremote.deprecated.utility.Utils
import java.lang.Exception
import java.util.UUID

class DataLoader(context: Context) {
    private var USER_PASSWORD: String? = null
    private var SERVER_URL: String? = null
    private val USER_ID: String?
    private var AUTH_TOKEN: String?
    private val utils = Utils()
    private val appSettings: AppSettings
    private val database: SqliteDB?

    private val requestQueue: RequestQueue
    private val items = ArrayList<DataBaseItem?>()
    private val textConnectionError: String

    interface DataLoaderListener {
        fun onDataLoaded(dataItems: java.util.ArrayList<DataBaseItem?>?)
        fun onDataLoaderError(error: String?)
    }

    init {
        listener = context as DataLoaderListener
        requestQueue = Volley.newRequestQueue(context)
        if (currentRequestTAG != null) {
            currentRequestTAG = UUID.randomUUID().toString()
        }

        database = SqliteDB.Companion.getInstance(context)

        appSettings = AppSettings.Companion.getInstance(context)!!
        if (appSettings.demoMode()) {
            USER_PASSWORD = "Помощник:12qwaszx"
            SERVER_URL = "http://hoot.com.ua/simple/hs/rc"
        } else {
            USER_PASSWORD = appSettings.getUserName() + ":" + appSettings.getUserPassword()
            SERVER_URL =
                "http://" + appSettings.getServerAddress() + "/" + appSettings.getDatabaseName() + "/hs/rc"
        }
        USER_ID = appSettings.getUserID()
        AUTH_TOKEN = appSettings.getAuthToken()

        textConnectionError = context.getString(R.string.error_unknown)
    }

    private fun authHeaders(): java.util.HashMap<String?, String?> {
        val headers = HashMap<String?, String?>()
        val auth = "Basic " + Base64.encodeToString(USER_PASSWORD!!.toByteArray(), Base64.NO_WRAP)
        headers.put("Authorization", auth)
        return headers
    }

    private fun loadDataFromResponse(response: JSONObject?) {
        if (response == null) {
            onDataProcessingError("Response is NULL")
            return
        }

        val responseString = response.toString()
        if (responseString.length < 500) {
            utils.debug("<<< $responseString")
        } else {
            utils.debug("<<< " + responseString.substring(0, 500) + "...")
        }

        if (response.has("result")) {
            try {
                //==============================================
                //   processing response from server
                //==============================================
                if (response.getString("result") == "ok") {
                    //==============================================
                    //   save authentication token
                    //==============================================

                    if (response.getString("token") != AUTH_TOKEN) {
                        AUTH_TOKEN = response.getString("token")
                        appSettings.setAuthToken(AUTH_TOKEN)
                    }

                    //==============================================
                    //   parse data array
                    //
                    //==============================================
                    // main data set
                    //==============================================
                    val dataSet = response.getJSONArray("data")
                    for (i in 0 until dataSet.length()) {
                        items.add(DataBaseItem(dataSet.getJSONObject(i)))
                    }

                    //==============================================
                    // documents filter
                    //==============================================
                    val filterElements = response.getJSONArray("filter")
                    val elements = ArrayList<DocumentField?>()
                    for (i in 0 until filterElements.length()) {
                        val element = DocumentField(filterElements.getJSONObject(i))
                        if (element.isReal()) elements.add(element)
                    }
                    if (!elements.isEmpty()) appSettings.setDocumentFilter(elements)

                    //==============================================
                    //   return result to listener
                    //==============================================
                    listener.onDataLoaded(items)
                } else {
                    onDataProcessingError(response.getString("message"))
                }
            } catch (ex: Exception) {
                Log.e("XBUG", "DataLoader.loadDataFromResponse: $ex")
                onDataProcessingError("Invalid response")
            }
        } else {
            //==============================================
            //  bad response structure or no response data
            //==============================================
            onDataProcessingError("Invalid response")
        }
    }

    private fun onDataProcessingError(error: String?) {
        items.clear()
        listener.onDataLoaderError(error)
    }

    private fun onRequestError(error: VolleyError, type: String?) {
        var textError = error.message
        if (error.cause != null) {
            textError = error.cause!!.getLocalizedMessage()
        }
        utils.log("w", "postDataSet(" + type + ") -> onRequestError: " + textError)
        onDataProcessingError(textConnectionError)
    }

    private fun postDataSet(type: String?, data: String?) {
        utils.debug(">>> " + type + " " + data)

        //===========================================
        //   make JSON object for sending
        //===========================================
        val dataSet = DataBaseItem()
        dataSet.put("userID", USER_ID)
        dataSet.put("token", AUTH_TOKEN)
        dataSet.put("type", type)
        dataSet.put("data", data)
        val jsonObject = dataSet.getAsJSON()

        //===========================================
        //    request url is always the same
        //===========================================
        val url = SERVER_URL + "/pst/" + USER_ID

        val request: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, jsonObject,
            Response.Listener { response: JSONObject? -> this.loadDataFromResponse(response) },
            Response.ErrorListener { error: VolleyError? -> onRequestError(error!!, type) }) {
            override fun getHeaders(): MutableMap<String?, String?> {
                return authHeaders()
            }
        }
        request.setRetryPolicy(DefaultRetryPolicy(15000, 3, 1.5f))

        requestQueue.add<JSONObject?>(request)
    }

    fun getAllowedCatalogsTypes() {
        postDataSet("allowed", "catalogs")
    }

    fun getDocuments(documentType: String) {
        items.clear()
        if (documentType == Constants.CACHED_DOCUMENTS) {
            val cachedItems: java.util.ArrayList<DataBaseItem?> = database!!.getCachedDataList()
            for (cacheItem in cachedItems) {
                try {
                    cacheItem?.let {
                        val item = DataBaseItem(JSONObject(it.getString("data")))
                        item.put(Constants.CACHE_GUID, it.getString("guid"))
                        items.add(item)
                    }
                } catch (jsonException: JSONException) {
                    utils.log("w", "getDocuments; JSONException: $jsonException")
                }
            }
            listener.onDataLoaded(items)
        } else {
            val item = DataBaseItem()
            item.put("type", documentType)
            item.put("filter", appSettings.getDocumentFilterAsString())
            postDataSet(Constants.DOCUMENTS, item.getAsJSON().toString())
        }
    }

    fun getDocumentContent(documentDataItem: DataBaseItem) {
        items.clear()
        if (documentDataItem.hasValue(Constants.CACHE_GUID)) {
            try {
                val linesArray = JSONArray(documentDataItem.getString("lines"))
                for (i in 0 until linesArray.length()) {
                    items.add(DataBaseItem(linesArray.getJSONObject(i)))
                }
            } catch (jsonException: JSONException) {
                utils.log("w", "getDocumentContent; JSONException: " + jsonException)
            }
            listener.onDataLoaded(items)
        } else {
            val documentData = documentDataItem.getAsJSON().toString()
            postDataSet("documentContent", documentData)
        }
    }

    fun saveDocument(document: String?) {
        postDataSet(Constants.ACTION_SAVE_DOCUMENT, document)
    }

    fun getCatalogData(
        catalogType: String?,
        groupCode: String?,
        searchFilter: String?,
        documentGUID: String?
    ) {
        val parameters = DataBaseItem()
        parameters.put("type", catalogType)
        parameters.put("group", groupCode)
        parameters.put("searchFilter", searchFilter)
        parameters.put("documentGUID", documentGUID)

        postDataSet("catalog", parameters.getAsJSON().toString())
    }

    fun getItemWithBarcode(item: DataBaseItem) {
        postDataSet("barcode", item.getAsJSON().toString())
    }

    fun checkConnection() {
        postDataSet("check", "")
    }

    companion object {
        private var currentRequestTAG: String? = null

        private lateinit var listener: DataLoaderListener
    }
}