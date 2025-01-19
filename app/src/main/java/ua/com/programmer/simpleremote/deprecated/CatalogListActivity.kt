package ua.com.programmer.simpleremote.deprecated

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.repository.DataLoader
import ua.com.programmer.simpleremote.deprecated.settings.Constants
import ua.com.programmer.simpleremote.deprecated.specialItems.Cache
import ua.com.programmer.simpleremote.deprecated.specialItems.DataBaseItem
import ua.com.programmer.simpleremote.deprecated.utility.Utils

class CatalogListActivity : AppCompatActivity(), DataLoader.DataLoaderListener {
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var catalogListAdapter: CatalogListAdapter? = null
    private val utils = Utils()
    private var catalogType: String? = null
    private var currentGroup: String? = ""
    private var currentGroupName: String? = ""
    private var searchFilter = ""
    private var noDataText: TextView? = null
    private var itemSelectionMode: Boolean? = null
    private var documentGUID: String? = null

    private val openNextScreen =
        registerForActivityResult<Intent?, ActivityResult?>(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? -> })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog_list)
        val actionBar = getSupportActionBar()
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        noDataText = findViewById<TextView>(R.id.text_no_data)
        noDataText!!.setVisibility(View.GONE)

        val editText = findViewById<EditText>(R.id.edit_search)
        editText.setVisibility(View.GONE)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                searchFilter = ""
                if (count > 0) {
                    searchFilter = s.toString()
                }
                updateList()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        val intent = getIntent()
        catalogType = intent.getStringExtra("catalogType")
        currentGroup = intent.getStringExtra("currentGroup")
        currentGroupName = intent.getStringExtra("currentGroupName")
        if (currentGroup == null) currentGroup = ""

        itemSelectionMode = intent.getBooleanExtra("itemSelectionMode", false)
        documentGUID = intent.getStringExtra("documentGUID")

        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.catalog_swipe)
        swipeRefreshLayout!!.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { this.updateList() })

        val recyclerView = findViewById<RecyclerView>(R.id.catalog_recycler)
        recyclerView.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.setLayoutManager(linearLayoutManager)
        catalogListAdapter = CatalogListAdapter()
        recyclerView.setAdapter(catalogListAdapter)
    }

    private fun updateList() {
        noDataText!!.setVisibility(View.GONE)
        if (searchFilter != "" || catalogListAdapter!!.getItemCount() == 0) {
            swipeRefreshLayout!!.setRefreshing(true)
        }
        /*
        setting activity title
         */
        if (currentGroupName != null) {
            setTitle(currentGroupName)
        } else if (utils.getPageTitleID(catalogType) != R.string.app_name) {
            setTitle(utils.getPageTitleID(catalogType))
        } else {
            setTitle(catalogType)
        }
        val dataLoader = DataLoader(this)
        dataLoader.getCatalogData(catalogType, currentGroup, searchFilter, documentGUID)
    }

    override fun onDataLoaded(dataBaseItems: java.util.ArrayList<DataBaseItem?>?) {
        noDataText!!.visibility = View.VISIBLE
        swipeRefreshLayout!!.isRefreshing = false
        dataBaseItems?.let {
            if (it.isNotEmpty()) {
                noDataText!!.visibility = View.GONE
            }
            val items : java.util.ArrayList<DataBaseItem> = ArrayList()
            for (item in it) {
                if (item != null) {
                    items.add(item)
                }
            }
            catalogListAdapter!!.loadListItems(items)
        }
    }

    override fun onDataLoaderError(error: String?) {
        swipeRefreshLayout!!.isRefreshing = false
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        updateList()
        super.onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) onBackPressed()

        if (id == R.id.action_search) {
            val editText = findViewById<EditText>(R.id.edit_search)
            if (editText.visibility == View.VISIBLE) {
                editText.visibility = View.GONE
                searchFilter = ""
                updateList()
            } else {
                editText.setVisibility(View.VISIBLE)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.catalog_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun onListItemClick(position: Int) {
        val dataBaseItem = catalogListAdapter!!.getListItem(position)
        if (dataBaseItem.getInt("isGroup") == 1) {
            val intent = Intent(this, CatalogListActivity::class.java)
            intent.putExtra("catalogType", catalogType)
            intent.putExtra("currentGroup", dataBaseItem.getString("code"))
            intent.putExtra("currentGroupName", dataBaseItem.getString("description"))
            intent.putExtra("itemSelectionMode", itemSelectionMode)
            if (itemSelectionMode == true) {
                openNextScreen.launch(intent)
            } else {
                startActivity(intent)
            }
        } else if (itemSelectionMode == true) {
            val cache = Cache.Companion.getInstance()
            val intent = getIntent()
            intent.putExtra("cacheKey", cache.put(dataBaseItem))
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            val intent = getIntent()
            intent.putExtra("cacheKey", data.getStringExtra("cacheKey"))
            setResult(RESULT_OK, intent)
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    ///////// Recycler Adapter //////////////////////////////////////
    internal inner class CatalogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvCode: TextView
        var tvDescription: TextView
        var ivIcon: ImageView?
        var tvRestTitle: TextView
        var tvRestValue: TextView
        var tvPriceValue: TextView
        var tvGroupDescription: TextView
        var cardView: CardView?

        init {
            cardView = view.findViewById<CardView?>(R.id.item_card)
            tvCode = view.findViewById<TextView>(R.id.item_code)
            tvDescription = view.findViewById<TextView>(R.id.item_description)
            ivIcon = view.findViewById<ImageView?>(R.id.item_icon)
            tvGroupDescription = view.findViewById<TextView>(R.id.group_description)
            tvRestTitle = view.findViewById<TextView>(R.id.rest_title)
            tvRestValue = view.findViewById<TextView>(R.id.rest_value)
            tvPriceValue = view.findViewById<TextView>(R.id.item_price)
        }

        fun setHolderInfo(dataBaseItem: DataBaseItem) {
            tvDescription.setText(dataBaseItem.getString("description"))

            val isGroup = dataBaseItem.getInt("isGroup")
            if (isGroup == 0) {
                if (catalogType == Constants.GOODS) {
                    tvCode.setText(dataBaseItem.getString("art"))
                    tvGroupDescription.setText(dataBaseItem.getString("groupName"))

                    val restValue = dataBaseItem.getString("rest")
                    if (restValue.isEmpty() || restValue == "0") {
                        tvRestTitle.setVisibility(View.INVISIBLE)
                        tvRestValue.setVisibility(View.INVISIBLE)
                    } else {
                        tvRestTitle.setVisibility(View.VISIBLE)
                        tvRestValue.setVisibility(View.VISIBLE)
                        tvRestValue.setText(restValue)
                    }
                    var price = dataBaseItem.getString("price")
                    if (price.isEmpty() || price == "0") {
                        tvPriceValue.setVisibility(View.INVISIBLE)
                    } else {
                        price = utils.format(dataBaseItem.getDouble("price"), 2)
                        tvPriceValue.setVisibility(View.VISIBLE)
                        tvPriceValue.setText(price)
                    }
                } else {
                    tvCode.setText(dataBaseItem.getString("code"))
                }
            }
        }
    }

    internal inner class CatalogListAdapter : RecyclerView.Adapter<CatalogViewHolder?>() {
        private val listItems = ArrayList<DataBaseItem>()

        @SuppressLint("NotifyDataSetChanged")
        fun loadListItems(values: java.util.ArrayList<DataBaseItem>) {
            listItems.clear()
            listItems.addAll(values)
            notifyDataSetChanged()
        }

        fun getListItem(position: Int): DataBaseItem {
            if (position < itemCount) {
                return listItems[position]
            }
            return DataBaseItem()
        }

        override fun getItemViewType(position: Int): Int {
            val dataBaseItem = getListItem(position)
            if (dataBaseItem.getInt("isGroup") == 1) {
                return R.layout.catalog_list_item_group
            } else if (catalogType == Constants.GOODS) {
                return R.layout.catalog_list_item_goods
            } else {
                return R.layout.catalog_list_item_contractors
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
            val view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false)
            return CatalogViewHolder(view)
        }

        override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
            holder.setHolderInfo(getListItem(position))
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
}