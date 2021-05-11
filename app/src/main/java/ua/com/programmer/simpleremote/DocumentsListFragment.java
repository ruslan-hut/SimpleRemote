package ua.com.programmer.simpleremote;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;
import ua.com.programmer.simpleremote.specialItems.DocumentField;

public class DocumentsListFragment extends Fragment{

    private Context mContext;
    private OnFragmentInteractionListener mListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DocumentsAdapter documentsAdapter;

    public DocumentsListFragment() {
        // Required empty public constructor
    }

    static DocumentsListFragment newInstance() {
        return new DocumentsListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_documents_list, container, false);

        swipeRefreshLayout = fragmentView.findViewById(R.id.documents_swipe);
        swipeRefreshLayout.setOnRefreshListener(this::updateList);

        RecyclerView recyclerView = fragmentView.findViewById(R.id.documents_recycler);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(linearLayoutManager);
        documentsAdapter = new DocumentsAdapter();
        recyclerView.setAdapter(documentsAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mListener != null){
                    mListener.onListScrolled(dy);
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        updateList();
        super.onResume();
    }

    private void updateList(){
        if (!swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setRefreshing(true);
        }
        if(mListener != null){
            mListener.onDataUpdateRequest();
        }
    }

    private void onListItemClick(int position) {
        if (swipeRefreshLayout.isRefreshing()){
            return;
        }
        if (mListener != null) {
            mListener.onFragmentInteraction(documentsAdapter.getListItem(position));
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        mContext = context;
    }

    void loadListData(ArrayList<DataBaseItem> items){
        documentsAdapter.loadListItems(items);
        swipeRefreshLayout.setRefreshing(false);
    }

    void loadError(String error){
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(DataBaseItem currentListItem);
        void onDataUpdateRequest();
        void onListScrolled(int dy);
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder{

        TextView tvNumber;
        TextView tvDate;
        TextView tvCompany;
        TextView tvWarehouse;
        TextView tvField1;
        TextView tvField2;
        TextView tvField3;
        TextView tvField4;
        TextView tvSum;
        TextView tvCheckedText;
        TextView tvNotes;
        ImageView icon;

        DocumentViewHolder(View view){
            super(view);
            tvNumber = view.findViewById(R.id.item_number);
            tvDate = view.findViewById(R.id.item_date);
            tvCompany = view.findViewById(R.id.item_company);
            tvWarehouse = view.findViewById(R.id.item_warehouse);
            tvField1 = view.findViewById(R.id.item_field1);
            tvField2 = view.findViewById(R.id.item_field2);
            tvField3 = view.findViewById(R.id.item_field3);
            tvField4 = view.findViewById(R.id.item_field4);
            tvCheckedText = view.findViewById(R.id.item_text_checked);
            tvNotes = view.findViewById(R.id.item_notes);
            tvSum = view.findViewById(R.id.item_sum);
            icon = view.findViewById(R.id.item_icon);
        }

        void setItemInfo(DataBaseItem dataBaseItem){
            tvNumber.setText(dataBaseItem.getString("number"));
            tvDate.setText(dataBaseItem.getString("date"));
            tvCompany.setText(dataBaseItem.getString("company"));
            tvWarehouse.setText(dataBaseItem.getString("warehouse"));
            tvSum.setText(dataBaseItem.getString("sum"));

            DocumentField field1 = new DocumentField(dataBaseItem.getString("field1"));
            tvField1.setText(field1.value);

            DocumentField field2 = new DocumentField(dataBaseItem.getString("field2"));
            if (field2.hasValue()) {
                tvField2.setText(field2.getNamedValue());
                tvField2.setVisibility(View.VISIBLE);
            }else{
                tvField2.setVisibility(View.GONE);
            }

            DocumentField field3 = new DocumentField(dataBaseItem.getString("field3"));
            if (field3.hasValue()) {
                tvField3.setText(field3.getNamedValue());
                tvField3.setVisibility(View.VISIBLE);
            }else {
                tvField3.setVisibility(View.GONE);
            }

            DocumentField field4 = new DocumentField(dataBaseItem.getString("field4"));
            if (field4.hasValue()) {
                tvField4.setText(field4.getNamedValue());
                tvField4.setVisibility(View.VISIBLE);
            }else {
                tvField4.setVisibility(View.GONE);
            }

            String notes = dataBaseItem.getString("notes");
            if (!notes.equals("")) {
                tvNotes.setText(notes);
                tvNotes.setVisibility(View.VISIBLE);
            }else {
                tvNotes.setVisibility(View.GONE);
            }

            if (dataBaseItem.hasValue("checked") && dataBaseItem.getBoolean("checked")) {
                tvCheckedText.setVisibility(View.VISIBLE);
            }else {
                tvCheckedText.setVisibility(View.INVISIBLE);
            }

            int isProcessed = dataBaseItem.getInt("isProcessed");
            int isDeleted = dataBaseItem.getInt("isDeleted");
            if (dataBaseItem.hasValue(Constants.CACHE_GUID)){
                icon.setImageResource(R.drawable.sharp_help_outline_24);
            }else if (isDeleted == 1) {
                icon.setImageResource(R.drawable.twotone_close_24);
            }else if(isProcessed == 1) {
                icon.setImageResource(R.drawable.twotone_check_box_24);
            }else {
                icon.setImageResource(R.drawable.twotone_check_box_outline_blank_24);
            }
        }
    }

    class DocumentsAdapter extends RecyclerView.Adapter<DocumentViewHolder>{

        private final ArrayList<DataBaseItem> listItems = new ArrayList<>();

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

        @NonNull
        @Override
        public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.documents_list_item,parent,false);
            return new DocumentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
            holder.setItemInfo(getListItem(position));
            holder.itemView.setOnClickListener((View v) -> onListItemClick(position));
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }
    }
}
