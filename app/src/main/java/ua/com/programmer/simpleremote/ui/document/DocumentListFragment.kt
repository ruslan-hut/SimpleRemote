package ua.com.programmer.simpleremote.ui.document

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.DocumentsListItemBinding
import ua.com.programmer.simpleremote.databinding.FragmentDocumentsListBinding
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.FilterItem
import ua.com.programmer.simpleremote.entity.getFilterDisplayString
import ua.com.programmer.simpleremote.entity.isAnyFilterSet
import ua.com.programmer.simpleremote.entity.isEmpty
import ua.com.programmer.simpleremote.entity.presentation
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.getValue
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class DocumentListFragment: Fragment(), MenuProvider  {

    private val viewModel: DocumentListViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentDocumentsListBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: DocumentListFragmentArgs by navArgs()
    private var pendingFilterName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setDocumentType(navigationArgs.type, navigationArgs.title)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentsListBinding.inflate(inflater, container, false)

        val menuHost : MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ItemsListAdapter(
            onItemClicked = { item ->
                sharedViewModel.setDocument(item)
                openDocument(viewModel.type, viewModel.title)
            },
        )
        val recycler = binding.documentsRecycler
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.setItemViewCacheSize(20)
        recycler.setHasFixedSize(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.documents.collect {
                        adapter.submitList(it)
                        if (it.isNotEmpty()) binding.emptyState.visibility = View.GONE
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.documentsSwipe.isRefreshing = loading
                        if (loading) {
                            binding.emptyState.visibility = View.GONE
                        } else {
                            val empty = viewModel.documents.value.isEmpty()
                            binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }
        binding.documentsSwipe.setOnRefreshListener {
            viewModel.loadDocuments()
        }
        binding.retryButton.setOnClickListener {
            binding.emptyState.visibility = View.GONE
            viewModel.loadDocuments()
        }

        observeCatalogPickerResult()
        updateFilterBanner()
    }

    private fun observeCatalogPickerResult() {
        val catalog = sharedViewModel.consumeSelectedCatalogItem()
        if (catalog != null && pendingFilterName != null) {
            viewModel.updateFilterValue(pendingFilterName!!, catalog.code, catalog.description)
            pendingFilterName = null
            viewModel.loadDocuments()
            updateFilterBanner()
        }
    }

    private fun updateFilterBanner() {
        val filters = viewModel.getActiveFilters()
        if (filters.isAnyFilterSet()) {
            binding.filterParameters.text = filters.getFilterDisplayString()
            binding.filterParameters.visibility = View.VISIBLE
        } else {
            binding.filterParameters.visibility = View.GONE
        }
    }

    private fun openDocument(type: String, title: String) {
        val action = DocumentListFragmentDirections.actionDocumentListFragmentToDocumentFragment(title, type)
        this.findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDocuments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.document_list_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_filter -> {
                val anchorView = requireActivity().findViewById<View>(R.id.action_filter)
                showFilterPopup(anchorView)
                true
            }
            else -> false
        }
    }

    private fun showFilterPopup(anchorView: View) {
        val filterSchema = viewModel.filterSchema.value ?: emptyList()
        if (filterSchema.isEmpty()) return

        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.filter_menu_action_layout, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val container = popupView.findViewById<LinearLayout>(R.id.filter_fields_container)
        val activeFilters = viewModel.getActiveFilters()
        val fieldMap = mutableMapOf<String, TextInputEditText>()

        for (filterItem in activeFilters) {
            val itemView = inflater.inflate(R.layout.filter_item_layout, container, false)
            val inputLayout = itemView as TextInputLayout
            val editText = itemView.findViewById<TextInputEditText>(R.id.filter_value_edittext)

            inputLayout.hint = filterItem.name
            inputLayout.helperText = filterItem.description
            editText.setText(filterItem.displayValue())

            fieldMap[filterItem.name] = editText

            editText.setOnLongClickListener {
                editText.text?.clear()
                filterItem.clearValue()
                true
            }

            when (filterItem.meta) {
                "date" -> {
                    editText.setOnClickListener {
                        val datePicker = MaterialDatePicker.Builder.datePicker().build()
                        datePicker.addOnPositiveButtonClickListener { timestamp ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val dateStr = sdf.format(Date(timestamp))
                            editText.setText(dateStr)
                        }
                        datePicker.show(childFragmentManager, "datePicker_${filterItem.name}")
                    }
                }
                "reference" -> {
                    editText.setOnClickListener {
                        // Save current field values before navigating
                        saveFieldValues(fieldMap, activeFilters)
                        viewModel.setActiveFilters(activeFilters)
                        pendingFilterName = filterItem.name
                        popupWindow.dismiss()
                        val action = DocumentListFragmentDirections
                            .actionDocumentListFragmentToCatalogListFragment(
                                type = filterItem.type,
                                title = filterItem.name,
                                group = "",
                                pickerMode = true
                            )
                        findNavController().navigate(action)
                    }
                }
            }

            container.addView(itemView)
        }

        val applyButton = popupView.findViewById<Button>(R.id.apply_button)
        applyButton.setOnClickListener {
            saveFieldValues(fieldMap, activeFilters)
            viewModel.setActiveFilters(activeFilters)
            popupWindow.dismiss()
            viewModel.loadDocuments()
            updateFilterBanner()
        }

        val clearButton = popupView.findViewById<Button>(R.id.clear_button)
        clearButton.setOnClickListener {
            fieldMap.values.forEach { it.text?.clear() }
            activeFilters.forEach { it.clearValue() }
            viewModel.setActiveFilters(activeFilters)
            popupWindow.dismiss()
            viewModel.loadDocuments()
            updateFilterBanner()
        }

        popupWindow.showAsDropDown(anchorView)
    }

    private fun saveFieldValues(
        fieldMap: Map<String, TextInputEditText>,
        filters: List<FilterItem>
    ) {
        for (filter in filters) {
            if (filter.meta == "date") {
                val text = fieldMap[filter.name]?.text?.toString() ?: ""
                filter.value = text
            }
            // Reference values are set via catalog picker, no need to read from text field
        }
    }

    private class ItemsListAdapter(
        private val onItemClicked: (Document) -> Unit
    ): ListAdapter<Document, ItemsListAdapter.ItemViewHolder>(DiffCallback) {

        class ItemViewHolder(private var binding: DocumentsListItemBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Document) {
                binding.apply {
                    itemNumber.text = item.number
                    itemDate.text = item.date

                    itemCompany.text = item.company
                    itemCompany.visibility = if (item.company.isEmpty()) View.GONE else View.VISIBLE

                    itemWarehouse.text = item.warehouse
                    itemWarehouse.visibility = if (item.warehouse.isEmpty()) View.GONE else View.VISIBLE

                    itemContractor.text = item.contractor
                    itemContractor.visibility = if (item.contractor.isEmpty()) View.GONE else View.VISIBLE

                    itemField1.text = item.field1.presentation()
                    itemField1.visibility = if (item.field1.isEmpty()) View.GONE else View.VISIBLE
                    itemField2.text = item.field2.presentation()
                    itemField2.visibility = if (item.field2.isEmpty()) View.GONE else View.VISIBLE
                    itemField3.text = item.field3.presentation()
                    itemField3.visibility = if (item.field3.isEmpty()) View.GONE else View.VISIBLE
                    itemField4.text = item.field4.presentation()
                    itemField4.visibility = if (item.field4.isEmpty()) View.GONE else View.VISIBLE

                    val sumText = if (item.currency.isEmpty()) item.sum else "${item.sum} ${item.currency}"
                    itemSum.text = sumText

                    if (item.notes.isNotEmpty()) {
                        itemNotes.text = item.notes
                        itemNotes.visibility = View.VISIBLE
                    } else {
                        itemNotes.visibility = View.GONE
                    }

                    if (item.checked) {
                        itemTextChecked.visibility = View.VISIBLE
                    } else {
                        itemTextChecked.visibility = View.INVISIBLE
                    }

                    if (item.lockedBy.isNotEmpty()) {
                        itemLockedBy.text = item.lockedBy
                        itemLockedBy.visibility = View.VISIBLE
                    } else {
                        itemLockedBy.visibility = View.GONE
                    }

                    if (item.repeated == "1") {
                        iconRepeat.visibility = View.VISIBLE
                    } else {
                        iconRepeat.visibility = View.GONE
                    }

                    if (item.cacheGUID.isNotEmpty()) {
                        itemIcon.setImageResource(R.drawable.sharp_help_outline_24)
                    }else if (item.isDeleted == 1) {
                        itemIcon.setImageResource(R.drawable.twotone_close_24)
                    }else if (item.isProcessed == 1) {
                        itemIcon.setImageResource(R.drawable.baseline_bookmark_added_24)
                    }else{
                        itemIcon.setImageResource(R.drawable.baseline_bookmark_border_24)
                    }
                }
            }
        }

        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<Document>() {

                override fun areItemsTheSame(oldItem: Document, newItem: Document): Boolean {
                    return oldItem.guid == newItem.guid
                }

                override fun areContentsTheSame(oldItem: Document, newItem: Document): Boolean {
                    return oldItem == newItem
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val viewHolder = ItemViewHolder(
                DocumentsListItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            viewHolder.itemView.setOnClickListener {
                val position = viewHolder.absoluteAdapterPosition
                onItemClicked(getItem(position))
            }
            return viewHolder
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }
}
