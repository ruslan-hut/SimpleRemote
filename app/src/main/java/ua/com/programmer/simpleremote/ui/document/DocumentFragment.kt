package ua.com.programmer.simpleremote.ui.document

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

        val pagerAdapter = DocumentViewPagerAdapter(this)

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

        val backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.exit_without_saving)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestEditUnlock(onSuccess = {
                            findNavController().popBackStack()
                        })
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.content.collect {
                        viewModel.setDocumentContent(it){
                            sharedViewModel.checkContent()
                        }
                    }
                }
                launch {
                    viewModel.isEditable.collect {
                        binding?.bottomBar?.visibility = if (it) View.VISIBLE else View.GONE
                        backCallback.isEnabled = it
                    }
                }
                launch {
                    viewModel.isLoading.collect {
                        binding?.progressBar?.visibility = if (it) View.VISIBLE else View.INVISIBLE
                    }
                }
            }
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
                if (viewModel.isEditable.value) {
                    requestEditUnlock()
                } else {
                    requestEditLock()
                }
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

    private fun requestEditUnlock(onSuccess: (() -> Unit)? = null) {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.edit_unlock_progress)
            .setCancelable(false)
            .create()
        dialog.show()

        viewModel.requestEditUnlock(
            documentGuid = sharedViewModel.getDocument().guid,
            onSuccess = {
                val bgColor = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorPrimaryContainer)
                val textColor = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnPrimaryContainer)
                dialog.window?.decorView?.setBackgroundColor(bgColor)
                dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(textColor)
                dialog.setMessage(getString(R.string.edit_unlock_success))
                dialog.window?.decorView?.postDelayed({
                    dialog.dismiss()
                    onSuccess?.invoke()
                }, 1000)
            },
            onError = { message ->
                val bgColor = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorErrorContainer)
                val textColor = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnErrorContainer)
                dialog.window?.decorView?.setBackgroundColor(bgColor)
                dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(textColor)
                dialog.setMessage(getString(R.string.edit_unlock_error, message))
                dialog.setCancelable(true)
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok)) { d, _ ->
                    d.dismiss()
                }
            }
        )
    }

    private fun requestEditLock() {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.edit_lock_progress)
            .setCancelable(false)
            .create()
        dialog.show()

        viewModel.requestEditLock(
            documentGuid = sharedViewModel.getDocument().guid,
            onSuccess = {
                val bgColor = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorPrimaryContainer)
                val textColor = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnPrimaryContainer)
                dialog.window?.decorView?.setBackgroundColor(bgColor)
                dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(textColor)
                dialog.setMessage(getString(R.string.edit_lock_success))
                dialog.window?.decorView?.postDelayed({ dialog.dismiss() }, 1000)
            },
            onError = { message ->
                val bgColor = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorErrorContainer)
                val textColor = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnErrorContainer)
                dialog.window?.decorView?.setBackgroundColor(bgColor)
                dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(textColor)
                dialog.setMessage(getString(R.string.edit_lock_error, message))
                dialog.setCancelable(true)
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok)) { d, _ ->
                    d.dismiss()
                }
            }
        )
    }

    private fun onSuccess() {
        sharedViewModel.setDocumentModified(false)
        if (sharedViewModel.autoCloseDocument()) {
            val dialog = AlertDialog.Builder(requireContext())
                .setMessage(R.string.toast_saved)
                .setCancelable(false)
                .create()
            dialog.show()
            dialog.window?.decorView?.postDelayed({
                if (isAdded) {
                    dialog.dismiss()
                    findNavController().popBackStack()
                }
            }, 1500)
        } else {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.toast_saved)
                .setPositiveButton(android.R.string.ok) { d, _ ->
                    d.dismiss()
                }
                .show()
        }
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
): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DocumentTitleFragment()
            else -> DocumentContentFragment()
        }
    }

}
