package ua.com.programmer.simpleremote.deprecated

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.deprecated.settings.Constants
import ua.com.programmer.simpleremote.deprecated.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.deprecated.specialItems.DocumentField

class DocumentsListFragment : Fragment() {
    private var mContext: Context? = null
    private var mListener: OnFragmentInteractionListener? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var documentsAdapter: DocumentsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentView = inflater.inflate(R.layout.fragment_documents_list, container, false)

        swipeRefreshLayout = fragmentView.findViewById<SwipeRefreshLayout>(R.id.documents_swipe)
        swipeRefreshLayout!!.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { this.updateList() })

        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.documents_recycler)
        //recyclerView.setHasFixedSize(true);
        val linearLayoutManager = LinearLayoutManager(mContext)
        recyclerView.setLayoutManager(linearLayoutManager)
        documentsAdapter = DocumentsAdapter()
        recyclerView.setAdapter(documentsAdapter)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (mListener != null) {
                    mListener!!.onListScrolled(dy)
                }
            }
        })

        return fragmentView
    }

    override fun onResume() {
        updateList()
        super.onResume()
    }

    private fun updateList() {
        if (!swipeRefreshLayout!!.isRefreshing) {
            swipeRefreshLayout!!.isRefreshing = true
        }
        if (mListener != null) {
            mListener!!.onDataUpdateRequest()
        }
    }

    private fun onListItemClick(position: Int) {
        if (swipeRefreshLayout!!.isRefreshing) {
            return
        }
        if (mListener != null) {
            mListener!!.onFragmentInteraction(documentsAdapter!!.getListItem(position))
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context as OnFragmentInteractionListener
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
        mContext = context
    }

    fun loadListData(items: java.util.ArrayList<DataBaseItem?>) {
        documentsAdapter!!.loadListItems(items)
        swipeRefreshLayout!!.isRefreshing = false
    }

    fun loadError(error: String?) {
        swipeRefreshLayout!!.isRefreshing = false
        Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(currentListItem: DataBaseItem?)
        fun onDataUpdateRequest()
        fun onListScrolled(dy: Int)
    }

    internal class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvNumber: TextView = view.findViewById<TextView>(R.id.item_number)
        var tvDate: TextView = view.findViewById<TextView>(R.id.item_date)
        var tvCompany: TextView = view.findViewById<TextView>(R.id.item_company)
        var tvWarehouse: TextView = view.findViewById<TextView>(R.id.item_warehouse)
        var tvContractor: TextView = view.findViewById<TextView>(R.id.item_contractor)
        var tvField1: TextView = view.findViewById<TextView>(R.id.item_field1)
        var tvField2: TextView = view.findViewById<TextView>(R.id.item_field2)
        var tvField3: TextView = view.findViewById<TextView>(R.id.item_field3)
        var tvField4: TextView = view.findViewById<TextView>(R.id.item_field4)
        var tvSum: TextView = view.findViewById<TextView>(R.id.item_sum)
        var tvCheckedText: TextView = view.findViewById<TextView>(R.id.item_text_checked)
        var tvNotes: TextView = view.findViewById<TextView>(R.id.item_notes)
        var icon: ImageView = view.findViewById<ImageView>(R.id.item_icon)
        var iconRepeat: ImageView = view.findViewById<ImageView>(R.id.icon_repeat)

        fun setItemInfo(dataBaseItem: DataBaseItem) {
            tvNumber.text = dataBaseItem.getString("number")
            tvDate.text = dataBaseItem.getString("date")
            tvCompany.text = dataBaseItem.getString("company")
            tvWarehouse.text = dataBaseItem.getString("warehouse")

            val contractor = dataBaseItem.getString("contractor")
            if (contractor.isEmpty()) {
                tvContractor.visibility = View.GONE
            } else {
                tvContractor.visibility = View.VISIBLE
                tvContractor.text = contractor
            }

            val currency = dataBaseItem.getString("currency")
            var textSum = dataBaseItem.getString("sum")
            if (!currency.isEmpty()) textSum = "$textSum $currency"
            tvSum.text = textSum

            val field1 = DocumentField(dataBaseItem.getString("field1"))
            tvField1.text = field1.value

            val field2 = DocumentField(dataBaseItem.getString("field2"))
            if (field2.hasValue()) {
                tvField2.text = field2.getNamedValue()
                tvField2.visibility = View.VISIBLE
            } else {
                tvField2.visibility = View.GONE
            }

            val field3 = DocumentField(dataBaseItem.getString("field3"))
            if (field3.hasValue()) {
                tvField3.text = field3.getNamedValue()
                tvField3.visibility = View.VISIBLE
            } else {
                tvField3.visibility = View.GONE
            }

            val field4 = DocumentField(dataBaseItem.getString("field4"))
            if (field4.hasValue()) {
                tvField4.text = field4.getNamedValue()
                tvField4.visibility = View.VISIBLE
            } else {
                tvField4.visibility = View.GONE
            }

            val notes = dataBaseItem.getString("notes")
            if (!notes.isEmpty()) {
                tvNotes.text = notes
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            if (dataBaseItem.hasValue("checked") && dataBaseItem.getBoolean("checked")) {
                tvCheckedText.visibility = View.VISIBLE
            } else {
                tvCheckedText.visibility = View.INVISIBLE
            }

            val isProcessed = dataBaseItem.getInt("isProcessed")
            val isDeleted = dataBaseItem.getInt("isDeleted")
            if (dataBaseItem.hasValue(Constants.CACHE_GUID)) {
                icon.setImageResource(R.drawable.sharp_help_outline_24)
            } else if (isDeleted == 1) {
                icon.setImageResource(R.drawable.twotone_close_24)
            } else if (isProcessed == 1) {
                icon.setImageResource(R.drawable.twotone_check_box_24)
            } else {
                icon.setImageResource(R.drawable.twotone_check_box_outline_blank_24)
            }

            if (dataBaseItem.getString("repeated") == "1") {
                iconRepeat.visibility = View.VISIBLE
            } else {
                iconRepeat.visibility = View.GONE
            }
        }
    }

    internal inner class DocumentsAdapter : RecyclerView.Adapter<DocumentViewHolder?>() {
        private val listItems = ArrayList<DataBaseItem?>()

        @SuppressLint("NotifyDataSetChanged")
        fun loadListItems(values: java.util.ArrayList<DataBaseItem?>) {
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.documents_list_item, parent, false)
            return DocumentViewHolder(view)
        }

        override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
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
        fun newInstance(): DocumentsListFragment {
            return DocumentsListFragment()
        }
    }
}