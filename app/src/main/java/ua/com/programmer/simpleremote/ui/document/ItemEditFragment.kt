package ua.com.programmer.simpleremote.ui.document

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.MainActivity
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.FragmentItemEditBinding
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel
import kotlin.getValue

@AndroidEntryPoint
class ItemEditFragment: Fragment(), MenuProvider {

    private val viewModel: ItemEditViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentItemEditBinding? = null
    private val binding get() = _binding
    private val navigationArgs: ItemEditFragmentArgs by navArgs()

    private var product: Product? = null
    private var productCode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productCode = navigationArgs.code ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentItemEditBinding.inflate(inflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (productCode.isEmpty()) {
            findNavController().popBackStack()
        }

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.content.collect {
                        viewModel.loadContent(it)
                    }
                }
                launch {
                    sharedViewModel.product.collect {
                        product = it
                        it?.let { bind(it) }
                    }
                }
                launch {
                    sharedViewModel.barcode.collect {
                        if (sharedViewModel.collectMode() && it == product?.barcode) {
                            if (sharedViewModel.confirmWithScan()) {
                                binding?.editQuantity?.setText(binding?.collectEdit?.text.toString())
                                saveProduct()
                            } else {
                                increaseQuantity()
                            }
                        }
                    }
                }
            }
        }
        binding?.editQuantity?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveProduct()
                true
            } else {
                false
            }
        }
        binding?.buttonCancel?.setOnClickListener {
            findNavController().popBackStack()
        }
        binding?.buttonYes?.setOnClickListener {
            saveProduct()
        }

    }

    private fun bind(product: Product) {
        binding?.apply {
            itemDescription.text = product.description
            itemCode.text = product.code
            collectUnit.text = product.unit
            restUnit.text = product.unit
            itemUnit.text = product.unit
            editNotes.setText(product.notes)

            product.contentItem?.let {
                collectEdit.text = it.quantity
                restEdit.text = it.rest
                editQuantity.setText(it.collect)
                if (it.userImage.isNotEmpty()) {
                    imageLine.visibility = View.VISIBLE
                    sharedViewModel.loadLocalImage(it.userImage, itemImage)
                    itemImage.setOnClickListener { showImageDialog(itemImage) }
                } else {
                    imageLine.visibility = View.GONE
                }
            }
        }
    }

    private fun saveProduct() {
        val qty = binding?.editQuantity?.text.toString()
        val notes = binding?.editNotes?.text.toString()
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.setDocumentContent(
                viewModel.confirmQuantity(product, qty, notes)
            )
            findNavController().popBackStack()
        }
    }

    private fun increaseQuantity() {
        val qty = binding?.editQuantity?.text.toString().toIntOrNull()?.plus(1)?.toString() ?: "1"
        binding?.editQuantity?.setText(qty)
        val notes = binding?.editNotes?.text.toString()
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.setDocumentContent(
                viewModel.confirmQuantity(product, qty, notes)
            )
        }
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.item_edit_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.take_photo -> {
                findNavController().navigate(
                    ItemEditFragmentDirections.actionItemEditFragmentToCameraFragment(mode = "photo")
                )
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (requireActivity() as? MainActivity)?.hideSoftKeyboard()
    }
}