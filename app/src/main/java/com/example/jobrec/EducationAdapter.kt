package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class EducationAdapter : RecyclerView.Adapter<EducationAdapter.EducationViewHolder>() {
    
    private val educationList = mutableListOf<Education>()
    private val viewHolders = mutableMapOf<Int, EducationViewHolder>()
    
    init {
        // Add an empty education item by default
        addNewEducation()
    }
    
    fun addNewEducation() {
        educationList.add(Education())
        notifyItemInserted(educationList.size - 1)
    }
    
    fun clearEducationList() {
        educationList.clear()
        viewHolders.clear()
        notifyDataSetChanged()
    }
    
    fun addEducation(education: Education) {
        educationList.add(education)
        notifyItemInserted(educationList.size - 1)
    }
    
    fun getEducationList(): List<Education> {
        // Update the data from the views before returning
        for (i in educationList.indices) {
            val holder = viewHolders[i]
            if (holder != null) {
                val education = educationList[i]
                education.institution = holder.institutionInput.text.toString()
                education.degree = holder.degreeInput.text.toString()
                education.startDate = holder.startDateInput.text.toString()
                education.endDate = holder.endDateInput.text.toString()
            }
        }
        return educationList
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EducationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_education, parent, false)
        return EducationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: EducationViewHolder, position: Int) {
        viewHolders[position] = holder
        val education = educationList[position]
        holder.institutionInput.setText(education.institution)
        holder.degreeInput.setText(education.degree)
        holder.startDateInput.setText(education.startDate)
        holder.endDateInput.setText(education.endDate)
    }
    
    override fun onViewRecycled(holder: EducationViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder.adapterPosition)
    }
    
    override fun getItemCount(): Int = educationList.size
    
    class EducationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val institutionInput: TextInputEditText = itemView.findViewById(R.id.institutionInput)
        val degreeInput: TextInputEditText = itemView.findViewById(R.id.degreeInput)
        val startDateInput: TextInputEditText = itemView.findViewById(R.id.startDateInput)
        val endDateInput: TextInputEditText = itemView.findViewById(R.id.endDateInput)
    }
    
    data class Education(
        var institution: String = "",
        var degree: String = "",
        var startDate: String = "",
        var endDate: String = ""
    )
} 