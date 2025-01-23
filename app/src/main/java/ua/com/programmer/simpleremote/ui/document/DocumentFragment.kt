package ua.com.programmer.simpleremote.ui.document

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.DocumentContentItemBinding
import ua.com.programmer.simpleremote.databinding.FragmentDocumentBinding
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel

@AndroidEntryPoint
class DocumentFragment: Fragment(), MenuProvider {

    private val viewModel: DocumentViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentDocumentBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: DocumentFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setDocumentType(navigationArgs.type, navigationArgs.title)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDocumentBinding.inflate(inflater)

        val menuHost : MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.document.observe(viewLifecycleOwner) {
            it?.let {
                bind(it)
                viewModel.setDocumentId(it.guid)
            }
        }
        sharedViewModel.barcode.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                viewModel.onBarcodeRead(it)
                sharedViewModel.clearBarcode()
            }
        }

        viewModel.content.observe(viewLifecycleOwner) {
            sharedViewModel.setDocumentContent(it)
        }
        viewModel.isEditable.observe(viewLifecycleOwner) {
            binding.bottomBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
        viewModel.product.observe(viewLifecycleOwner) { product ->
            product?.let {
                onProductReceived(it)
                viewModel.resetProduct()
            }
        }

        val adapter = ItemsListAdapter(
            onItemClicked = { item ->
                viewModel.onItemClicked(item)
            },
        )
        val recycler = binding.documentContent
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())

        sharedViewModel.content.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

    }

    private fun bind(item: Document) {
        binding.apply {
            documentTitle.text = viewModel.title
            documentNumber.text = item.number
            documentDate.text = item.date
            documentHeaderNotes.text = item.notes

            documentContractor.text = item.contractor
            documentHeaderContractor.visibility = if (item.contractor.isEmpty()) View.GONE else View.VISIBLE

            if (item.cacheGUID.isNotEmpty()) {
                documentIcon.setImageResource(R.drawable.sharp_help_outline_24)
            }else if (item.isDeleted == 1) {
                documentIcon.setImageResource(R.drawable.twotone_close_24)
            }else if (item.isProcessed == 1) {
                documentIcon.setImageResource(R.drawable.twotone_check_box_24)
            }else{
                documentIcon.setImageResource(R.drawable.twotone_check_box_outline_blank_24)
            }

            if (item.contractor.isNotEmpty()) {
                documentContractor.text = item.contractor
                documentContractor.visibility = View.VISIBLE
            } else {
                documentContractor.visibility = View.GONE
            }

            if (item.field1.isEmpty()) {
                documentHeaderField1.visibility = View.GONE
            } else {
                documentField1Name.text = item.field1
                documentHeaderField1.visibility = View.VISIBLE
            }
            if (item.field2.isEmpty()) {
                documentHeaderField2.visibility = View.GONE
            } else {
                documentField2Name.text = item.field2
                documentHeaderField2.visibility = View.VISIBLE
            }
            if (item.field3.isEmpty()) {
                documentHeaderField3.visibility = View.GONE
            } else {
                documentField3Name.text = item.field3
                documentHeaderField3.visibility = View.VISIBLE
            }
            if (item.field4.isEmpty()) {
                documentHeaderField4.visibility = View.GONE
            } else {
                documentField4Name.text = item.field4
                documentHeaderField4.visibility = View.VISIBLE
            }

        }
    }

    private fun onProductReceived(product: Product) {
        sharedViewModel.setProduct(product)

        if (product.id.isEmpty()) {
            Toast.makeText(requireContext(), R.string.warn_no_barcode, Toast.LENGTH_SHORT).show()
            return
        }

        val action = DocumentFragmentDirections.actionDocumentFragmentToItemEditFragment(product.code)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.document_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.edit_document -> {
                viewModel.enableEdit()
            }
            R.id.delete_document -> {
                //
            }
            R.id.save_document -> {
                viewModel.saveDocument()
            }
            R.id.refresh -> {
                //
            }
            else -> return false
        }
        return true
    }

    private class ItemsListAdapter(
        private val onItemClicked: (Content) -> Unit
    ): ListAdapter<Content, ItemsListAdapter.ItemViewHolder>(DiffCallback) {

        class ItemViewHolder(private var binding: DocumentContentItemBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Content) {
                binding.apply {
                    itemLineNumber.text = "${item.line}"
                    itemDescription.text = item.description
                    itemCode.text = item.code

                    itemCode2.text = item.code2
                    itemCode2.visibility = if (item.code2.isEmpty()) View.GONE else View.VISIBLE

                    itemCode3.text = item.code3
                    itemCode3.visibility = if (item.code3.isEmpty()) View.GONE else View.VISIBLE

                    itemRest.text = item.rest
                    itemRest.visibility = if (item.rest.isEmpty()) View.GONE else View.VISIBLE
                    itemRestTitle.visibility = if (item.rest.isEmpty()) View.GONE else View.VISIBLE

                    itemPrice.text = item.price
                    itemQuantity.text = item.quantity
                    itemUnit.text = item.unit
                    itemSum.text = item.sum
                    itemCollect.text = item.collect

                    itemNotes.text = item.notes
                    //itemNotes.visibility = if (item.notes.isEmpty()) View.GONE else View.VISIBLE

                    itemImage.visibility = if (item.image.isEmpty()) View.GONE else View.VISIBLE
                    //itemImage.setImageURI(item.image.toUri())

                    iconStar.visibility = if (item.modified) View.VISIBLE else View.GONE
                    isChecked.isChecked = item.checked

                }
            }
        }

        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<Content>() {

                override fun areItemsTheSame(oldItem: Content, newItem: Content): Boolean {
                    return oldItem.line == newItem.line
                }

                override fun areContentsTheSame(oldItem: Content, newItem: Content): Boolean {
                    return oldItem == newItem
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val viewHolder = ItemViewHolder(
                DocumentContentItemBinding.inflate(
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