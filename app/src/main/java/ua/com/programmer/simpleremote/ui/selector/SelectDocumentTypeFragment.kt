package ua.com.programmer.simpleremote.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.databinding.FragmentSelectDataTypeBinding
import ua.com.programmer.simpleremote.databinding.SelectListItemBinding
import ua.com.programmer.simpleremote.entity.DataType

@AndroidEntryPoint
class SelectDocumentTypeFragment: Fragment() {

    private val viewModel: SelectorViewModel by activityViewModels()
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

        viewModel.documents.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ItemsListAdapter(
        private val onItemClicked: (DataType) -> Unit
    ): ListAdapter<DataType, ItemsListAdapter.ItemViewHolder>(DiffCallback) {

        class ItemViewHolder(private var binding: SelectListItemBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind(item: DataType) {
                binding.apply {
                    itemDescription.text = item.description
                }
            }
        }

        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<DataType>() {

                override fun areItemsTheSame(oldItem: DataType, newItem: DataType): Boolean {
                    return oldItem.code == newItem.code
                }

                override fun areContentsTheSame(oldItem: DataType, newItem: DataType): Boolean {
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