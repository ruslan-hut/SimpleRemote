package ua.com.programmer.simpleremote.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.databinding.FragmentSelectDataTypeBinding
import ua.com.programmer.simpleremote.databinding.SelectListItemBinding
import ua.com.programmer.simpleremote.entity.ListType
import kotlin.getValue

@AndroidEntryPoint
class SelectCatalogTypeFragment: Fragment() {

    private val viewModel: SelectorViewModel by viewModels()
    private var _binding : FragmentSelectDataTypeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSelectDataTypeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ItemsListAdapter(
            onItemClicked = { item ->
                //viewModel.setCurrentConnection(item.guid)
            },
        )
        val recycler = binding.recycler
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())

        viewModel.catalogs.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.cardNoData.visibility = View.VISIBLE
                binding.recycler.visibility = View.GONE
            } else {
                binding.cardNoData.visibility = View.GONE
                binding.recycler.visibility = View.VISIBLE
            }
            adapter.submitList(it)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.listSwipe.isRefreshing = it
        }

        binding.cardNoData.setOnClickListener {
            binding.cardNoData.visibility = View.GONE
            viewModel.tryReconnect()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ItemsListAdapter(
        private val onItemClicked: (ListType) -> Unit
    ): ListAdapter<ListType, ItemsListAdapter.ItemViewHolder>(DiffCallback) {

        class ItemViewHolder(private var binding: SelectListItemBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind(item: ListType) {
                binding.apply {
                    itemDescription.text = item.description
                }
            }
        }

        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<ListType>() {

                override fun areItemsTheSame(oldItem: ListType, newItem: ListType): Boolean {
                    return oldItem.code == newItem.code
                }

                override fun areContentsTheSame(oldItem: ListType, newItem: ListType): Boolean {
                    return oldItem == newItem
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val viewHolder = ItemViewHolder(
                SelectListItemBinding.inflate(
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