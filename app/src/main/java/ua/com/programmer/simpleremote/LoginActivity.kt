package ua.com.programmer.simpleremote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import org.json.JSONArray
import ua.com.programmer.simpleremote.repository.DataLoader.DataLoaderListener
import ua.com.programmer.simpleremote.LoginActivity.ConnectionsSpinnerAdapter
import ua.com.programmer.simpleremote.SqliteDB.Companion.getInstance
import ua.com.programmer.simpleremote.repository.DataLoader
import ua.com.programmer.simpleremote.settings.AppSettings
import ua.com.programmer.simpleremote.settings.ConnectionSettingsActivity
import ua.com.programmer.simpleremote.settings.Constants
import ua.com.programmer.simpleremote.specialItems.Cache
import ua.com.programmer.simpleremote.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.specialItems.DocumentField
import ua.com.programmer.simpleremote.utility.Utils
import java.lang.Exception
import java.util.ArrayList

class LoginActivity : AppCompatActivity(), DataLoaderListener, AdapterView.OnItemSelectedListener {
    private var appSettings: AppSettings? = null
    private var connectionsSpinner: Spinner? = null
    private var adapter: ConnectionsSpinnerAdapter? = null
    private var progressBar: ProgressBar? = null

    private val utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setResult(0)
        Cache.getInstance().clear()
        imageCacheCleaner()

        appSettings = AppSettings.getInstance(this)

