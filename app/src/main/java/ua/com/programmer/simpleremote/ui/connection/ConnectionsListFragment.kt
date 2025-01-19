package ua.com.programmer.simpleremote.ui.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.dao.entity.getGuid
import ua.com.programmer.simpleremote.dao.entity.isEquals
import ua.com.programmer.simpleremote.databinding.ConnectionsListItemBinding
import ua.com.programmer.simpleremote.databinding.FragmentListBinding

@AndroidEntryPoint
class ConnectionsListFragment: Fragment() {

    private val viewModel: ConnectionsListViewModel by viewModels()
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ItemsListAdapter(
            onItemClicked = { item ->
                viewModel.setCurrentConnection(item.guid)
            },
            onItemLongClicked = { item ->
                openConnection(item)
            }
        )
        val recycler = binding.listRecycler
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())

        viewModel.connections.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        binding.fab.setOnClickListener{ openConnection(null) }
    }

    private fun openConnection(item: ConnectionSettings?) {
        val action = ConnectionsListFragmentDirections.actionConnectionsListFragmentToConnectionFragment(item?.guid)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ItemsListAdapter(
        private val onItemClicked: (ConnectionSettings) -> Unit,
        private val onItemLongClicked: (ConnectionSettings) -> Unit)
        : ListAdapter<ConnectionSettings, ItemsListAdapter.ItemViewHolder>(DiffCallback) {

        class ItemViewHolder(private var binding: ConnectionsListItemBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind(item: ConnectionSettings) {
                binding.apply {
                    description.text = item.description
                    server.text = item.serverAddress
                    user.text = item.user
                    guid.text = item.getGuid()
                    activeIcon.visibility = if (item.isCurrent == 1) View.VISIBLE else View.INVISIBLE
                }
            }
        }

        companion object {
            private val DiffCallback = object : DiffUtil.ItemCallback<ConnectionSettings>() {

                override fun areItemsTheSame(oldItem: ConnectionSettings, newItem: ConnectionSettings): Boolean {
                    return oldItem.guid == newItem.guid
                }

                override fun areContentsTheSame(oldItem: ConnectionSettings, newItem: ConnectionSettings): Boolean {
                    return oldItem.isEquals(newItem)
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val viewHolder = ItemViewHolder(
                ConnectionsListItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            viewHolder.itemView.setOnClickListener {
                val position = viewHolder.absoluteAdapterPosition
                onItemClicked(getItem(position))
            }
            viewHolder.itemView.setOnLongClickListener {
                val position = viewHolder.absoluteAdapterPosition
                onItemLongClicked(getItem(position))
                true
            }
            return viewHolder
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }
}