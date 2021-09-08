package ua.com.programmer.simpleremote;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;

import ua.com.programmer.simpleremote.settings.AppSettings;
import ua.com.programmer.simpleremote.settings.ConnectionSettingsActivity;
import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.utility.Utils;

public class LoginActivity extends AppCompatActivity implements DataLoader.DataLoaderListener, AdapterView.OnItemSelectedListener{

    private AppSettings appSettings;
    private Spinner connectionsSpinner;
    private ConnectionsSpinnerAdapter adapter;
    private ProgressBar progressBar;

    private final Utils utils = new Utils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setResult(0);
        Cache.getInstance().clear();

        appSettings = AppSettings.getInstance(this);

        CheckBox autoConnect = findViewById(R.id.autoconnect);
        autoConnect.setChecked(appSettings.autoConnect());
        autoConnect.setOnCheckedChangeListener((CompoundButton b, boolean isChecked) ->
            appSettings.setAutoConnectMode(isChecked));

        String textVersion = BuildConfig.VERSION_NAME+" : "+appSettings.getUserID().substring(0,8);
        TextView version = findViewById(R.id.version);
        version.setText(textVersion);
        version.setOnClickListener((View view) -> {
            String logContent = utils.readLogs().toString();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, logContent);
            intent.setType("text/plain");
            startActivity(intent);
        });

        TextView loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener((View v) -> connect());
        TextView editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener((View v) -> editConnection());

        adapter = new ConnectionsSpinnerAdapter(this, android.R.layout.simple_spinner_item);
        connectionsSpinner = findViewById(R.id.current_connection);
        connectionsSpinner.setAdapter(adapter);
        connectionsSpinner.setOnItemSelectedListener(this);

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void editConnection(){
        Intent intent = new Intent(this, ConnectionSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        setSpinnerItems();
        if (appSettings.autoConnect()){
            connect();
        }
        super.onResume();
    }

    @Override
    public void onDataLoaded(ArrayList<DataBaseItem> dataItems) {
        progressBar.setVisibility(View.INVISIBLE);
        if (dataItems.size() > 0) {
            if (checkServerResponse(dataItems.get(0))) {
                setResult(1);
                finish();
            }else{
                Toast.makeText(this, R.string.error_access_denied, Toast.LENGTH_SHORT).show();
            }

        }else {
            Toast.makeText(this, R.string.error_no_data, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDataLoaderError(String error) {
        progressBar.setVisibility(View.INVISIBLE);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        //Toast.makeText(this, "Selected "+i, Toast.LENGTH_SHORT).show();
        DataBaseItem selectedItem = (DataBaseItem) connectionsSpinner.getSelectedItem();
        if (selectedItem != null){
            if (appSettings.getCurrentConnectionAlias().equals(selectedItem.getString("alias"))){
                return;
            }
            if (selectedItem.getString("action").equals("addNew")) {
                appSettings.setCurrentConnectionAlias("");
                appSettings.setServerAddress("");
                appSettings.setDatabaseName("");
                appSettings.setUserName("");
                appSettings.setPassword("");
            }else {
                appSettings.setCurrentConnectionAlias(selectedItem.getString("alias"));
                appSettings.setServerAddress(selectedItem.getString("server_address"));
                appSettings.setDatabaseName(selectedItem.getString("database_name"));
                appSettings.setUserName(selectedItem.getString("user_name"));
                appSettings.setPassword(selectedItem.getString("user_password"));
                appSettings.setConnectionID(selectedItem.getInt("_id"));
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    private void setSpinnerItems(){
        String currentAlias = appSettings.getCurrentConnectionAlias();
        adapter.clear();

        adapter.addAll(SqliteDB.getInstance(this).getConnections());

        DataBaseItem specialItem = new DataBaseItem();
        specialItem.put("alias", getResources().getString(R.string.alias_add_new));
        specialItem.put("server_address", getResources().getString(R.string.alias_add_new_hint));
        specialItem.put("action", "addNew");
        adapter.add(specialItem);
        adapter.notifyDataSetChanged();

        int position = adapter.aliasPosition(currentAlias);
        if (position >= 0){
            connectionsSpinner.setSelection(position);
        }
    }

    private void connect(){
        if (appSettings.getServerAddress().equals("")) {
            Toast.makeText(this, R.string.error_no_address, Toast.LENGTH_SHORT).show();
        }else {
            progressBar.setVisibility(View.VISIBLE);
            DataLoader dataLoader = new DataLoader(this);
            dataLoader.checkConnection();
        }
    }

    private boolean checkServerResponse(DataBaseItem response){

        ArrayList<DataBaseItem> catalogsList = new ArrayList<>();
        ArrayList<DataBaseItem> documentsList = new ArrayList<>();
        try {
            JSONArray catalogs = new JSONArray(response.getString("catalog"));
            for (int i=0; i<catalogs.length(); i++){
                catalogsList.add(new DataBaseItem(catalogs.getJSONObject(i)));
            }
            JSONArray documents = new JSONArray(response.getString("document"));
            for (int i=0; i<documents.length(); i++){
                documentsList.add(new DataBaseItem(documents.getJSONObject(i)));
            }
        }catch (Exception e){
            utils.log("e","checkServerResponse: "+e.toString());
            utils.log("e","response: "+response.getAsJSON());
        }
        if (documentsList.size() > 0){
            DataBaseItem cacheList = new DataBaseItem();
            cacheList.put("description",getResources().getString(R.string.cached_list));
            cacheList.put("code", Constants.CACHED_DOCUMENTS);
            documentsList.add(cacheList);
        }
        appSettings.setAllowedCatalogs(catalogsList);
        appSettings.setAllowedDocuments(documentsList);

        //reset documents filter
        appSettings.setDocumentFilter(new ArrayList<>());

        appSettings.setLoadImages(response.getBoolean("loadImages"));

        return response.getBoolean("read");
    }

    private class ConnectionsSpinnerAdapter extends ArrayAdapter<DataBaseItem>{

        ConnectionsSpinnerAdapter(Context context, int resID){
            super(context, resID);
        }

        int aliasPosition(String alias){
            for (int i=0; i<getCount(); i++){
                DataBaseItem item = getItem(i);
                if (item != null && item.getString("alias").equals(alias)){
                    return i;
                }
            }
            return -1;
        }

        private View getItemView(int position, @NonNull ViewGroup parent){
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.spinner_item, parent, false);

            DataBaseItem item = super.getItem(position);

            if (item != null){
                TextView tvAlias = view.findViewById(R.id.alias);
                tvAlias.setText(item.getString("alias"));
                TextView tvServer = view.findViewById(R.id.server_address);
                tvServer.setText(item.getString("server_address"));
            }

            return view;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getItemView(position, parent);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getItemView(position, parent);
        }
    }

}
