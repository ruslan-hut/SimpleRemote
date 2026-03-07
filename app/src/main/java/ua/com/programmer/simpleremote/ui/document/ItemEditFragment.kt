package ua.com.programmer.simpleremote.ui.document

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
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

    private fun showImageDialog(imageView: ImageView) {
        val context = imageView.context
        val dialog = Dialog(context, R.style.FullscreenImageDialog)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_fullscreen_image, null)
        val fullscreenImage = dialogView.findViewById<ImageView>(R.id.fullscreen_image)
        val container = dialogView.findViewById<FrameLayout>(R.id.fullscreen_container)

        fullscreenImage.setImageDrawable(imageView.drawable)

        val metrics = Resources.getSystem().displayMetrics
        val horizontalPadding = (metrics.widthPixels * 0.05).toInt()
        val verticalPadding = (metrics.heightPixels * 0.05).toInt()
        container.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        dialog.setContentView(dialogView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        container.setOnClickListener { dialog.dismiss() }
        fullscreenImage.setOnClickListener { dialog.dismiss() }
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