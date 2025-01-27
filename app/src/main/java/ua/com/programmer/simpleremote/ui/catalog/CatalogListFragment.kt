package ua.com.programmer.simpleremote.ui.catalog

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
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.databinding.CatalogListItemGoodsBinding
import ua.com.programmer.simpleremote.databinding.CatalogListItemGroupBinding
import ua.com.programmer.simpleremote.databinding.FragmentSwipeListBinding
import ua.com.programmer.simpleremote.entity.Catalog
import ua.com.programmer.simpleremote.entity.getPrice
import ua.com.programmer.simpleremote.entity.getRest
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel

@AndroidEntryPoint
class CatalogListFragment: Fragment() {

    private val viewModel: CatalogListViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentSwipeListBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: CatalogListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setCatalogType(navigationArgs.type, navigationArgs.title, navigationArgs.group, navigationArgs.docGuid)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwipeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter =
            ItemsListAdapter(
                onItemClicked = { item ->
                    if (item.isGroup == 1) {
                        nextPage(item.code)
                    }
                },
            )
        val recycler = binding.listRecycler
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())

        viewModel.elements.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        binding.listSwipe.setOnRefreshListener {
            binding.listSwipe.isRefreshing = false
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.listSwipe.isRefreshing = it
        }
    }

    private fun nextPage(group: String) {
        val action = CatalogListFragmentDirections.actionCatalogListFragmentSelf(viewModel.type, viewModel.title, group, viewModel.docGuid)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ItemsListAdapter(
        private val onItemClicked: (Catalog) -> Unit
    ): ListAdapter<Catalog, ItemsListAdapter.ItemViewHolder>(DiffCallback) {

        abstract class ItemViewHolder(binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
            abstract fun bind(
                catalog: Catalog,
                onItemClicked: (Catalog) -> Unit
            )
        }

        class ElementViewHolder(private var binding: CatalogListItemGoodsBinding): ItemViewHolder(binding as ViewBinding) {
            override fun bind(catalog: Catalog, onItemClicked: (Catalog) -> Unit) {
                binding.apply {
                    itemCode.text = catalog.art
                    itemDescription.text = catalog.description
                    groupDescription.text = catalog.groupName

                    val price = catalog.getPrice()
                    if (price.isEmpty()) {
                        priceTitle.visibility = View.INVISIBLE
                        itemPrice.visibility = View.INVISIBLE
                    } else {
                        itemPrice.text = price
                        itemPrice.visibility = View.VISIBLE
                        priceTitle.visibility = View.VISIBLE
                    }

                    val rest = catalog.getRest()
                    if (rest.isEmpty()) {
                        restContainer.visibility = View.INVISIBLE
                    } else {
                        restValue.text = rest
                        restContainer.visibility = View.VISIBLE
                    }
                }
            }
        }

        class GroupViewHolder(private var binding: CatalogListItemGroupBinding): ItemViewHolder(binding as ViewBinding) {
            override fun bind(catalog: Catalog, onItemClicked: (Catalog) -> Unit) {
                binding.apply {
                    itemDescription.text = catalog.description
                }
            }
        }

        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<Catalog>() {

                override fun areItemsTheSame(oldItem: Catalog, newItem: Catalog): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: Catalog, newItem: Catalog): Boolean {
                    return oldItem == newItem
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val viewHolder = when (viewType) {
                0 -> {
                    val binding = CatalogListItemGoodsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    ElementViewHolder(binding)
                }
                else -> {
                    val binding = CatalogListItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    GroupViewHolder(binding)
                }
            }
            viewHolder.itemView.setOnClickListener {
                val position = viewHolder.absoluteAdapterPosition
                onItemClicked(getItem(position))
            }
            return viewHolder
        }

        override fun getItemViewType(position: Int): Int {
            val product = getItem(position)
            return product.isGroup
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(getItem(position), onItemClicked)
        }
    }
}