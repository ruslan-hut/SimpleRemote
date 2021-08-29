package ua.com.programmer.simpleremote;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ua.com.programmer.simpleremote.settings.AppSettings;
import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.specialItems.DocumentField;
import ua.com.programmer.simpleremote.utility.Utils;

class DataLoader {

    private final String USER_PASSWORD;
    private final String SERVER_URL;
    private final String USER_ID;
    private String AUTH_TOKEN;
    private final Utils utils = new Utils();
    private final AppSettings appSettings;
    private final SqliteDB database;

    private final RequestQueue requestQueue;
    private final ArrayList<DataBaseItem> items = new ArrayList<>();
    private static String currentRequestTAG;

    private final String textConnectionError;

    public interface DataLoaderListener{
        void onDataLoaded(ArrayList<DataBaseItem> dataItems);
        void onDataLoaderError(String error);
    }

    private static DataLoaderListener listener;

    DataLoader(Context context){
        listener = (DataLoaderListener) context;
        requestQueue = Volley.newRequestQueue(context);
        if (currentRequestTAG != null){
            currentRequestTAG = UUID.randomUUID().toString();
        }

        database = SqliteDB.getInstance(context);

        appSettings = AppSettings.getInstance(context);
        if (appSettings.demoMode()) {
            USER_PASSWORD = "Помощник:12qwaszx";
            SERVER_URL = "http://193.0.247.122:88/simple/hs/rc";
        }else{
            USER_PASSWORD = appSettings.getUserName()+":"+appSettings.getUserPassword();
            SERVER_URL = "http://"+appSettings.getServerAddress()+"/"+appSettings.getDatabaseName()+"/hs/rc";
        }
        USER_ID = appSettings.getUserID();
        AUTH_TOKEN = appSettings.getAuthToken();

        textConnectionError = context.getString(R.string.error_unknown);
    }

    private HashMap<String, String> authHeaders(){
        HashMap<String, String> headers = new HashMap<>();
        String auth = "Basic " + Base64.encodeToString(USER_PASSWORD.getBytes(), Base64.NO_WRAP);
        headers.put("Authorization", auth);
        return headers;
    }

    private void loadDataFromResponse(JSONObject response){

        String responseString = response.toString();
        if (responseString.length() < 500) {
            utils.debug("<<< " + responseString);
        }else{
            utils.debug("<<< "+responseString.substring(0,500)+"...");
        }

        if (response.has("result")) {
            try {
                //==============================================
                //   processing response from server
                //==============================================
                if (response.getString("result").equals("ok")) {

                    //==============================================
                    //   save authentication token
                    //==============================================
                    if (!response.getString("token").equals(AUTH_TOKEN)){
                        AUTH_TOKEN = response.getString("token");
                        appSettings.setAuthToken(AUTH_TOKEN);
                    }

                    //==============================================
                    //   parse data array
                    //
                    //==============================================
                    // main data set
                    //==============================================
                    JSONArray dataSet = response.getJSONArray("data");
                    for (int i = 0; i < dataSet.length(); i++) {
                        items.add(new DataBaseItem(dataSet.getJSONObject(i)));
                    }

                    //==============================================
                    // documents filter
                    //==============================================
                    JSONArray filterElements = response.getJSONArray("filter");
                    ArrayList<DocumentField> elements = new ArrayList<>();
                    for (int i = 0; i < filterElements.length(); i++) {
                        DocumentField element = new DocumentField(filterElements.getJSONObject(i));
                        if (element.isReal()) elements.add(element);
                    }
                    if (elements.size() != 0) appSettings.setDocumentFilter(elements);

                    //==============================================
                    //   return result to listener
                    //==============================================
                    listener.onDataLoaded(items);

                } else {
                    onDataProcessingError(response.getString("message"));
                }
            } catch (Exception ex) {
                Log.e("XBUG", "DataLoader.loadDataFromResponse: " + ex.toString());
                onDataProcessingError("Invalid response");
            }
        }else{
            //==============================================
            //  bad response structure or no response data
            //==============================================
            onDataProcessingError("Invalid response");
        }

    }

    private void onDataProcessingError(String error){
        items.clear();
        listener.onDataLoaderError(error);
    }

    private void onRequestError(VolleyError error,String type){
        String textError = error.getLocalizedMessage();
        if (error.getCause() != null){
            textError = error.getCause().getLocalizedMessage();
        }
        utils.log("w", "postDataSet("+type+") -> onRequestError: "+textError);
        onDataProcessingError(textConnectionError);
    }

    private void postDataSet(String type, String data){

        utils.debug(">>> "+type+" "+data);

        //===========================================
        //   make JSON object for sending
        //===========================================
        DataBaseItem dataSet = new DataBaseItem();
        dataSet.put("userID",USER_ID);
        dataSet.put("token",AUTH_TOKEN);
        dataSet.put("type",type);
        dataSet.put("data",data);
        JSONObject jsonObject = dataSet.getAsJSON();

        //===========================================
        //    request url is always the same
        //===========================================
        String url = SERVER_URL+"/pst/"+USER_ID;

        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                this::loadDataFromResponse,
                (VolleyError error) -> onRequestError(error,type)){

            @Override
            public Map<String, String> getHeaders() {
                return authHeaders();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(5000,3,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    void getAllowedCatalogsTypes(){
        postDataSet("allowed","catalogs");
    }

    void getDocuments(String documentType){
        items.clear();
        if (documentType.equals(Constants.CACHED_DOCUMENTS)) {
            ArrayList<DataBaseItem> cachedItems = database.getCachedDataList();
            for (DataBaseItem cacheItem: cachedItems){
                try {
                    DataBaseItem item = new DataBaseItem(new JSONObject(cacheItem.getString("data")));
                    item.put(Constants.CACHE_GUID,cacheItem.getString("guid"));
                    items.add(item);
                }catch (JSONException jsonException){
                    utils.log("w","getDocuments; JSONException: "+jsonException.toString());
                }
            }
            listener.onDataLoaded(items);
        }else{
            DataBaseItem item = new DataBaseItem();
            item.put("type",documentType);
            item.put("filter",appSettings.getDocumentFilterAsString());
            postDataSet(Constants.DOCUMENTS,item.getAsJSON().toString());
        }
    }

    void getDocumentContent(DataBaseItem documentDataItem){
        items.clear();
        if (documentDataItem.hasValue(Constants.CACHE_GUID)) {
            try {
                JSONArray linesArray = new JSONArray(documentDataItem.getString("lines"));
                for (int i = 0; i < linesArray.length(); i++) {
                    items.add(new DataBaseItem(linesArray.getJSONObject(i)));
                }
            }catch (JSONException jsonException){
                utils.log("w","getDocumentContent; JSONException: "+jsonException.toString());
            }
            listener.onDataLoaded(items);
        }else {
            String documentData = documentDataItem.getAsJSON().toString();
            postDataSet("documentContent", documentData);
        }
    }

    void saveDocument(String document){
        postDataSet(Constants.ACTION_SAVE_DOCUMENT, document);
    }

    void getCatalogData(String catalogType, String groupCode, String searchFilter, String documentGUID){

        DataBaseItem parameters = new DataBaseItem();
        parameters.put("type",catalogType);
        parameters.put("group",groupCode);
        parameters.put("searchFilter",searchFilter);
        parameters.put("documentGUID",documentGUID);

        postDataSet("catalog", parameters.getAsJSON().toString());
    }

    void getItemWithBarcode(DataBaseItem item){
        postDataSet("barcode", item.getAsJSON().toString());
    }

    void checkConnection(){
        postDataSet("check","");
    }

}
