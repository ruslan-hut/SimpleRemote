package ua.com.programmer.simpleremote.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.R
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
        viewModel.setCatalogType(navigationArgs.type, navigationArgs.title, navigationArgs.group)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwipeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val pickerMode: Boolean by lazy { navigationArgs.pickerMode }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()

        val adapter =
            ItemsListAdapter(
                onItemClicked = { item ->
                    if (item.isGroup == 1) {
                        nextPage(item.code)
                    }

                    if (item.isGroup == 0) {
                        if (pickerMode) {
                            sharedViewModel.setSelectedCatalogItem(item)
                            findNavController().popBackStack(R.id.documentListFragment, false)
                        } else {
                            sharedViewModel.addProduct(item) {
                                findNavController().popBackStack(R.id.documentFragment, false)
                            }
                        }
                    }
                },
            )
        val recycler = binding.listRecycler
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.elements.collect {
                        adapter.submitList(it)
                    }
                }
                launch {
                    viewModel.isLoading.collect {
                        binding.listSwipe.isRefreshing = it
                    }
                }
            }
        }

        binding.listSwipe.setOnRefreshListener {
            binding.listSwipe.isRefreshing = false
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.catalog_list_menu, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView
                searchView.queryHint = getString(R.string.search)
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        val text = newText ?: ""
                        if (text.length > 2) {
                            viewModel.search(text)
                        } else if (text.isEmpty()) {
                            viewModel.clearSearch()
                        }
                        return true
                    }
                })
                searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean = true

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        viewModel.clearSearch()
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun nextPage(group: String) {
        val action = CatalogListFragmentDirections.actionCatalogListFragmentSelf(viewModel.type, viewModel.title, group, pickerMode)
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

                    itemView.setOnClickListener {
                        onItemClicked(catalog)
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
