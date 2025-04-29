package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LanguageAdapter : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {
    private val languages = mutableListOf<Language>()
    private val proficiencyLevels = listOf("Basic", "Intermediate", "Advanced", "Native")

    fun addLanguage(language: Language) {
        languages.add(language)
        notifyItemInserted(languages.size - 1)
    }

    fun addNewLanguage() {
        addLanguage(Language())
    }

    fun removeLanguage(position: Int) {
        if (position in languages.indices) {
            languages.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getLanguages(): List<Language> = languages.toList()

    fun clearLanguages() {
        languages.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val languageNameInput: TextInputEditText = itemView.findViewById(R.id.languageNameInput)
        private val proficiencyDropdown: AutoCompleteTextView = itemView.findViewById(R.id.proficiencyDropdown)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.removeLanguageButton)

        init {
            // Set up proficiency dropdown
            val adapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_dropdown_item_1line,
                proficiencyLevels
            )
            proficiencyDropdown.setAdapter(adapter)

            // Set up remove button
            removeButton.setOnClickListener {
                removeLanguage(adapterPosition)
            }
        }

        fun bind(language: Language) {
            languageNameInput.setText(language.name)
            proficiencyDropdown.setText(language.proficiency, false)

            // Update language object when text changes
            languageNameInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    languages[adapterPosition] = language.copy(
                        name = languageNameInput.text.toString()
                    )
                }
            }

            proficiencyDropdown.setOnItemClickListener { _, _, position, _ ->
                languages[adapterPosition] = language.copy(
                    proficiency = proficiencyLevels[position]
                )
            }
        }
    }
} 