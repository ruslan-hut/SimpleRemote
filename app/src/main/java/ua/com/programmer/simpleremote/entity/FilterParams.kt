package ua.com.programmer.simpleremote.entity

import android.content.Context
import ua.com.programmer.simpleremote.R

data class FilterParams(
    var documentNumber: String = "",
    var contractor: String = "",
    var warehouse: String = "",
    var date: String = "",
){
    fun getFilterString(context: Context): String {
        val filters = mutableListOf<String>()
        if (documentNumber.isNotEmpty()) {
            filters.add(context.getString(R.string.filter_label_number, documentNumber))
        }
        if (contractor.isNotEmpty()) {
            filters.add(context.getString(R.string.filter_label_contractor, contractor))
        }
        if (warehouse.isNotEmpty()) {
            filters.add(context.getString(R.string.filter_label_warehouse, warehouse))
        }
        if (date.isNotEmpty()) {
            filters.add(context.getString(R.string.filter_label_date, date))
        }
        return filters.joinToString(", ")
    }

    fun isFilterSet(): Boolean {
        return documentNumber.isNotEmpty() || contractor.isNotEmpty() || warehouse.isNotEmpty() || date.isNotEmpty()
    }
}