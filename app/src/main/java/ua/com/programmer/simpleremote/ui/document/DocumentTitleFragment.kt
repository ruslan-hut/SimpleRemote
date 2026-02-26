package ua.com.programmer.simpleremote.ui.document

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import ua.com.programmer.simpleremote.R
import ua.com.programmer.simpleremote.databinding.FragmentDocumentTitleBinding
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.isEmpty
import ua.com.programmer.simpleremote.ui.shared.SharedViewModel
import kotlin.getValue

@AndroidEntryPoint
class DocumentTitleFragment(private val viewModel: DocumentViewModel): Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var _binding : FragmentDocumentTitleBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDocumentTitleBinding.inflate(inflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.document.observe(viewLifecycleOwner) {
            it?.let {
                bind(it)
                viewModel.setDocumentId(it.guid){type, guid ->
                    sharedViewModel.loadDocumentContent(type, guid)
                }
            }
        }
        viewModel.count.observe(viewLifecycleOwner) {
            binding?.documentArticles?.text = "$it"
        }
        binding?.editPlaces?.doOnTextChanged { text, _, _, _ ->
            // Only update the ViewModel if the text has actually changed
            // from what the ViewModel currently holds for placesCollected.
            // This assumes your SharedViewModel has a way to get the current
            // placesCollected value or that you compare it to the last value
            // that was set from the document.
            // For simplicity, we'll compare with the current document's value.
            if (text.toString() != sharedViewModel.document.value?.placesCollected) {
                sharedViewModel.setDocumentPlacesCollected(text.toString())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bind(item: Document) {
        binding?.apply {
            documentTitle.text = viewModel.getTitle()
            documentNumber.text = item.number
            documentDate.text = item.date

            // Check if the current text is different before setting it
            if (editPlaces.text.toString() != item.placesCollected) {
                editPlaces.setText(item.placesCollected) // TextView.BufferType.EDITABLE is the default for EditText
            }

            documentNotes.text = item.notes
            documentNotes.setOnClickListener {
                showEditNotesDialog(item)
            }

            documentContractor.text = item.contractor
            documentHeaderContractor.visibility = if (item.contractor.isEmpty()) View.GONE else View.VISIBLE

            documentIsChecked.isChecked = item.checked
            documentIsChecked.setOnCheckedChangeListener { _, isChecked ->
                sharedViewModel.setDocumentChecked(isChecked)
            }

            if (item.cacheGUID.isNotEmpty()) {
                documentIcon.setImageResource(R.drawable.sharp_help_outline_24)
            }else if (item.isDeleted == 1) {
                documentIcon.setImageResource(R.drawable.twotone_close_24)
            }else if (item.isProcessed == 1) {
                documentIcon.setImageResource(R.drawable.baseline_bookmark_added_24)
            }else{
                documentIcon.setImageResource(R.drawable.baseline_bookmark_border_24)
            }

            if (item.contractor.isNotEmpty()) {
                documentContractor.text = item.contractor
                documentContractor.visibility = View.VISIBLE
            } else {
                documentContractor.visibility = View.GONE
            }

            if (item.field1.isEmpty()) {
                documentHeaderField1.visibility = View.GONE
            } else {
                documentField1Name.text = item.field1.description
                documentField1Value.text = item.field1.value
                documentHeaderField1.visibility = View.VISIBLE
            }
            if (item.field2.isEmpty()) {
                documentHeaderField2.visibility = View.GONE
            } else {
                documentField2Name.text = item.field2.description
                documentField2Value.text = item.field2.value
                documentHeaderField2.visibility = View.VISIBLE
            }
            if (item.field3.isEmpty()) {
                documentHeaderField3.visibility = View.GONE
            } else {
                documentField3Name.text = item.field3.description
                documentField3Value.text = item.field3.value
                documentHeaderField3.visibility = View.VISIBLE
            }
            if (item.field4.isEmpty()) {
                documentHeaderField4.visibility = View.GONE
            } else {
                documentField4Name.text = item.field4.description
                documentField4Value.text = item.field4.value
                documentHeaderField4.visibility = View.VISIBLE
            }

            documentPrice.text = item.sum
        }
    }

    @SuppressLint("InflateParams")
    private fun showEditNotesDialog(item: Document) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_edit_text, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.edit_text)

        editText.setText(item.notes)

        with(builder) {
            setTitle(R.string.notes)
            setView(dialogLayout)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val newNotes = editText.text.toString()
                sharedViewModel.setDocumentNotes(newNotes)
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

}