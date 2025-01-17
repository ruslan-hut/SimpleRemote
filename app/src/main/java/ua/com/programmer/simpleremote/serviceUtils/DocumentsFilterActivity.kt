package ua.com.programmer.simpleremote.serviceUtils

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ua.com.programmer.simpleremote.CatalogListActivity
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.serviceUtils.DocumentsFilterActivity.FilterListAdapter
import ua.com.programmer.simpleremote.serviceUtils.DocumentsFilterActivity.FilterViewHolder
import ua.com.programmer.simpleremote.settings.AppSettings
import ua.com.programmer.simpleremote.specialItems.Cache
import ua.com.programmer.simpleremote.specialItems.DocumentField
import java.util.ArrayList
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class DocumentsFilterActivity : AppCompatActivity() {
    private var adapter: FilterListAdapter? = null
    private var filter: ArrayList<DocumentField>? = null
    private var cache: Cache = Cache.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documents_filter)
        setTitle(R.string.filter)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        filter = AppSettings.getDocumentFilter()

        val confirmButton = findViewById<TextView>(R.id.confirm_button)
        confirmButton.setOnClickListener(View.OnClickListener { v: View? -> onConfirmButtonClick() })
        val resetButton = findViewById<TextView>(R.id.reset_button)
        resetButton.setOnClickListener(View.OnClickListener { v: View? -> onResetButtonClick() })

        val recyclerView = findViewById<RecyclerView>(R.id.filter_elements)
        //recyclerView.setHasFixedSize(true);
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.setLayoutManager(linearLayoutManager)
        adapter = FilterListAdapter()
        recyclerView.setAdapter(adapter)

        adapter!!.loadListItems(filter!!)
    }

    fun onResetButtonClick() {
        val newFilter = ArrayList<DocumentField?>()
        for (i in filter!!.indices) {
            val item = filter!!.get(i)
            item.code = ""
            item.value = ""
            newFilter.add(item)
        }
        AppSettings.getInstance(this)?.setDocumentFilter(newFilter)
        finish()
    }

    fun onConfirmButtonClick() {
        val newFilter = ArrayList<DocumentField?>()
        for (i in 0 until adapter!!.itemCount) {
            val item = adapter!!.getListItem(i)
            newFilter.add(item)
        }
        AppSettings.getInstance(this)?.setDocumentFilter(newFilter)
        finish()
    }

    private fun pickDate(position: Int) {
        val calendar: Calendar = GregorianCalendar()
        val Y = calendar.get(Calendar.YEAR)
        val M = calendar.get(Calendar.MONTH)
        val D = calendar.get(Calendar.DATE)
        val dialog: AlertDialog = DatePickerDialog(
            this,
            OnDateSetListener { view: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                val cal: Calendar = GregorianCalendar()
                cal.set(year, month, dayOfMonth, 0, 0)
                val date = String.format(Locale.getDefault(), "%1\$td.%1\$tm.%1\$tY", cal)
                val filterElement = adapter!!.getListItem(position)
                filterElement.value = date
                adapter!!.notifyItemChanged(position)
            },
            Y,
            M,
            D
        )
        dialog.show()
    }

    fun onListItemClick(position: Int) {
        val filterElement = adapter!!.getListItem(position)
        if (filterElement.isCatalog()) {
            val intent = Intent(this, CatalogListActivity::class.java)
            intent.putExtra("catalogType", filterElement.type)
            intent.putExtra("itemSelectionMode", true)
            startActivityForResult(intent, position)
        } else if (filterElement.isDate()) {
            pickDate(position)
        }
    }

    fun onClearItemClick(position: Int) {
        val filterElement = adapter!!.getListItem(position)
        filterElement.value = ""
        filterElement.code = ""
        adapter!!.notifyItemChanged(position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) {
            return
        }
        val filterElement = adapter!!.getListItem(requestCode)
        val dataBaseItem = cache.get(data.getStringExtra("cacheKey"))
        filterElement.code = dataBaseItem.getString("code")
        filterElement.value = dataBaseItem.getString("description")
        adapter!!.notifyItemChanged(requestCode)

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    internal inner class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvName: TextView
        var tvValue: TextView
        var clearButton: ImageView

        init {
            tvName = view.findViewById<TextView>(R.id.field_name)
            tvValue = view.findViewById<TextView>(R.id.field_value)
            clearButton = view.findViewById<ImageView>(R.id.clear_button)
        }

        fun setHolderInfo(position: Int) {
            val element = adapter!!.getListItem(position)
            tvName.setText(element.description)
            tvValue.setText(element.value)
            tvValue.setOnClickListener(View.OnClickListener { v: View? -> onListItemClick(position) })
            clearButton.setOnClickListener(View.OnClickListener { v: View? ->
                onClearItemClick(
                    position
                )
            })
        }
    }

    internal inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder?>() {
        private val listItems = ArrayList<DocumentField>()

        fun loadListItems(values: ArrayList<DocumentField>) {
            listItems.clear()
            listItems.addAll(values)
            notifyDataSetChanged()
        }

        fun getListItem(position: Int): DocumentField {
            if (position < itemCount) {
                return listItems[position]
            }
            return DocumentField("")
        }

        override fun getItemViewType(position: Int): Int {
            return R.layout.filter_element
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
            return FilterViewHolder(view)
        }

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            //DocumentField element = getListItem(position);
            holder.setHolderInfo(position)
            //holder.itemView.setOnClickListener((View v) -> onListItemClick(element));
        }

        override fun getItemCount(): Int {
            return listItems.size
        }
    }
}
