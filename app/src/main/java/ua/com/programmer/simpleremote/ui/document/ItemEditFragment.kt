package ua.com.programmer.simpleremote.ui.document

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.databinding.FragmentItemEditBinding
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel
import kotlin.getValue

@AndroidEntryPoint
class ItemEditFragment: Fragment() {

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

        sharedViewModel.content.observe(viewLifecycleOwner) {
            viewModel.loadContent(it)
        }
        sharedViewModel.product.observe(viewLifecycleOwner) {
            product = it
            bind(it)
        }
        sharedViewModel.barcode.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                if (sharedViewModel.collectMode() && it == product?.barcode){
                    if (sharedViewModel.confirmWithScan()) {
                        binding?.editQuantity?.setText(binding?.collectEdit?.text.toString())
                        saveProduct()
                    } else {
                        increaseQuantity()
                    }
                }

                sharedViewModel.clearBarcode()
            }
        }
        binding?.buttonCancel?.setOnClickListener {
            findNavController().popBackStack()
        }
        binding?.buttonYes?.setOnClickListener {
            saveProduct()
        }
        binding?.itemImage?.setOnClickListener {
            findNavController().navigate(
                ItemEditFragmentDirections.actionItemEditFragmentToCameraFragment(mode = "photo")
            )
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
                sharedViewModel.loadLocalImage(it.image, itemImage)
            }
        }
    }

    private fun saveProduct() {
        val qty = binding?.editQuantity?.text.toString()
        val notes = binding?.editNotes?.text.toString()
        sharedViewModel.setDocumentContent(
            viewModel.confirmQuantity(product, qty, notes)
        )
        findNavController().popBackStack()
    }

    private fun increaseQuantity() {
        val qty = binding?.editQuantity?.text.toString().toIntOrNull()?.plus(1)?.toString() ?: "1"
        binding?.editQuantity?.setText(qty)
        val notes = binding?.editNotes?.text.toString()
        sharedViewModel.setDocumentContent(
            viewModel.confirmQuantity(product, qty, notes)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}