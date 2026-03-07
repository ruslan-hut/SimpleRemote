package ua.com.programmer.simpleremote.ui.document

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.FragmentItemPlacementBinding
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Place
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel

@AndroidEntryPoint
class ItemPlacementFragment: Fragment() {

    private val viewModel: ItemPlacementViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentItemPlacementBinding? = null
    private val binding get() = _binding
    private val navigationArgs: ItemPlacementFragmentArgs by navArgs()
    private lateinit var adapter: ItemPlacementAdapter

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
        _binding = FragmentItemPlacementBinding.inflate(inflater)
        return binding?.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (productCode.isEmpty()) {
            findNavController().popBackStack()
        }

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
                        it ?: return@collect
                        bind(it)

                        val placeList = product?.contentItem?.place?.toMutableList() ?: mutableListOf()
                        adapter = ItemPlacementAdapter(placeList) { updatedList ->
                            product?.contentItem?.place = updatedList
                        }
                        binding?.placementRecycler?.adapter = adapter
                        binding?.placementRecycler?.layoutManager = LinearLayoutManager(requireContext())
                    }
                }
                launch {
                    sharedViewModel.barcode.collect {
                        if (it.isNotEmpty()) {
                            val matchingPlace = adapter.places.find { place -> place.code == it }
                            if (matchingPlace != null) {
                                matchingPlace.quantity += 1
                            } else {
                                adapter.places.add(Place(quantity = 1, code = it))
                            }
                            adapter.notifyDataSetChanged()
                            sharedViewModel.clearBarcode()
                            product?.contentItem?.place = adapter.places
                        }
                    }
                }
            }
        }



        binding?.buttonCancel?.setOnClickListener {
            findNavController().popBackStack()
        }
        binding?.buttonYes?.setOnClickListener {
            saveProduct()
        }

        val placeList = product?.contentItem?.place?.toMutableList() ?: mutableListOf()
        adapter = ItemPlacementAdapter(placeList) { updatedList ->
            product?.contentItem?.place = updatedList
        }

        val recycler = binding?.placementRecycler
        recycler?.adapter = adapter
        recycler?.layoutManager = LinearLayoutManager(requireContext())

    }


    class ItemPlacementAdapter(
        private val placeList: MutableList<Place>,
        private val onDataChanged: (List<Place>) -> Unit
    ) : RecyclerView.Adapter<ItemPlacementAdapter.ItemViewHolder>() {

        val places: MutableList<Place>
            get() = placeList


        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val quantity: TextView = itemView.findViewById(R.id.text_quantity)
            val placeCode: TextView = itemView.findViewById(R.id.text_place)

            init {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val placeItem = places[pos]
                        showQuantityInputDialog(placeItem, pos)
                    }
                }

                itemView.setOnLongClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        removePlaceAt(pos)
                        true
                    } else {
                        false
                    }
                }
            }

            private fun showQuantityInputDialog(place: Place, position: Int) {
                val context = itemView.context
                val input = EditText(context).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(place.quantity.toString())
                    setSelection(text.length)
                }

                AlertDialog.Builder(context)
                    .setTitle(R.string.enter_quantity)
                    .setView(input)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val newQuantity = input.text.toString().toIntOrNull()
                        if (newQuantity != null && newQuantity >= 0) {
                            placeList[position].quantity = newQuantity
                            notifyItemChanged(position)
                            onDataChanged(placeList)
                        }
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }



            private fun removePlaceAt(position: Int) {
                places.removeAt(position)
                notifyItemRemoved(position)
                onDataChanged(places)
            }
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_placement_row, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val place = places[position]
            holder.quantity.text = place.quantity.toString()
            holder.placeCode.text = place.code
        }

        override fun getItemCount(): Int = places.size
    }


    private fun bind(product: Product) {
        binding?.apply {
            itemDescription.text = product.description
            itemCode.text = product.code
        }
    }

    private fun saveProduct() {
        val currentPlaces = product?.contentItem?.place ?: emptyList()
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.setDocumentContent(
                viewModel.confirmPlace(product, currentPlaces)
            )
            findNavController().popBackStack()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}