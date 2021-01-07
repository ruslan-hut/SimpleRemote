package ua.com.programmer.simpleremote;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.view.View;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ua.com.programmer.simpleremote.serviceUtils.DocumentsFilterActivity;
import ua.com.programmer.simpleremote.settings.AppSettings;
import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
            DocumentsListFragment.OnFragmentInteractionListener,
            SelectDataTypeFragment.OnFragmentInteractionListener,
            DataLoader.DataLoaderListener{

    private long backPressedTime;
    private Fragment fragment;
    private String fragmentTAG = "";
    private String documentType = "";
    private String pageTitle;
    private FloatingActionButton floatingActionButton;
    private final Utils utils = new Utils();
    private final Cache cache = Cache.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(v -> fabOnClickAction());
        floatingActionButton.show();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navHeaderView = navigationView.getHeaderView(0);
        TextView appNameText = navHeaderView.findViewById(R.id.nav_header_app_title);
        appNameText.setOnClickListener((View v) -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, utils.readLogs().toString());
            intent.setType("text/plain");
            startActivity(intent);
        });

        String userID = AppSettings.getInstance(this).getUserID();
        String version = BuildConfig.VERSION_NAME+" ("+userID.substring(0, 8)+")";
        TextView navText1 = navHeaderView.findViewById(R.id.nav_header_text1);
        navText1.setText(version);

        checkStateAndLogin(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPageTitle();
    }

    private void setPageTitle(){
        if (pageTitle != null) {
            setTitle(pageTitle);
        }else if (utils.getPageTitleID(fragmentTAG) != R.string.app_name) {
            setTitle(utils.getPageTitleID(fragmentTAG));
        }else{
            setTitle(fragmentTAG);
        }
    }

    private void checkStateAndLogin(boolean disableAutoLogin){
        if (disableAutoLogin){
            AppSettings.getInstance(this).setAutoConnectMode(false);
        }
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //requestCode = 1: from Login activity
        if (requestCode == 1) {
            if (resultCode == 1) {
                //on successful connection with server
                if (fragmentTAG == null || fragmentTAG.equals("")){
                    attachFragment(Constants.DOCUMENTS);
                }
            }else {
                //connection failed
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }else if (fragmentTAG.equals(Constants.DOCUMENTS_LIST)){
            attachFragment(Constants.DOCUMENTS);
        } else {
            if (backPressedTime+2000>System.currentTimeMillis()) {
                super.onBackPressed();
            }else {
                Toast.makeText(this, R.string.hint_press_back, Toast.LENGTH_SHORT).show();
                backPressedTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_filter) {
            if (fragmentTAG.equals(Constants.DOCUMENTS_LIST)){
                Intent intent = new Intent(this, DocumentsFilterActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_logoff) checkStateAndLogin(true);
        if (id == R.id.nav_documents) attachFragment(Constants.DOCUMENTS);
        if (id == R.id.nav_catalogs) attachFragment(Constants.CATALOGS);
        if (id == R.id.nav_settings_connection) {
            Intent intentConnection = new Intent(this, ConnectionSettingsActivity.class);
            startActivity(intentConnection);
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void makeFragmentAttach(){
        if(fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.main_screen_container, fragment)
                    .commitAllowingStateLoss();
        }
    }

    //fragment for choosing list data type
    private void attachFragment(String tag){
        floatingActionButton.hide();
        if (tag == null){
            return;
        }

        cache.setFragmentTAG(tag);
        fragmentTAG = tag;
        fragment = SelectDataTypeFragment.newInstance(tag);

        makeFragmentAttach();

        pageTitle = getString(utils.getPageTitleID(fragmentTAG));
        setPageTitle();
    }

    //attaches fragment with documents list
    private void attachDocumentListFragment(String tag){
        if (tag == null){
            return;
        }
        fragmentTAG = Constants.DOCUMENTS_LIST;
        cache.setFragmentTAG(fragmentTAG);
        documentType = tag;
        fragment = DocumentsListFragment.newInstance();
        makeFragmentAttach();
    }

    @Override
    public void onFragmentInteraction(DataBaseItem currentListItem) {
        String type = currentListItem.getString("specialType");
        if (type.isEmpty()) {
            type = currentListItem.getString("code");
        }
        String description = currentListItem.getString("description");
        if (description != null && !description.equals("")) {
            pageTitle = description;
            setPageTitle();
        }
        switch (fragmentTAG){
            case Constants.DOCUMENTS:
                //*******************************************
                //invoke selected document type list opening
                //*******************************************
                attachDocumentListFragment(type);
                break;
            case Constants.CATALOGS:
                //*******************************************
                //invoke selected catalog type list opening
                //*******************************************
                Intent intentCatalog = new Intent(this, CatalogListActivity.class);
                intentCatalog.putExtra("catalogType", type);
                startActivity(intentCatalog);
                break;
            case Constants.DOCUMENTS_LIST:
                //*******************************************
                //open selected document
                //*******************************************
                if (!currentListItem.hasValue("type")){
                    currentListItem.put("type", documentType);
                }
                Intent intent = new Intent(this, DocumentActivity.class);
                intent.putExtra("cacheKey", cache.put(currentListItem));
                intent.putExtra("guid",currentListItem.getString("guid"));
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onDataUpdateRequest() {
        DataLoader dataLoader = new DataLoader(this);
        switch (fragmentTAG){
            case Constants.DOCUMENTS:
                //dataLoader.getAllowedDocumentsTypes();
                break;
            case Constants.DOCUMENTS_LIST:
                dataLoader.getDocuments(documentType);
                break;
            case Constants.CATALOGS:
                dataLoader.getAllowedCatalogsTypes();
                break;
        }
    }

    @Override
    public void onDataLoaded(ArrayList<DataBaseItem> dataItems) {
        if (dataItems.size() == 0 && !documentType.equals(Constants.CACHED_DOCUMENTS)){
            Toast.makeText(this, R.string.error_no_data, Toast.LENGTH_SHORT).show();
        }
        if (fragment instanceof DocumentsListFragment){
            DocumentsListFragment documentsListFragment = (DocumentsListFragment) fragment;
            documentsListFragment.loadListData(dataItems);
            if (documentType.equals(Constants.CACHED_DOCUMENTS)) {
                floatingActionButton.hide();
            }else {
                floatingActionButton.show();
            }
        }
        if (fragment instanceof SelectDataTypeFragment){
            SelectDataTypeFragment selectDataTypeFragment = (SelectDataTypeFragment) fragment;
            selectDataTypeFragment.loadListData(dataItems);
        }
    }

    @Override
    public void onDataLoaderError(String error) {
        if (fragment instanceof SelectDataTypeFragment){
            SelectDataTypeFragment selectDataTypeFragment = (SelectDataTypeFragment) fragment;
            selectDataTypeFragment.loadError(error);
        }
        if (fragment instanceof DocumentsListFragment) {
            DocumentsListFragment documentsListFragment = (DocumentsListFragment) fragment;
            documentsListFragment.loadError(error);
        }else{
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onListScrolled(int dy) {
        if (dy > 0 && floatingActionButton.getVisibility() == View.VISIBLE) {
            floatingActionButton.hide();
        }else if (dy < 0 && floatingActionButton.getVisibility() != View.VISIBLE){
            floatingActionButton.show();
        }
    }

    private void fabOnClickAction(){
        Intent intent = new Intent(this, DocumentActivity.class);
        DataBaseItem documentDataItem = new DataBaseItem();
        documentDataItem.newDocumentDataItem(documentType);
        intent.putExtra("cacheKey", cache.put(documentDataItem));
        startActivity(intent);
    }
}
