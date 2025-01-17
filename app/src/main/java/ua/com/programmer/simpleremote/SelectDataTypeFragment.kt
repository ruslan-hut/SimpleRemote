package ua.com.programmer.simpleremote

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ua.com.programmer.simpleremote.settings.AppSettings
import ua.com.programmer.simpleremote.settings.Constants
import ua.com.programmer.simpleremote.specialItems.DataBaseItem
import java.lang.RuntimeException
import java.util.ArrayList


class SelectDataTypeFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private var dataTypeClass: String? = null
    private var context: Context? = null
    private var adapter: Adapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            dataTypeClass = requireArguments().getString(DATA_TYPE_CLASS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentView = inflater.inflate(R.layout.fragment_select_data_type, container, false)

        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.recycler)
        //recyclerView.setHasFixedSize(true);
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.setLayoutManager(linearLayoutManager)
        adapter = Adapter()
        recyclerView.setAdapter(adapter)

        if (dataTypeClass == Constants.CATALOGS) {
            adapter!!.loadListItems(AppSettings.getAllowedCatalogs())
        }
        if (dataTypeClass == Constants.DOCUMENTS) {
            adapter!!.loadListItems(AppSettings.getAllowedDocuments())
        }

        return fragmentView
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is DocumentsListFragment.OnFragmentInteractionListener) {
            listener = context as OnFragmentInteractionListener
        } else {
            throw RuntimeException(
                context.toString()
                        + " must implement OnFragmentInteractionListener"
            )
        }
        this.context = context
    }

    fun loadListData(items: ArrayList<DataBaseItem?>) {
        adapter!!.loadListItems(items)
    }

    fun loadError(error: String?) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }

    private fun onListItemClick(position: Int) {
        if (listener != null) {
            listener!!.onFragmentInteraction(adapter!!.getListItem(position))
        }
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(currentListItem: DataBaseItem?)
    }

    internal class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvDescription: TextView = view.findViewById<TextView>(R.id.item_description)

        fun setItemInfo(dataBaseItem: DataBaseItem) {
            tvDescription.text = dataBaseItem.getString("description")
        }
    }

    internal inner class Adapter : RecyclerView.Adapter<ViewHolder?>() {
        private val listItems = ArrayList<DataBaseItem?>()

        fun loadListItems(values: ArrayList<DataBaseItem?>) {
            listItems.clear()
            listItems.addAll(values)
            notifyDataSetChanged()
        }

        fun getListItem(position: Int): DataBaseItem? {
            if (position < itemCount) {
                return listItems[position]
            }
            return DataBaseItem()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.select_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setItemInfo(getListItem(position)!!)
            holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
                onListItemClick(
                    position
                )
            })
        }

        override fun getItemCount(): Int {
            return listItems.size
        }
    }

    companion object {
        private const val DATA_TYPE_CLASS = "data_type_class"

        @JvmStatic
        fun newInstance(dataTypeClass: String?): SelectDataTypeFragment {
            val fragment = SelectDataTypeFragment()
            val args = Bundle()
            args.putString(DATA_TYPE_CLASS, dataTypeClass)
            fragment.setArguments(args)
            return fragment
        }
    }
}