        val autoConnect = findViewById<CheckBox>(R.id.autoconnect)
        autoConnect.isChecked = appSettings!!.autoConnect()
        autoConnect.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { b: CompoundButton?, isChecked: Boolean ->
            appSettings!!.setAutoConnectMode(
                isChecked
            )
        })

        val textVersion =
            BuildConfig.VERSION_NAME + " : " + appSettings!!.getUserID().substring(0, 8)
        val version = findViewById<TextView>(R.id.version)
        version.text = textVersion
        version.setOnClickListener(View.OnClickListener { view: View? ->
            val logContent = utils.readLogs().toString()
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, logContent)
            intent.type = "text/plain"
            startActivity(intent)
        })

        val loginButton = findViewById<TextView>(R.id.login_button)
        loginButton.setOnClickListener(View.OnClickListener { v: View? -> connect() })
        val editButton = findViewById<TextView>(R.id.edit_button)
        editButton.setOnClickListener(View.OnClickListener { v: View? -> editConnection() })

        adapter = ConnectionsSpinnerAdapter(this, android.R.layout.simple_spinner_item)
        connectionsSpinner = findViewById<Spinner>(R.id.current_connection)
        connectionsSpinner!!.adapter = adapter
        connectionsSpinner!!.onItemSelectedListener = this

        progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar!!.visibility = View.INVISIBLE
    }

    private fun editConnection() {
        val intent = Intent(this, ConnectionSettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        setSpinnerItems()
        if (appSettings!!.autoConnect()) {
            connect()
        }
        super.onResume()
    }

    override fun onDataLoaded(dataItems: ArrayList<DataBaseItem?>?) {
        progressBar!!.visibility = View.INVISIBLE
        if (dataItems == null) {
            Toast.makeText(this, R.string.error_no_data, Toast.LENGTH_SHORT).show()
            return
        }

        if (!dataItems.isEmpty()) {
            if (checkServerResponse(dataItems[0]!!)) {
                setResult(1)
                finish()
            } else {
                Toast.makeText(this, R.string.error_access_denied, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, R.string.error_no_data, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDataLoaderError(error: String?) {
        progressBar!!.visibility = View.INVISIBLE
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        //Toast.makeText(this, "Selected "+i, Toast.LENGTH_SHORT).show();
        val selectedItem = connectionsSpinner!!.selectedItem as DataBaseItem?
        if (selectedItem != null) {
            if (appSettings!!.getCurrentConnectionAlias() == selectedItem.getString("alias")) {
                return
            }
            if (selectedItem.getString("action") == "addNew") {
                appSettings!!.setCurrentConnectionAlias("")
                appSettings!!.setServerAddress("")
                appSettings!!.setDatabaseName("")
                appSettings!!.setUserName("")
                appSettings!!.setPassword("")
            } else {
                appSettings!!.setCurrentConnectionAlias(selectedItem.getString("alias"))
                appSettings!!.setServerAddress(selectedItem.getString("server_address"))
                appSettings!!.setDatabaseName(selectedItem.getString("database_name"))
                appSettings!!.setUserName(selectedItem.getString("user_name"))
                appSettings!!.setPassword(selectedItem.getString("user_password"))
                appSettings!!.setConnectionID(selectedItem.getInt("_id"))
            }
        }
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}

    private fun setSpinnerItems() {
        val currentAlias = appSettings!!.getCurrentConnectionAlias()
        adapter!!.clear()

        adapter!!.addAll(getInstance(this)!!.getConnections())

        val specialItem = DataBaseItem()
        specialItem.put("alias", getResources().getString(R.string.alias_add_new))
        specialItem.put("server_address", getResources().getString(R.string.alias_add_new_hint))
        specialItem.put("action", "addNew")
        adapter!!.add(specialItem)
        adapter!!.notifyDataSetChanged()

        val position = adapter!!.aliasPosition(currentAlias)
        if (position >= 0) {
            connectionsSpinner!!.setSelection(position)
        }
    }

    private fun connect() {
        if (appSettings!!.getServerAddress().isEmpty()) {
            Toast.makeText(this, R.string.error_no_address, Toast.LENGTH_SHORT).show()
        } else {
            progressBar!!.visibility = View.VISIBLE
            val dataLoader = DataLoader(this)
            dataLoader.checkConnection()
        }
    }

    /**
     * Reads response from server. Response data contains user-specified options
     *
     * @param response array of parameters
     * @return true if user was granted read access
     */
    private fun checkServerResponse(response: DataBaseItem): Boolean {
        val catalogsList = ArrayList<DataBaseItem?>()
        val documentsList = ArrayList<DataBaseItem?>()
        try {
            val catalogs = JSONArray(response.getString("catalog"))
            for (i in 0 until catalogs.length()) {
                catalogsList.add(DataBaseItem(catalogs.getJSONObject(i)))
            }
            val documents = JSONArray(response.getString("document"))
            for (i in 0 until documents.length()) {
                documentsList.add(DataBaseItem(documents.getJSONObject(i)))
            }
        } catch (e: Exception) {
            utils.error("checkServerResponse: $e")
            utils.error("response: " + response.getAsJSON())
        }
        if (!documentsList.isEmpty()) {
            val cacheList = DataBaseItem()
            cacheList.put("description", getResources().getString(R.string.cached_list))
            cacheList.put("code", Constants.CACHED_DOCUMENTS)
            documentsList.add(cacheList)
        }
        appSettings!!.setAllowedCatalogs(catalogsList)
        appSettings!!.setAllowedDocuments(documentsList)

        //reset documents filter
        appSettings!!.setDocumentFilter(ArrayList<DocumentField?>())

        appSettings!!.setLoadImages(response.getBoolean("loadImages"))

        var workingMode = response.getString("mode")
        if (workingMode.isEmpty()) workingMode = Constants.MODE_FULL
        appSettings!!.setWorkingMode(workingMode)

        return response.getBoolean("read")
    }

    private inner class ConnectionsSpinnerAdapter(context: Context, resID: Int) :
        ArrayAdapter<DataBaseItem?>(context, resID) {
        fun aliasPosition(alias: String?): Int {
            for (i in 0 until count) {
                val item = getItem(i)
                if (item != null && item.getString("alias") == alias) {
                    return i
                }
            }
            return -1
        }

        fun getItemView(position: Int, parent: ViewGroup): View {
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.spinner_item, parent, false)

            val item = super.getItem(position)

            if (item != null) {
                val tvAlias = view.findViewById<TextView>(R.id.alias)
                tvAlias.text = item.getString("alias")
                val tvServer = view.findViewById<TextView>(R.id.server_address)
                tvServer.text = item.getString("server_address")
            }

            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getItemView(position, parent)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getItemView(position, parent)
        }
    }

    private fun imageCacheCleaner() {
        val context = applicationContext

        val thread = Thread(Runnable { Glide.get(context).clearDiskCache() })
        thread.start()
    }
}
