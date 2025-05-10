package com.example.jobrec.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class CertificateAdapter : RecyclerView.Adapter<CertificateAdapter.CertificateViewHolder>() {
    private val certificates = mutableListOf<Certificate>()
    
    data class Certificate(
        var name: String = "",
        var issuer: String = "",
        var year: String = "",
        var description: String = ""
    )
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CertificateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_certificate, parent, false)
        return CertificateViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CertificateViewHolder, position: Int) {
        holder.bind(certificates[position])
    }
    
    override fun getItemCount(): Int = certificates.size
    
    fun addNewCertificate() {
        certificates.add(Certificate())
        notifyItemInserted(certificates.size - 1)
    }
    
    fun addCertificate(certificate: Certificate) {
        certificates.add(certificate)
        notifyItemInserted(certificates.size - 1)
    }
    
    fun getCertificatesList(): List<Map<String, String>> {
        return certificates.map { certificate ->
            mapOf(
                "name" to certificate.name,
                "issuer" to certificate.issuer,
                "year" to certificate.year,
                "description" to certificate.description
            )
        }
    }
    
    inner class CertificateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val certificateNameLayout: TextInputLayout = itemView.findViewById(R.id.certificateNameLayout)
        private val certificateName: AutoCompleteTextView = itemView.findViewById(R.id.certificateName)
        private val certificateIssuer: TextInputEditText = itemView.findViewById(R.id.certificateIssuer)
        private val certificateYear: TextInputEditText = itemView.findViewById(R.id.certificateYear)
        private val certificateDescription: TextInputEditText = itemView.findViewById(R.id.certificateDescription)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.btnRemoveCertificate)
        
        private val certificateOptions = arrayOf(
            "AWS Certified Solutions Architect",
            "AWS Certified Developer",
            "AWS Certified SysOps Administrator",
            "Microsoft Certified: Azure Fundamentals",
            "Microsoft Certified: Azure Administrator",
            "Microsoft Certified: Azure Developer",
            "Google Cloud Certified - Professional Cloud Architect",
            "Google Cloud Certified - Professional Data Engineer",
            "Certified Information Systems Security Professional (CISSP)",
            "Certified Ethical Hacker (CEH)",
            "CompTIA Security+",
            "CompTIA Network+",
            "CompTIA A+",
            "Cisco Certified Network Associate (CCNA)",
            "Cisco Certified Network Professional (CCNP)",
            "Project Management Professional (PMP)",
            "Certified ScrumMaster (CSM)",
            "Certified Scrum Product Owner (CSPO)",
            "Oracle Certified Professional, Java SE Programmer",
            "Oracle Certified Associate, Java SE Programmer",
            "Certified Kubernetes Administrator (CKA)",
            "Certified Kubernetes Application Developer (CKAD)",
            "Red Hat Certified System Administrator (RHCSA)",
            "Red Hat Certified Engineer (RHCE)",
            "Salesforce Certified Administrator",
            "Salesforce Certified Platform Developer",
            "Salesforce Certified Sales Cloud Consultant",
            "Salesforce Certified Service Cloud Consultant",
            "Certified Information Security Manager (CISM)",
            "Certified in Risk and Information Systems Control (CRISC)",
            "ITIL Foundation",
            "ITIL Practitioner",
            "ITIL Intermediate",
            "ITIL Expert",
            "ITIL Master",
            "Six Sigma Green Belt",
            "Six Sigma Black Belt",
            "Certified Data Professional (CDP)",
            "Certified Analytics Professional (CAP)",
            "Certified Business Analysis Professional (CBAP)",
            "Other"
        )
        
        fun bind(certificate: Certificate) {
            // Setup certificate name dropdown with search
            setupCertificateNameDropdown()
            
            // Set values
            certificateName.setText(certificate.name)
            certificateIssuer.setText(certificate.issuer)
            certificateYear.setText(certificate.year)
            certificateDescription.setText(certificate.description)
            
            // Setup text change listeners
            certificateName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    certificate.name = s.toString()
                }
            })
            
            certificateIssuer.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    certificate.issuer = s.toString()
                }
            })
            
            certificateYear.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    certificate.year = s.toString()
                }
            })
            
            certificateDescription.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    certificate.description = s.toString()
                }
            })
            
            // Setup remove button
            removeButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    certificates.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, certificates.size)
                }
            }
        }
        
        private fun setupCertificateNameDropdown() {
            val adapter = SearchableAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, certificateOptions)
            certificateName.setAdapter(adapter)
            
            // Show dropdown when focused
            certificateName.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    certificateName.showDropDown()
                }
            }
            
            // Show dropdown when clicked
            certificateName.setOnClickListener {
                certificateName.showDropDown()
            }
        }
    }
    
    // Custom adapter that allows searching in the dropdown
    class SearchableAdapter(
        context: Context,
        resource: Int,
        private val items: Array<String>
    ) : ArrayAdapter<String>(context, resource, items) {
        private val allItems: List<String> = items.toList()
        
        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val filterResults = FilterResults()
                    if (constraint.isNullOrEmpty()) {
                        filterResults.values = allItems
                        filterResults.count = allItems.size
                    } else {
                        val filteredList = allItems.filter {
                            it.lowercase(Locale.getDefault()).contains(constraint.toString().lowercase(Locale.getDefault()))
                        }
                        filterResults.values = filteredList
                        filterResults.count = filteredList.size
                    }
                    return filterResults
                }
                
                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    clear()
                    if (results != null && results.count > 0) {
                        addAll(results.values as List<String>)
                    } else {
                        addAll(allItems)
                    }
                    notifyDataSetChanged()
                }
            }
        }
    }
}
