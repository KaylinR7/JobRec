package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class ExperienceAdapter : RecyclerView.Adapter<ExperienceAdapter.ExperienceViewHolder>() {
    
    private val experienceList = mutableListOf<Experience>()
    private val viewHolders = mutableMapOf<Int, ExperienceViewHolder>()
    
    init {
        // Add an empty experience item by default
        addNewExperience()
    }
    
    fun addNewExperience() {
        experienceList.add(Experience())
        notifyItemInserted(experienceList.size - 1)
    }
    
    fun clearExperienceList() {
        experienceList.clear()
        viewHolders.clear()
        notifyDataSetChanged()
    }
    
    fun addExperience(experience: Experience) {
        experienceList.add(experience)
        notifyItemInserted(experienceList.size - 1)
    }
    
    fun getExperienceList(): List<Experience> {
        // Update the data from the views before returning
        for (i in experienceList.indices) {
            val holder = viewHolders[i]
            if (holder != null) {
                val experience = experienceList[i]
                experience.company = holder.companyInput.text.toString()
                experience.position = holder.positionInput.text.toString()
                experience.startDate = holder.startDateInput.text.toString()
                experience.endDate = holder.endDateInput.text.toString()
                experience.description = holder.descriptionInput.text.toString()
            }
        }
        return experienceList
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExperienceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_experience, parent, false)
        return ExperienceViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ExperienceViewHolder, position: Int) {
        viewHolders[position] = holder
        val experience = experienceList[position]
        holder.companyInput.setText(experience.company)
        holder.positionInput.setText(experience.position)
        holder.startDateInput.setText(experience.startDate)
        holder.endDateInput.setText(experience.endDate)
        holder.descriptionInput.setText(experience.description)
    }
    
    override fun onViewRecycled(holder: ExperienceViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder.adapterPosition)
    }
    
    override fun getItemCount(): Int = experienceList.size
    
    class ExperienceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val companyInput: TextInputEditText = itemView.findViewById(R.id.companyInput)
        val positionInput: TextInputEditText = itemView.findViewById(R.id.positionInput)
        val startDateInput: TextInputEditText = itemView.findViewById(R.id.startDateInput)
        val endDateInput: TextInputEditText = itemView.findViewById(R.id.endDateInput)
        val descriptionInput: TextInputEditText = itemView.findViewById(R.id.descriptionInput)
    }
    
    data class Experience(
        var company: String = "",
        var position: String = "",
        var startDate: String = "",
        var endDate: String = "",
        var description: String = ""
    )
} 