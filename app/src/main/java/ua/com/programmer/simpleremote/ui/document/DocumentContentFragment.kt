package ua.com.programmer.simpleremote.ui.document

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.DocumentContentItemBinding
import ua.com.programmer.simpleremote.databinding.FragmentDocumentContentBinding
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.isEquals
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel

@AndroidEntryPoint
class DocumentContentFragment: Fragment() {

    private val viewModel: DocumentViewModel by viewModels(ownerProducer = { requireParentFragment() })
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
            setItemViewCacheSize(20)
            setHasFixedSize(true)
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

        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!
        val backgroundPaint = Paint().apply { color = 0xFFF44336.toInt() }

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun isItemViewSwipeEnabled(): Boolean {
                return viewModel.isEditable.value
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val item = listAdapter?.currentList?.getOrNull(position) ?: return
                sharedViewModel.setItemDeleted(item.code)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (dX < 0) {
                    val bg = RectF(
                        itemView.right + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                    c.drawRect(bg, backgroundPaint)

                    val iconSize = deleteIcon.intrinsicHeight
                    val iconTop = itemView.top + (itemView.height - iconSize) / 2
                    val iconMargin = (itemView.height - iconSize) / 2
                    val iconLeft = itemView.right - iconMargin - iconSize
                    val iconRight = itemView.right - iconMargin
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconTop + iconSize)
                    deleteIcon.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recycler)

        binding?.retryButton?.setOnClickListener {
            binding?.emptyState?.visibility = View.GONE
            sharedViewModel.loadDocumentContent(viewModel.getType(), viewModel.getDocGuid())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.content.collect {
                        listAdapter?.submitList(it)
                        binding?.emptyState?.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    sharedViewModel.barcode.collect {
                        if (!viewModel.isEditable.value) {
                            showNotEditableWarning()
                        } else if (sharedViewModel.placementMode()) {
                            viewModel.onBarcodeRead(it, ::scrollToProduct)
                        } else if (sharedViewModel.editMode()) {
                            viewModel.addProduct(it, ::scrollToProduct)
                        }
                    }
                }
            }
        }
    }

    private fun showNotEditableWarning() {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.warn_not_editable)
            .setCancelable(true)
            .create()
        dialog.show()
        dialog.window?.decorView?.postDelayed({ dialog.dismiss() }, 1500)
    }

    private fun openProductScreen(product: Product) {
        sharedViewModel.setProduct(product)
        val action = DocumentFragmentDirections.actionDocumentFragmentToItemEditFragment(product.code)
        findNavController().navigate(action)
    }

    private fun openProductPlacementScreen(product: Product) {
        sharedViewModel.setProduct(product)
        val action = DocumentFragmentDirections.actionDocumentFragmentToItemPlacementFragment(product.code)
        findNavController().navigate(action)
    }

    private fun scrollToProduct(product: Product) {
        if (product.id.isEmpty()) {
            Toast.makeText(requireContext(), R.string.warn_no_barcode, Toast.LENGTH_SHORT).show()
            return
        }

        sharedViewModel.setProductOnScan(product)

        val position = listAdapter?.findProductPosition(product) ?: -1
        if (position >= 0) {
            viewModel.onListScrolled(position)
            recycler?.smoothScrollToPosition(position)
            if (sharedViewModel.placementMode()){
                openProductPlacementScreen(product)
            }
        }else{
            Toast.makeText(requireContext(), R.string.no_product_in_document, Toast.LENGTH_SHORT).show()
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
                    itemImage.setOnClickListener {
                        showImageDialog(itemImage)
                    }

                    val restValue = item.rest.replace(",", ".").toDoubleOrNull() ?: 0.0
                    lowRestOverlay.visibility = if (restValue <= 0) View.VISIBLE else View.GONE
                    uncheckedOverlay.visibility = if (!item.checked && !item.modified) View.VISIBLE else View.GONE
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
                    return oldItem.code == newItem.code
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
                imageLoader(item.image, imageView)
            })
        }

        fun findProductPosition(product: Product): Int {
            return currentList.indexOfFirst { it.code == product.code }
        }
    }
}
