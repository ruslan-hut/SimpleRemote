package ua.com.programmer.simpleremote.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.SqliteDB
import ua.com.programmer.simpleremote.specialItems.DataBaseItem

class ConnectionSettingsActivity : AppCompatActivity() {
    private var appSettings: AppSettings? = null

    var alias: EditText? = null
    var serverAddress: EditText? = null
    var databaseName: EditText? = null
    var userName: EditText? = null
    var password: EditText? = null
    var demo: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_settings)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.action_connection_settings)

        appSettings = AppSettings.getInstance(this)

        alias = findViewById<EditText>(R.id.pref_alias)
        alias!!.setText(appSettings!!.getCurrentConnectionAlias())
        serverAddress = findViewById<EditText>(R.id.pref_server_address)
        serverAddress!!.setText(appSettings!!.getServerAddress())
        databaseName = findViewById<EditText>(R.id.pref_database)
        databaseName!!.setText(appSettings!!.getDatabaseName())
        userName = findViewById<EditText>(R.id.pref_user)
        userName!!.setText(appSettings!!.getUserName())
        password = findViewById<EditText>(R.id.pref_password)
        password!!.setText(appSettings!!.getUserPassword())

        val demoMode = appSettings!!.getServerAddress() == "demo"
        demo = findViewById<Switch>(R.id.demo)
        demo!!.setChecked(demoMode)
        onDemoModeChecked()

        demo!!.setOnClickListener(View.OnClickListener { v: View? -> onDemoModeChecked() })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.connection_settings_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun onDemoModeChecked() {
        val demoMode = demo!!.isChecked()
        serverAddress!!.setEnabled(!demoMode)
        databaseName!!.setEnabled(!demoMode)
        userName!!.setEnabled(!demoMode)
        password!!.setEnabled(!demoMode)
        if (demoMode) {
            serverAddress!!.setText(R.string.demo_key)
            if (alias!!.getText().toString() == "") {
                alias!!.setText(R.string.pref_demo_mode)
            }
            databaseName!!.setText("")
            userName!!.setText("")
            password!!.setText("")
        }
    }

    private fun saveSettings() {
        val demo = findViewById<Switch>(R.id.demo)
        appSettings!!.setDemoMode(demo.isChecked())

        appSettings!!.setCurrentConnectionAlias(alias!!.getText().toString())
        appSettings!!.setServerAddress(serverAddress!!.getText().toString())
        appSettings!!.setDatabaseName(databaseName!!.getText().toString())
        appSettings!!.setUserName(userName!!.getText().toString())
        appSettings!!.setPassword(password!!.getText().toString())

        val dataBaseItem = DataBaseItem()
        dataBaseItem.put("alias", alias!!.getText().toString())
        dataBaseItem.put("server_address", serverAddress!!.getText().toString())
        dataBaseItem.put("database_name", databaseName!!.getText().toString())
        dataBaseItem.put("user_name", userName!!.getText().toString())
        dataBaseItem.put("user_password", password!!.getText().toString())
        SqliteDB.getInstance(this)!!.updateSettings(dataBaseItem)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        if (id == android.R.id.home) onBackPressed()
        if (id == R.id.delete) deleteSettings()
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("")
    override fun onBackPressed() {
        saveSettings()
        super.onBackPressed()
    }

    fun deleteSettings() {
        val connectionAlias = alias!!.text.toString()
        if (connectionAlias != "") {
            SqliteDB.getInstance(this)!!.deleteSettings(connectionAlias)

            appSettings!!.setDemoMode(false)
            appSettings!!.setCurrentConnectionAlias("")
            appSettings!!.setServerAddress("")
            appSettings!!.setDatabaseName("")
            appSettings!!.setUserName("")
            appSettings!!.setPassword("")

            finish()
        }
    }
}
