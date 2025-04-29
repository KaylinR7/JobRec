package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ReferenceAdapter : RecyclerView.Adapter<ReferenceAdapter.ReferenceViewHolder>() {
    
    private val referenceList = mutableListOf<Reference>()
    private val viewHolders = mutableMapOf<Int, ReferenceViewHolder>()
    
    init {
        // Add an empty reference item by default
        addNewReference()
    }
    
    fun addNewReference() {
        referenceList.add(Reference())
        notifyItemInserted(referenceList.size - 1)
    }
    
    fun clearReferenceList() {
        referenceList.clear()
        viewHolders.clear()
        notifyDataSetChanged()
    }
    
    fun addReference(reference: Reference) {
        referenceList.add(reference)
        notifyItemInserted(referenceList.size - 1)
    }
    
    fun getReferenceList(): List<Reference> {
        // Update the data from the views before returning
        for (i in referenceList.indices) {
            val holder = viewHolders[i]
            if (holder != null) {
                val reference = referenceList[i]
                reference.name = holder.nameInput.text.toString()
                reference.position = holder.positionInput.text.toString()
                reference.company = holder.companyInput.text.toString()
                reference.email = holder.emailInput.text.toString()
                reference.phone = holder.phoneInput.text.toString()
            }
        }
        return referenceList
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReferenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reference, parent, false)
        return ReferenceViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ReferenceViewHolder, position: Int) {
        viewHolders[position] = holder
        val reference = referenceList[position]
        holder.nameInput.setText(reference.name)
        holder.positionInput.setText(reference.position)
        holder.companyInput.setText(reference.company)
        holder.emailInput.setText(reference.email)
        holder.phoneInput.setText(reference.phone)
    }
    
    override fun onViewRecycled(holder: ReferenceViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder.adapterPosition)
    }
    
    override fun getItemCount(): Int = referenceList.size
    
    class ReferenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameInput: TextInputEditText = itemView.findViewById(R.id.referenceNameInput)
        val positionInput: TextInputEditText = itemView.findViewById(R.id.referencePositionInput)
        val companyInput: TextInputEditText = itemView.findViewById(R.id.referenceCompanyInput)
        val emailInput: TextInputEditText = itemView.findViewById(R.id.referenceEmailInput)
        val phoneInput: TextInputEditText = itemView.findViewById(R.id.referencePhoneInput)
    }
    
    data class Reference(
        var name: String = "",
        var position: String = "",
        var company: String = "",
        var email: String = "",
        var phone: String = ""
    )
} 