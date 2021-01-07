package ua.com.programmer.simpleremote.serviceUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import ua.com.programmer.simpleremote.settings.AppSettings;
import ua.com.programmer.simpleremote.specialItems.Cache;
import ua.com.programmer.simpleremote.CatalogListActivity;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.R;
import ua.com.programmer.simpleremote.specialItems.DocumentField;

public class DocumentsFilterActivity extends AppCompatActivity {

    FilterListAdapter adapter;
    ArrayList<DocumentField> filter;
    Cache cache = Cache.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents_filter);
        setTitle(R.string.filter);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        filter = AppSettings.getDocumentFilter();

        TextView confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener((View v) -> onConfirmButtonClick());
        TextView resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener((View v) -> onResetButtonClick());

        RecyclerView recyclerView = findViewById(R.id.filter_elements);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new FilterListAdapter();
        recyclerView.setAdapter(adapter);

        adapter.loadListItems(filter);
    }

    void onResetButtonClick(){
        ArrayList<DocumentField> newFilter = new ArrayList<>();
        for (int i=0; i<filter.size(); i++){
            DocumentField item = filter.get(i);
            item.code = "";
            item.value = "";
            newFilter.add(item);
        }
        AppSettings.getInstance(this).setDocumentFilter(newFilter);
        finish();
    }

    void onConfirmButtonClick(){
        ArrayList<DocumentField> newFilter = new ArrayList<>();
        for (int i=0; i<adapter.getItemCount(); i++){
            DocumentField item = adapter.getListItem(i);
            newFilter.add(item);
        }
        AppSettings.getInstance(this).setDocumentFilter(newFilter);
        finish();
    }

    private void pickDate(int position){
        final Calendar calendar = new GregorianCalendar();
        int Y = calendar.get(Calendar.YEAR);
        int M = calendar.get(Calendar.MONTH);
        int D = calendar.get(Calendar.DATE);
        AlertDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar cal = new GregorianCalendar();
            cal.set(year,month,dayOfMonth,0,0);
            String date = String.format(Locale.getDefault(),"%1$td.%1$tm.%1$tY",cal);
            DocumentField filterElement = adapter.getListItem(position);
            filterElement.value = date;
            adapter.notifyItemChanged(position);
        },Y,M,D);
        dialog.show();
    }

    void onListItemClick(int position){
        DocumentField filterElement = adapter.getListItem(position);
        if (filterElement.isCatalog()) {
            Intent intent = new Intent(this, CatalogListActivity.class);
            intent.putExtra("catalogType", filterElement.type);
            intent.putExtra("itemSelectionMode", true);
            startActivityForResult(intent, position);
        }else if (filterElement.isDate()){
            pickDate(position);
        }
    }

    void onClearItemClick(int position){
        DocumentField filterElement = adapter.getListItem(position);
        filterElement.value = "";
        filterElement.code = "";
        adapter.notifyItemChanged(position);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data == null){
            return;
        }
        DocumentField filterElement = adapter.getListItem(requestCode);
        DataBaseItem dataBaseItem = cache.get(data.getStringExtra("cacheKey"));
        filterElement.code = dataBaseItem.getString("code");
        filterElement.value = dataBaseItem.getString("description");
        adapter.notifyItemChanged(requestCode);

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    class FilterViewHolder extends RecyclerView.ViewHolder{

        TextView tvName;
        TextView tvValue;
        ImageView clearButton;

        FilterViewHolder(View view){
            super(view);
            tvName = view.findViewById(R.id.field_name);
            tvValue = view.findViewById(R.id.field_value);
            clearButton = view.findViewById(R.id.clear_button);
        }

        void setHolderInfo(int position){
            DocumentField element = adapter.getListItem(position);
            tvName.setText(element.description);
            tvValue.setText(element.value);
            tvValue.setOnClickListener((View v) -> onListItemClick(position));
            clearButton.setOnClickListener((View v) -> onClearItemClick(position));
        }
    }

    class FilterListAdapter extends RecyclerView.Adapter<DocumentsFilterActivity.FilterViewHolder>{

        private final ArrayList<DocumentField> listItems = new ArrayList<>();

        void loadListItems(ArrayList<DocumentField> values){
            listItems.clear();
            listItems.addAll(values);
            notifyDataSetChanged();
        }

        DocumentField getListItem(int position){
            if (position < getItemCount()){
                return listItems.get(position);
            }
            return new DocumentField("");
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.filter_element;
        }

        @NonNull
        @Override
        public DocumentsFilterActivity.FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(viewType,parent,false);
            return new DocumentsFilterActivity.FilterViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DocumentsFilterActivity.FilterViewHolder holder, int position) {
            //DocumentField element = getListItem(position);
            holder.setHolderInfo(position);
            //holder.itemView.setOnClickListener((View v) -> onListItemClick(element));
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }
    }
}
