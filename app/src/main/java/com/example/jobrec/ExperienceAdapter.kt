package com.example.jobrec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
class ExperienceAdapter : RecyclerView.Adapter<ExperienceAdapter.ViewHolder>() {
    private val experienceList = mutableListOf<Experience>()
    private val viewHolders = mutableMapOf<Int, ViewHolder>()
    init {
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
    fun getExperienceList(): List<Map<String, String>> {
        val updatedList = mutableListOf<Experience>()
        for (i in experienceList.indices) {
            val experience = experienceList[i].copy()
            val holder = viewHolders[i]
            if (holder != null) {
                experience.title = holder.titleInput.text.toString()
                experience.company = holder.companyInput.text.toString()
                experience.startDate = holder.startDateInput.text.toString()
                experience.endDate = holder.endDateInput.text.toString()
                experience.description = holder.descriptionInput.text.toString()
            }
            updatedList.add(experience)
        }
        return updatedList
            .filter { exp ->
                !(exp.title.isBlank() && exp.company.isBlank() &&
                  exp.startDate.isBlank() && exp.endDate.isBlank() &&
                  exp.description.isBlank())
            }
            .map { experience ->
                mapOf(
                    "title" to experience.title,
                    "company" to experience.company,
                    "startDate" to experience.startDate,
                    "endDate" to experience.endDate,
                    "description" to experience.description
                )
            }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_experience, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        viewHolders[position] = holder
        val experience = experienceList[position]
        holder.titleInput.setText(experience.title)
        holder.companyInput.setText(experience.company)
        holder.startDateInput.setText(experience.startDate)
        holder.endDateInput.setText(experience.endDate)
        holder.descriptionInput.setText(experience.description)
        holder.removeButton.setOnClickListener {
            if (experienceList.size > 1) {  
                val position = holder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    experienceList.removeAt(position)
                    notifyItemRemoved(position)
                    for (i in position until experienceList.size) {
                        notifyItemChanged(i)
                    }
                }
            }
        }
    }
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        val keyToRemove = viewHolders.entries.find { it.value == holder }?.key
        if (keyToRemove != null) {
            viewHolders.remove(keyToRemove)
        }
    }
    override fun getItemCount(): Int = experienceList.size
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleInput: TextInputEditText = view.findViewById(R.id.etExperienceTitle)
        val companyInput: TextInputEditText = view.findViewById(R.id.etExperienceCompany)
        val startDateInput: TextInputEditText = view.findViewById(R.id.etExperienceStartDate)
        val endDateInput: TextInputEditText = view.findViewById(R.id.etExperienceEndDate)
        val descriptionInput: TextInputEditText = view.findViewById(R.id.etExperienceDescription)
        val removeButton: Button = view.findViewById(R.id.btnRemoveExperience)
    }
    data class Experience(
        var title: String = "",
        var company: String = "",
        var startDate: String = "",
        var endDate: String = "",
        var description: String = ""
    )
}