package com.example.jobrec.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.google.android.material.card.MaterialCardView

data class CertificateBadge(
    val name: String,
    val issuer: String,
    val year: String,
    val description: String = "",
    val category: String = ""
)

class CertificateBadgeAdapter(
    private val onBadgeClick: (CertificateBadge) -> Unit = {}
) : ListAdapter<CertificateBadge, CertificateBadgeAdapter.BadgeViewHolder>(BadgeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_certificate_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = getItem(position)
        holder.bind(badge)
    }

    inner class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val certificateIcon: ImageView = itemView.findViewById(R.id.certificateIcon)
        private val certificateName: TextView = itemView.findViewById(R.id.certificateName)
        private val certificateIssuer: TextView = itemView.findViewById(R.id.certificateIssuer)
        private val certificateYear: TextView = itemView.findViewById(R.id.certificateYear)

        fun bind(badge: CertificateBadge) {
            certificateName.text = badge.name
            certificateIssuer.text = badge.issuer
            certificateYear.text = badge.year

            // Set icon and colors based on certificate category
            val (iconRes, strokeColor) = getCertificateStyle(badge)
            certificateIcon.setImageResource(iconRes)
            cardView.strokeColor = ContextCompat.getColor(itemView.context, strokeColor)

            cardView.setOnClickListener {
                onBadgeClick(badge)
            }
        }

        private fun getCertificateStyle(badge: CertificateBadge): Pair<Int, Int> {
            return when {
                badge.name.contains("AWS", ignoreCase = true) -> 
                    Pair(R.drawable.ic_certificate, R.color.aws_orange)
                badge.name.contains("Azure", ignoreCase = true) || badge.name.contains("Microsoft", ignoreCase = true) -> 
                    Pair(R.drawable.ic_certificate, R.color.azure_blue)
                badge.name.contains("Google", ignoreCase = true) || badge.name.contains("GCP", ignoreCase = true) -> 
                    Pair(R.drawable.ic_certificate, R.color.google_blue)
                badge.name.contains("Oracle", ignoreCase = true) -> 
                    Pair(R.drawable.ic_certificate, R.color.oracle_red)
                badge.name.contains("Cisco", ignoreCase = true) -> 
                    Pair(R.drawable.ic_certificate, R.color.cisco_blue)
                badge.name.contains("CompTIA", ignoreCase = true) -> 
                    Pair(R.drawable.ic_certificate, R.color.comptia_red)
                badge.name.contains("PMP", ignoreCase = true) || badge.name.contains("Project", ignoreCase = true) -> 
                    Pair(R.drawable.ic_certificate, R.color.pmp_green)
                badge.name.contains("Scrum", ignoreCase = true) -> 
                    Pair(R.drawable.ic_certificate, R.color.scrum_blue)
                else -> 
                    Pair(R.drawable.ic_certificate, R.color.primary)
            }
        }
    }

    private class BadgeDiffCallback : DiffUtil.ItemCallback<CertificateBadge>() {
        override fun areItemsTheSame(oldItem: CertificateBadge, newItem: CertificateBadge): Boolean {
            return oldItem.name == newItem.name && oldItem.issuer == newItem.issuer && oldItem.year == newItem.year
        }

        override fun areContentsTheSame(oldItem: CertificateBadge, newItem: CertificateBadge): Boolean {
            return oldItem == newItem
        }
    }
}
