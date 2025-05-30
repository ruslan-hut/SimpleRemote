package ua.com.programmer.simpleremote.ui.document

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.FragmentDocumentPagedBinding
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel

@AndroidEntryPoint
class DocumentFragment: Fragment(), MenuProvider {

    private val viewModel: DocumentViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentDocumentPagedBinding? = null
    private val binding get() = _binding
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
        _binding = FragmentDocumentPagedBinding.inflate(inflater)

        val menuHost : MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagerAdapter = DocumentViewPagerAdapter(this, viewModel)

        binding?.apply {
            container.adapter = pagerAdapter
            TabLayoutMediator(documentTabs, container) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.tab_title)
                    else -> getString(R.string.title_products)
                }
            }.attach()
            scannerButton.setOnClickListener {
                openCamera()
            }
        }

        sharedViewModel.content.observe(viewLifecycleOwner) {
            viewModel.setDocumentContent(it){
                sharedViewModel.checkContent()
            }
        }
        viewModel.isEditable.observe(viewLifecycleOwner) {
            binding?.bottomBar?.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding?.progressBar?.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        binding?.addItemButton?.setOnClickListener {
            val action = DocumentFragmentDirections.actionDocumentFragmentToCatalogListFragment(
                type = "Товары",
                title = "Товары",
                group = "",
            )
            findNavController().navigate(action)
        }

    }

    private fun openCamera() {
        val action = DocumentFragmentDirections.actionDocumentFragmentToCameraFragment(mode = "barcode")
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
                viewModel.deleteDocument()
            }
            R.id.save_document -> {
                viewModel.saveDocument(
                    document = sharedViewModel.getDocument(),
                    onSuccess = ::onSuccess,
                    onError = ::onError
                )
            }
            R.id.refresh -> {
                sharedViewModel.loadDocumentContent(viewModel.getType(), viewModel.getDocGuid())
            }
            else -> return false
        }
        return true
    }

    private fun onSuccess() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.toast_saved)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                findNavController().popBackStack()
            }
            .show()
    }

    private fun onError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.warning)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .show()
    }

}

private class DocumentViewPagerAdapter(
    fragment: Fragment,
    private val viewModel: DocumentViewModel
): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment: Fragment = when (position) {
            0 -> DocumentTitleFragment(viewModel)
            else -> DocumentContentFragment(viewModel)
        }
        return fragment
    }

}