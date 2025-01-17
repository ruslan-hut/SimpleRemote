package ua.com.programmer.simpleremote

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import ua.com.programmer.simpleremote.DataLoader.DataLoaderListener
import ua.com.programmer.simpleremote.SelectDataTypeFragment.Companion.newInstance
import ua.com.programmer.simpleremote.serviceUtils.DocumentsFilterActivity
import ua.com.programmer.simpleremote.settings.AppSettings
import ua.com.programmer.simpleremote.settings.ConnectionSettingsActivity
import ua.com.programmer.simpleremote.settings.Constants
import ua.com.programmer.simpleremote.specialItems.Cache
import ua.com.programmer.simpleremote.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.utility.Utils
import java.util.ArrayList

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    DocumentsListFragment.OnFragmentInteractionListener,
    SelectDataTypeFragment.OnFragmentInteractionListener, DataLoaderListener {
    private var backPressedTime: Long = 0
    private var fragment: Fragment? = null
    private var fragmentTAG: String? = ""
    private var documentType: String? = ""
    private var pageTitle: String? = null
    private var floatingActionButton: FloatingActionButton? = null
    private val utils = Utils()
    private val cache: Cache = Cache.getInstance()

    private val openNextScreen =
        registerForActivityResult<Intent?, ActivityResult?>(StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? -> })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)

        floatingActionButton = findViewById<FloatingActionButton>(R.id.fab)
        floatingActionButton!!.setOnClickListener(View.OnClickListener { v: View? -> fabOnClickAction() })
        floatingActionButton!!.show()

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val navHeaderView = navigationView.getHeaderView(0)
        val appNameText = navHeaderView.findViewById<TextView>(R.id.nav_header_app_title)
        appNameText.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, utils.readLogs().toString())
            intent.type = "text/plain"
            startActivity(intent)
        })

        val userID = AppSettings.getInstance(this)?.getUserID() ?: "0000000000"
        val version = BuildConfig.VERSION_NAME + " (" + userID.substring(0, 8) + ")"
        val navText1 = navHeaderView.findViewById<TextView>(R.id.nav_header_text1)
        navText1.text = version

        checkStateAndLogin(false)
    }

    override fun onResume() {
        super.onResume()
        setPageTitle()
    }

    private fun setPageTitle() {
        if (pageTitle != null) {
            title = pageTitle
        } else if (utils.getPageTitleID(fragmentTAG) != R.string.app_name) {
            setTitle(utils.getPageTitleID(fragmentTAG))
        } else {
            title = fragmentTAG
        }
    }

    private fun checkStateAndLogin(disableAutoLogin: Boolean) {
        if (disableAutoLogin) {
            AppSettings.getInstance(this)?.setAutoConnectMode(false)
        }
        val intent = Intent(this, LoginActivity::class.java)
        openNextScreen.launch(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == 1) {
            //on successful connection with server
            if (fragmentTAG == null || fragmentTAG == "") {
                attachFragment(Constants.DOCUMENTS)
            }
        } else {
            //connection failed
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (fragmentTAG == Constants.DOCUMENTS_LIST) {
            attachFragment(Constants.DOCUMENTS)
        } else {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                super.onBackPressed()
            } else {
                Toast.makeText(this, R.string.hint_press_back, Toast.LENGTH_SHORT).show()
                backPressedTime = System.currentTimeMillis()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_filter) {
            if (fragmentTAG == Constants.DOCUMENTS_LIST) {
                val intent = Intent(this, DocumentsFilterActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.nav_logoff) checkStateAndLogin(true)
        if (id == R.id.nav_documents) attachFragment(Constants.DOCUMENTS)
        if (id == R.id.nav_catalogs) attachFragment(Constants.CATALOGS)
        if (id == R.id.nav_settings_connection) {
            val intentConnection = Intent(this, ConnectionSettingsActivity::class.java)
            startActivity(intentConnection)
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun makeFragmentAttach() {
        if (fragment != null) {
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.main_screen_container, fragment!!)
                .commitAllowingStateLoss()
        }
    }

    //fragment for choosing list data type
    private fun attachFragment(tag: String?) {
        floatingActionButton!!.hide()
        if (tag == null) {
            return
        }

        cache.setFragmentTAG(tag)
        fragmentTAG = tag
        fragment = newInstance(tag)

        makeFragmentAttach()

        pageTitle = getString(utils.getPageTitleID(fragmentTAG))
        setPageTitle()
    }

    //attaches fragment with documents list
    private fun attachDocumentListFragment(tag: String?) {
        if (tag == null) {
            return
        }
        fragmentTAG = Constants.DOCUMENTS_LIST
        cache.setFragmentTAG(fragmentTAG)
        documentType = tag
        fragment = DocumentsListFragment.newInstance()
        makeFragmentAttach()
    }

    override fun onFragmentInteraction(currentListItem: DataBaseItem?) {
        var type = currentListItem?.getString("specialType") ?: ""
        if (type.isEmpty()) {
            type = currentListItem?.getString("code").toString()
        }
        val description = currentListItem?.getString("description")
        if (description != null && description != "") {
            pageTitle = description
            setPageTitle()
        }
        when (fragmentTAG) {
            Constants.DOCUMENTS ->                 //*******************************************
                //invoke selected document type list opening
                //*******************************************
                attachDocumentListFragment(type)

            Constants.CATALOGS -> {
                //*******************************************
                //invoke selected catalog type list opening
                //*******************************************
                val intentCatalog = Intent(this, CatalogListActivity::class.java)
                intentCatalog.putExtra("catalogType", type)
                startActivity(intentCatalog)
            }

            Constants.DOCUMENTS_LIST -> {
                //*******************************************
                //open selected document
                //*******************************************
                currentListItem?.hasValue("type")?.let {
                    if (!it) {
                        currentListItem.put("type", documentType)
                    }
                }
                val intent = Intent(this, DocumentActivity::class.java)
                intent.putExtra("cacheKey", cache.put(currentListItem))
                intent.putExtra("guid", currentListItem?.getString("guid"))
                startActivity(intent)
            }
        }
    }

    override fun onDataUpdateRequest() {
        val dataLoader = DataLoader(this)
        when (fragmentTAG) {
            Constants.DOCUMENTS -> {}
            Constants.DOCUMENTS_LIST -> dataLoader.getDocuments(documentType.toString())
            Constants.CATALOGS -> dataLoader.getAllowedCatalogsTypes()
        }
    }

    override fun onDataLoaded(dataItems: ArrayList<DataBaseItem?>?) {
        val items = dataItems ?: ArrayList()
        if (items.isEmpty() && documentType != Constants.CACHED_DOCUMENTS) {
            Toast.makeText(this, R.string.error_no_data, Toast.LENGTH_SHORT).show()
        }
        if (fragment is DocumentsListFragment) {
            val documentsListFragment = fragment as DocumentsListFragment
            documentsListFragment.loadListData(items)
            if (documentType == Constants.CACHED_DOCUMENTS) {
                floatingActionButton!!.hide()
            } else {
                floatingActionButton!!.show()
            }
        }
        if (fragment is SelectDataTypeFragment) {
            val selectDataTypeFragment = fragment as SelectDataTypeFragment
            selectDataTypeFragment.loadListData(items)
        }
    }

    override fun onDataLoaderError(error: String?) {
        if (fragment is SelectDataTypeFragment) {
            val selectDataTypeFragment = fragment as SelectDataTypeFragment
            selectDataTypeFragment.loadError(error)
        }
        if (fragment is DocumentsListFragment) {
            val documentsListFragment = fragment as DocumentsListFragment
            documentsListFragment.loadError(error)
        } else {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onListScrolled(dy: Int) {
        if (dy > 0 && floatingActionButton!!.visibility == View.VISIBLE) {
            floatingActionButton!!.hide()
        } else if (dy < 0 && floatingActionButton!!.visibility != View.VISIBLE) {
            floatingActionButton!!.show()
        }
    }

    private fun fabOnClickAction() {
        val intent = Intent(this, DocumentActivity::class.java)
        val documentDataItem = DataBaseItem()
        documentDataItem.newDocumentDataItem(documentType)
        intent.putExtra("cacheKey", cache.put(documentDataItem))
        startActivity(intent)
    }
}
