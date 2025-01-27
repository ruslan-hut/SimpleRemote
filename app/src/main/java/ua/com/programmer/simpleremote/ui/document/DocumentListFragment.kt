package ua.com.programmer.simpleremote.ui.document

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.databinding.DocumentsListItemBinding
import ua.com.programmer.simpleremote.databinding.FragmentDocumentsListBinding
import ua.com.programmer.simpleremote.entity.Document
import kotlin.getValue
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.entity.isEmpty
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel

@AndroidEntryPoint
class DocumentListFragment: Fragment() {

    private val viewModel: DocumentListViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentDocumentsListBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: DocumentListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setDocumentType(navigationArgs.type, navigationArgs.title)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDocumentsListBinding.inflate(inflater, container, false)
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

        viewModel.documents.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        binding.documentsSwipe.setOnRefreshListener {
            binding.documentsSwipe.isRefreshing = false
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.documentsSwipe.isRefreshing = it
        }
    }

    private fun openDocument(type: String, title: String) {
        val action = DocumentListFragmentDirections.actionDocumentListFragmentToDocumentFragment(title, type)
        this.findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                    itemWarehouse.text = item.warehouse

                    itemContractor.text = item.contractor
                    itemContractor.visibility = if (item.contractor.isEmpty()) View.GONE else View.VISIBLE

                    itemField1.text = item.field1.value
                    itemField1.visibility = if (item.field1.isEmpty()) View.GONE else View.VISIBLE
                    itemField2.text = item.field2.value
                    itemField2.visibility = if (item.field2.isEmpty()) View.GONE else View.VISIBLE
                    itemField3.text = item.field3.value
                    itemField3.visibility = if (item.field3.isEmpty()) View.GONE else View.VISIBLE
                    itemField4.text = item.field4.value
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
                        itemIcon.setImageResource(R.drawable.twotone_check_box_24)
                    }else{
                        itemIcon.setImageResource(R.drawable.twotone_check_box_outline_blank_24)
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