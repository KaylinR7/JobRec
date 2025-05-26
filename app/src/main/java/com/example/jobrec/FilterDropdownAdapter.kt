package com.example.jobrec
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
class FilterDropdownAdapter(
    context: Context,
    private val options: List<FilterOption>
) : ArrayAdapter<FilterOption>(context, 0, options) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }
    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_filter_dropdown, parent, false)
        val option = getItem(position)
        val textOption = view.findViewById<TextView>(R.id.textOption)
        val textCount = view.findViewById<TextView>(R.id.textCount)
        textOption.text = option?.name ?: ""
        if (option?.count ?: 0 > 0) {
            textCount.visibility = View.VISIBLE
            textCount.text = option?.count.toString()
        } else {
            textCount.visibility = View.GONE
        }
        return view
    }
    override fun getItem(position: Int): FilterOption? {
        return options.getOrNull(position)
    }
}
data class FilterOption(
    val name: String,
    val count: Int = 0
) {
    override fun toString(): String = name
}
