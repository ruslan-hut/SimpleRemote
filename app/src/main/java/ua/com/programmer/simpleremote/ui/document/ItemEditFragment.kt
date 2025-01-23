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
    private val binding get() = _binding!!
    private val navigationArgs: ItemEditFragmentArgs by navArgs()

    var productCode = ""
    var productBarcode = ""

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (productCode.isEmpty()) {
            findNavController().popBackStack()
        }

        sharedViewModel.product.observe(viewLifecycleOwner) {
            productBarcode = it.barcode
            bind(it)
        }
        sharedViewModel.barcode.observe(viewLifecycleOwner) {
            if (it.isNotEmpty() && it == productBarcode) {
                sharedViewModel.clearBarcode()
                findNavController().popBackStack()
            }
        }
    }

    private fun bind(product: Product) {
        binding.apply {
            itemDescription.text = product.description
            itemCode.text = product.code
            collectUnit.text = product.unit
            restUnit.text = product.unit

            product.contentItem?.let {
                collectEdit.text = it.quantity
                restEdit.text = it.rest
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}