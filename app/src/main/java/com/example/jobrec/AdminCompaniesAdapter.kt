package com.example.jobrec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
class AdminCompaniesAdapter(
    private var companies: List<Company>,
    private val onViewClick: (Company) -> Unit,
    private val onEditClick: (Company) -> Unit,
    private val onDeleteClick: (Company) -> Unit
) : RecyclerView.Adapter<AdminCompaniesAdapter.CompanyViewHolder>() {
    inner class CompanyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.companyNameText)
        val emailText: TextView = itemView.findViewById(R.id.companyEmailText)
        val viewButton: MaterialButton = itemView.findViewById(R.id.viewCompanyButton)
        val editButton: MaterialButton = itemView.findViewById(R.id.editCompanyButton)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteCompanyButton)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_company, parent, false)
        return CompanyViewHolder(view)
    }
    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        val company = companies[position]
        holder.nameText.text = company.companyName
        holder.emailText.text = company.email
        holder.viewButton.setOnClickListener { onViewClick(company) }
        holder.editButton.setOnClickListener { onEditClick(company) }
        holder.deleteButton.setOnClickListener { onDeleteClick(company) }
    }
    override fun getItemCount(): Int = companies.size
    fun updateCompanies(newCompanies: List<Company>) {
        companies = newCompanies
        notifyDataSetChanged()
    }
    val companiesList: List<Company>
        get() = companies
}