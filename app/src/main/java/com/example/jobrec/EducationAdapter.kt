package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EducationAdapter : RecyclerView.Adapter<EducationAdapter.ViewHolder>() {

    private val educationList = mutableListOf<Education>()
    private val viewHolders = mutableMapOf<Int, ViewHolder>()

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

    fun getEducationList(): List<Map<String, String>> {
        // Update the data from the views before returning
        val updatedList = mutableListOf<Education>()

        // Copy the current list to avoid concurrent modification
        for (i in educationList.indices) {
            val education = educationList[i].copy()
            val holder = viewHolders[i]
            if (holder != null) {
                education.institution = holder.institutionInput.text.toString()
                education.degree = holder.degreeInput.text.toString()
                education.startDate = holder.startDateInput.text.toString()
                education.endDate = holder.endDateInput.text.toString()
                education.description = holder.descriptionInput.text.toString()
            }
            updatedList.add(education)
        }

        return updatedList.map { education ->
            mapOf(
                "institution" to education.institution,
                "degree" to education.degree,
                "startDate" to education.startDate,
                "endDate" to education.endDate,
                "description" to education.description
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_education, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        viewHolders[position] = holder
        val education = educationList[position]
        holder.institutionInput.setText(education.institution)
        holder.degreeInput.setText(education.degree)
        holder.startDateInput.setText(education.startDate)
        holder.endDateInput.setText(education.endDate)
        holder.descriptionInput.setText(education.description)

        // Set up remove button click listener
        holder.removeButton.setOnClickListener {
            if (educationList.size > 1) {  // Keep at least one education entry
                val position = holder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    educationList.removeAt(position)
                    notifyItemRemoved(position)
                    // Update positions of remaining items
                    for (i in position until educationList.size) {
                        notifyItemChanged(i)
                    }
                }
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Find the key to remove by value instead of using adapterPosition
        val keyToRemove = viewHolders.entries.find { it.value == holder }?.key
        if (keyToRemove != null) {
            viewHolders.remove(keyToRemove)
        }
    }

    override fun getItemCount(): Int = educationList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val institutionInput: TextInputEditText = view.findViewById(R.id.etEducationInstitution)
        val degreeInput: TextInputEditText = view.findViewById(R.id.etEducationDegree)
        val startDateInput: TextInputEditText = view.findViewById(R.id.etEducationStartDate)
        val endDateInput: TextInputEditText = view.findViewById(R.id.etEducationEndDate)
        val descriptionInput: TextInputEditText = view.findViewById(R.id.etEducationDescription)
        val removeButton: Button = view.findViewById(R.id.btnRemoveEducation)
    }

    data class Education(
        var institution: String = "",
        var degree: String = "",
        var startDate: String = "",
        var endDate: String = "",
        var description: String = ""
    )
}