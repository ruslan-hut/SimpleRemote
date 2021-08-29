package ua.com.programmer.simpleremote;

import android.annotation.SuppressLint;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.utility.Utils;

public class CatalogListActivity extends AppCompatActivity implements DataLoader.DataLoaderListener{

    private SwipeRefreshLayout swipeRefreshLayout;
    private CatalogListAdapter catalogListAdapter;
    private final Utils utils = new Utils();
    private String catalogType;
    private String currentGroup = "";
    private String currentGroupName = "";
    private String searchFilter = "";
    private TextView noDataText;
    private Boolean itemSelectionMode;
    private String documentGUID;

    private final ActivityResultLauncher<Intent> openNextScreen = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog_list);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        noDataText = findViewById(R.id.text_no_data);
        noDataText.setVisibility(View.GONE);

        EditText editText = findViewById(R.id.edit_search);
        editText.setVisibility(View.GONE);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchFilter = "";
                if (count > 0) {
                    searchFilter = s.toString();
                }
                updateList();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Intent intent = getIntent();
        catalogType = intent.getStringExtra("catalogType");
        currentGroup = intent.getStringExtra("currentGroup");
        currentGroupName = intent.getStringExtra("currentGroupName");
        if (currentGroup == null) currentGroup = "";

        itemSelectionMode = intent.getBooleanExtra("itemSelectionMode", false);
        documentGUID = intent.getStringExtra("documentGUID");

        swipeRefreshLayout = findViewById(R.id.catalog_swipe);
        swipeRefreshLayout.setOnRefreshListener(this::updateList);

        RecyclerView recyclerView = findViewById(R.id.catalog_recycler);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        catalogListAdapter = new CatalogListAdapter();
        recyclerView.setAdapter(catalogListAdapter);

    }

    private void updateList(){
        noDataText.setVisibility(View.GONE);
        if (!searchFilter.equals("") || catalogListAdapter.getItemCount() == 0){
            swipeRefreshLayout.setRefreshing(true);
        }
        /*
        setting activity title
         */
        if (currentGroupName != null) {
            setTitle(currentGroupName);
        }else if (utils.getPageTitleID(catalogType) != R.string.app_name) {
            setTitle(utils.getPageTitleID(catalogType));
        }else{
            setTitle(catalogType);
        }
        DataLoader dataLoader = new DataLoader(this);
        dataLoader.getCatalogData(catalogType, currentGroup, searchFilter,documentGUID);
    }

    @Override
    public void onDataLoaded(ArrayList<DataBaseItem> dataBaseItems){
        catalogListAdapter.loadListItems(dataBaseItems);
        swipeRefreshLayout.setRefreshing(false);
        if (dataBaseItems.isEmpty()) {
            noDataText.setVisibility(View.VISIBLE);
        }else {
            noDataText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDataLoaderError(String error) {
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        updateList();
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) onBackPressed();

        if (id == R.id.action_search){
            EditText editText = findViewById(R.id.edit_search);
            if (editText.getVisibility() == View.VISIBLE) {
                editText.setVisibility(View.GONE);
                searchFilter = "";
                updateList();
            }else {
                editText.setVisibility(View.VISIBLE);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.catalog_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void onListItemClick(int position){
        DataBaseItem dataBaseItem = catalogListAdapter.getListItem(position);
        if (dataBaseItem.getInt("isGroup") == 1) {
            Intent intent = new Intent(this, CatalogListActivity.class);
            intent.putExtra("catalogType", catalogType);
            intent.putExtra("currentGroup", dataBaseItem.getString("code"));
            intent.putExtra("currentGroupName", dataBaseItem.getString("description"));
            intent.putExtra("itemSelectionMode", itemSelectionMode);
            if (itemSelectionMode) {
                openNextScreen.launch(intent);
            }else {
                startActivity(intent);
            }
        }else if (itemSelectionMode){
            Cache cache = Cache.getInstance();
            Intent intent = getIntent();
            intent.putExtra("cacheKey", cache.put(dataBaseItem));
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null){
            Intent intent = getIntent();
            intent.putExtra("cacheKey", data.getStringExtra("cacheKey"));
            setResult(RESULT_OK, intent);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    ///////// Recycler Adapter //////////////////////////////////////

    class CatalogViewHolder extends RecyclerView.ViewHolder{

        TextView tvCode;
        TextView tvDescription;
        ImageView ivIcon;
        TextView tvRestTitle;
        TextView tvRestValue;
        TextView tvPriceValue;
        TextView tvGroupDescription;
        CardView cardView;

        CatalogViewHolder(View view){
            super(view);
            cardView = view.findViewById(R.id.item_card);
            tvCode = view.findViewById(R.id.item_code);
            tvDescription = view.findViewById(R.id.item_description);
            ivIcon = view.findViewById(R.id.item_icon);
            tvGroupDescription = view.findViewById(R.id.group_description);
            tvRestTitle = view.findViewById(R.id.rest_title);
            tvRestValue = view.findViewById(R.id.rest_value);
            tvPriceValue = view.findViewById(R.id.item_price);
        }

        void setHolderInfo(DataBaseItem dataBaseItem){
            tvDescription.setText(dataBaseItem.getString("description"));

            int isGroup = dataBaseItem.getInt("isGroup");
            if (isGroup == 0) {

                if (catalogType.equals(Constants.GOODS)) {
                    tvCode.setText(dataBaseItem.getString("art"));
                    tvGroupDescription.setText(dataBaseItem.getString("groupName"));

                    String restValue = dataBaseItem.getString("rest");
                    if (restValue.equals("0")) {
                        tvRestTitle.setVisibility(View.INVISIBLE);
                        tvRestValue.setVisibility(View.INVISIBLE);
                    } else {
                        tvRestTitle.setVisibility(View.VISIBLE);
                        tvRestValue.setVisibility(View.VISIBLE);
                        tvRestValue.setText(restValue);
                    }
                    String price = utils.format(dataBaseItem.getDouble("price"), 2);
                    tvPriceValue.setText(price);
                }else{
                    tvCode.setText(dataBaseItem.getString("code"));
                }

            }
        }
    }

    class CatalogListAdapter extends RecyclerView.Adapter<CatalogViewHolder>{

        private final ArrayList<DataBaseItem> listItems = new ArrayList<>();

        @SuppressLint("NotifyDataSetChanged")
        void loadListItems(ArrayList<DataBaseItem> values){
            listItems.clear();
            listItems.addAll(values);
            notifyDataSetChanged();
        }

        DataBaseItem getListItem(int position){
            if (position < getItemCount()){
                return listItems.get(position);
            }
            return new DataBaseItem();
        }

        @Override
        public int getItemViewType(int position) {
            DataBaseItem dataBaseItem = getListItem(position);
            if (dataBaseItem.getInt("isGroup") == 1) {
                return R.layout.catalog_list_item_group;
            }else if (catalogType.equals(Constants.GOODS)) {
                return R.layout.catalog_list_item_goods;
            }else {
                return R.layout.catalog_list_item_contractors;
            }
        }

        @NonNull
        @Override
        public CatalogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(viewType,parent,false);
            return new CatalogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CatalogViewHolder holder, int position) {
            holder.setHolderInfo(getListItem(position));
            holder.itemView.setOnClickListener((View v) -> onListItemClick(position));
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }
    }
}
