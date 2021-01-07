package ua.com.programmer.simpleremote;


import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ua.com.programmer.simpleremote.settings.AppSettings;
import ua.com.programmer.simpleremote.settings.Constants;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SelectDataTypeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SelectDataTypeFragment extends Fragment {

    private static final String DATA_TYPE_CLASS = "data_type_class";

    private OnFragmentInteractionListener listener;
    private String dataTypeClass;
    private Context context;
    private Adapter adapter;

    public SelectDataTypeFragment() {
        // Required empty public constructor
    }

    static SelectDataTypeFragment newInstance(String dataTypeClass) {
        SelectDataTypeFragment fragment = new SelectDataTypeFragment();
        Bundle args = new Bundle();
        args.putString(DATA_TYPE_CLASS, dataTypeClass);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            dataTypeClass = getArguments().getString(DATA_TYPE_CLASS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_select_data_type, container, false);

        RecyclerView recyclerView = fragmentView.findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new SelectDataTypeFragment.Adapter();
        recyclerView.setAdapter(adapter);

        if (dataTypeClass.equals(Constants.CATALOGS)){
            adapter.loadListItems(AppSettings.getAllowedCatalogs());
        }
        if (dataTypeClass.equals(Constants.DOCUMENTS)){
            adapter.loadListItems(AppSettings.getAllowedDocuments());
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof DocumentsListFragment.OnFragmentInteractionListener) {
            listener = (SelectDataTypeFragment.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        this.context = context;
    }

    void loadListData(ArrayList<DataBaseItem> items){
        adapter.loadListItems(items);
    }

    void loadError(String error){
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
    }

    private void onListItemClick(int position){
        if (listener != null){
            listener.onFragmentInteraction(adapter.getListItem(position));
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(DataBaseItem currentListItem);
    }

    ///////// Recycler Adapter //////////////////////////////////////

    static class ViewHolder extends RecyclerView.ViewHolder{

        TextView tvDescription;

        ViewHolder(View view){
            super(view);
            tvDescription = view.findViewById(R.id.item_description);
        }

        void setItemInfo(DataBaseItem dataBaseItem){
            tvDescription.setText(dataBaseItem.getString("description"));
        }
    }

    class Adapter extends RecyclerView.Adapter<SelectDataTypeFragment.ViewHolder>{

        private ArrayList<DataBaseItem> listItems = new ArrayList<>();

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
        public SelectDataTypeFragment.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_list_item,parent,false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SelectDataTypeFragment.ViewHolder holder, int position) {
            holder.setItemInfo(getListItem(position));
            holder.itemView.setOnClickListener((View v) -> onListItemClick(position));
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }
    }

}
