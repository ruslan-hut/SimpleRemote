package ua.com.programmer.simpleremote.settings;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import ua.com.programmer.simpleremote.R;
import ua.com.programmer.simpleremote.SqliteDB;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;

public class ConnectionSettingsActivity extends AppCompatActivity {

    private AppSettings appSettings;

    EditText alias;
    EditText serverAddress;
    EditText databaseName;
    EditText userName;
    EditText password;
    Switch demo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_settings);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.action_connection_settings);

        appSettings = AppSettings.getInstance(this);

        alias = findViewById(R.id.pref_alias);
        alias.setText(appSettings.getCurrentConnectionAlias());
        serverAddress = findViewById(R.id.pref_server_address);
        serverAddress.setText(appSettings.getServerAddress());
        databaseName = findViewById(R.id.pref_database);
        databaseName.setText(appSettings.getDatabaseName());
        userName = findViewById(R.id.pref_user);
        userName.setText(appSettings.getUserName());
        password = findViewById(R.id.pref_password);
        password.setText(appSettings.getUserPassword());

        boolean demoMode = appSettings.getServerAddress().equals("demo");
        demo = findViewById(R.id.demo);
        demo.setChecked(demoMode);
        onDemoModeChecked();

        demo.setOnClickListener((View v) -> onDemoModeChecked());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connection_settings_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void onDemoModeChecked(){
        boolean demoMode = demo.isChecked();
        serverAddress.setEnabled(!demoMode);
        databaseName.setEnabled(!demoMode);
        userName.setEnabled(!demoMode);
        password.setEnabled(!demoMode);
        if (demoMode){
            serverAddress.setText(R.string.demo_key);
            if (alias.getText().toString().equals("")){
                alias.setText(R.string.pref_demo_mode);
            }
            databaseName.setText("");
            userName.setText("");
            password.setText("");
        }
    }

    private void saveSettings(){
        Switch demo = findViewById(R.id.demo);
        appSettings.setDemoMode(demo.isChecked());

        appSettings.setCurrentConnectionAlias(alias.getText().toString());
        appSettings.setServerAddress(serverAddress.getText().toString());
        appSettings.setDatabaseName(databaseName.getText().toString());
        appSettings.setUserName(userName.getText().toString());
        appSettings.setPassword(password.getText().toString());

        DataBaseItem dataBaseItem = new DataBaseItem();
        dataBaseItem.put("alias", alias.getText().toString());
        dataBaseItem.put("server_address", serverAddress.getText().toString());
        dataBaseItem.put("database_name", databaseName.getText().toString());
        dataBaseItem.put("user_name", userName.getText().toString());
        dataBaseItem.put("user_password", password.getText().toString());
        SqliteDB.getInstance(this).updateSettings(dataBaseItem);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) onBackPressed();
        if (id == R.id.delete) deleteSettings();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        saveSettings();
        super.onBackPressed();
    }

    void deleteSettings(){
        String connectionAlias = alias.getText().toString();
        if (!connectionAlias.equals("")){

            SqliteDB.getInstance(this).deleteSettings(connectionAlias);

            appSettings.setDemoMode(false);
            appSettings.setCurrentConnectionAlias("");
            appSettings.setServerAddress("");
            appSettings.setDatabaseName("");
            appSettings.setUserName("");
            appSettings.setPassword("");

            finish();
        }
    }
}
