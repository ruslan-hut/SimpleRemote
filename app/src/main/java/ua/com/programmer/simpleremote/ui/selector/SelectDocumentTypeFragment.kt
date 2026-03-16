package ua.com.programmer.simpleremote.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.programmer.simpleremote.databinding.FragmentSelectDataTypeBinding
import ua.com.programmer.simpleremote.databinding.SelectListItemBinding
import ua.com.programmer.simpleremote.entity.ListType
import ua.com.programmer.simpleremote.repository.CachedDocumentData
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel

@AndroidEntryPoint
class SelectDocumentTypeFragment: Fragment() {

    private val viewModel: SelectorViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentSelectDataTypeBinding? = null
    private val binding get() = _binding!!
    private var restoreChecked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectDataTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ItemsListAdapter(
            onItemClicked = { item ->
                openDocumentList(item.code, item.description)
            },
        )
        val recycler = binding.recycler
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.documents.collect {
                        adapter.submitList(it)
                    }
                }
                launch {
                    viewModel.isLoading.collect {
                        binding.listSwipe.isRefreshing = it
                    }
                }
                launch {
                    viewModel.isError.collect {
                        binding.cardNoData.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    sharedViewModel.connection.collect {
                        if (it != null && !restoreChecked) {
                            restoreChecked = true
                            checkCachedDocuments()
                        }
                    }
                }
            }
        }

        binding.cardNoData.setOnClickListener {
            binding.cardNoData.visibility = View.GONE
            viewModel.tryReconnect()
        }
        binding.listSwipe.setOnRefreshListener {
            viewModel.tryReconnect()
        }
    }

    private fun checkCachedDocuments() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cached = withContext(Dispatchers.IO) {
                sharedViewModel.getCachedDocuments()
            }
            if (cached.isNotEmpty()) {
                showRestoreDialog(cached)
            }
        }
    }

    private fun showRestoreDialog(cached: List<CachedDocumentData>) {
        val data = cached.first()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.restore_unsaved_title)
            .setMessage(getString(R.string.restore_unsaved_message, data.displayTitle))
            .setPositiveButton(R.string.restore) { _, _ ->
                restoreDocument(data)
            }
            .setNegativeButton(R.string.discard) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    sharedViewModel.discardCachedDocument(data.document.guid)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun restoreDocument(data: CachedDocumentData) {
        sharedViewModel.restoreCachedDocument(data.document, data.content)
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.selectDocumentTypeFragment) return
        val action = SelectDocumentTypeFragmentDirections.actionSelectDocumentTypeFragmentToDocumentFragment(
            type = data.documentType,
            title = data.displayTitle,
        )
        navController.navigate(action)
    }

    private fun openDocumentList(type: String, title: String) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.selectDocumentTypeFragment) return
        val action = SelectDocumentTypeFragmentDirections.actionSelectDocumentTypeFragmentToDocumentListFragment(type, title)
        navController.navigate(action)
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
