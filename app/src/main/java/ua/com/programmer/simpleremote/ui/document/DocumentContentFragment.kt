package ua.com.programmer.simpleremote.ui.document

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.DocumentContentItemBinding
import ua.com.programmer.simpleremote.databinding.FragmentDocumentContentBinding
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.isEquals
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel
import kotlin.getValue

@AndroidEntryPoint
class DocumentContentFragment(private val viewModel: DocumentViewModel): Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentDocumentContentBinding? = null
    private val binding get() = _binding

    private var recycler: RecyclerView? = null
    private var listAdapter: ItemsListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDocumentContentBinding.inflate(inflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listAdapter = ItemsListAdapter(
            imageLoader = { code, imageView ->
                sharedViewModel.loadImage(code, imageView)
            },
            onItemChecked = { code, isChecked ->
                sharedViewModel.setItemChecked(code, isChecked)
            },
            onItemClicked = { item ->
                viewModel.onItemClicked(item, ::openProductScreen)
            },
        )
        recycler = binding?.documentContent
        recycler?.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                        viewModel.onListScrolled(firstVisibleItemPosition)
                    }
                }
            })
        }

        sharedViewModel.content.observe(viewLifecycleOwner) {
            listAdapter?.submitList(it)
        }

        sharedViewModel.barcode.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                viewModel.onBarcodeRead(it, ::scrollToProduct)
                sharedViewModel.clearBarcode()
            }
        }

    }

    private fun openProductScreen(product: Product) {
        sharedViewModel.setProduct(product)
        val action = DocumentFragmentDirections.actionDocumentFragmentToItemEditFragment(product.code)
        findNavController().navigate(action)
    }

    private fun scrollToProduct(product: Product) {
        sharedViewModel.setProduct(product)

        if (product.id.isEmpty()) {
            Toast.makeText(requireContext(), R.string.warn_no_barcode, Toast.LENGTH_SHORT).show()
            return
        }

        val position = listAdapter?.findProductPosition(product) ?: -1
        if (position >= 0) {
            Log.d("RC_DocumentContentFragment", "onProductReceived: position=$position")
            viewModel.onListScrolled(position)
            recycler?.smoothScrollToPosition(position)
        }
    }

    override fun onResume() {
        super.onResume()
        val position = viewModel.getScrollPosition()
        recycler?.smoothScrollToPosition(position)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ItemsListAdapter(
        private val imageLoader: (String, ImageView) -> Unit,
        private val onItemChecked: (String, Boolean) -> Unit,
        private val onItemClicked: (Content) -> Unit
    ): ListAdapter<Content, ItemsListAdapter.ItemViewHolder>(DiffCallback) {

        class ItemViewHolder(private var binding: DocumentContentItemBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Content, onItemChecked: (Boolean) -> Unit, imageLoader: (ImageView) -> Unit) {
                binding.apply {
                    itemLineNumber.text = "${item.line}"
                    itemDescription.text = item.description
                    itemCode.text = item.art

                    itemCode2.text = item.code2
                    itemCode2.visibility = if (item.code2.isEmpty()) View.GONE else View.VISIBLE

                    itemCode3.text = item.code3
                    itemCode3.visibility = if (item.code3.isEmpty()) View.GONE else View.VISIBLE

                    itemRest.text = item.rest
                    itemRest.visibility = if (item.rest.isEmpty()) View.GONE else View.VISIBLE
                    itemRestTitle.visibility = if (item.rest.isEmpty()) View.GONE else View.VISIBLE

                    itemPrice.text = item.price
                    itemSum.text = item.sum

                    itemQuantity.text = item.quantity
                    itemUnit.text = item.unit

                    itemCollect.text = item.collect
                    itemUnitCollect.text = item.unit
                    itemUnitCollect.visibility = if (item.collect.isEmpty()) View.GONE else View.VISIBLE

                    itemNotes.text = item.notes
                    itemNotes.visibility = if (item.notes.isEmpty()) View.GONE else View.VISIBLE

                    imageLoader(itemImage)
                    //itemImage.visibility = if (loadImages) View.GONE else View.VISIBLE

                    iconStar.visibility = if (item.modified) View.VISIBLE else View.GONE
                    isChecked.isChecked = item.checked
                    isChecked.setOnCheckedChangeListener { view, isChecked ->
                        if (view.isPressed) {
                            onItemChecked(isChecked)
                        }
                    }

                }
            }
        }

        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<Content>() {

                override fun areItemsTheSame(oldItem: Content, newItem: Content): Boolean {
                    return oldItem.line == newItem.line
                }

                override fun areContentsTheSame(oldItem: Content, newItem: Content): Boolean {
                    return oldItem.isEquals(newItem)
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
            val item = getItem(position)
            holder.bind(getItem(position), { isChecked ->
                onItemChecked(item.code, isChecked)
            }, { imageView ->
                imageLoader(item.code, imageView)
            })
        }

        fun findProductPosition(product: Product): Int {
            return currentList.indexOfFirst { it.code == product.code }
        }
    }
}