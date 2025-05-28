package com.example.jobrec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.button.MaterialButton
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.example.jobrec.utils.ImageUtils
import java.util.Date
import java.util.concurrent.TimeUnit
class CandidateSearchAdapter(
    private val onCandidateClick: (User) -> Unit
) : ListAdapter<User, CandidateSearchAdapter.CandidateViewHolder>(CandidateDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_candidate_search, parent, false)
        return CandidateViewHolder(view, onCandidateClick)
    }
    override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    class CandidateViewHolder(
        itemView: View,
        private val onCandidateClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ShapeableImageView = itemView.findViewById(R.id.profileImage)
        private val nameText: TextView = itemView.findViewById(R.id.nameText)
        private val educationText: TextView = itemView.findViewById(R.id.educationText)
        private val locationText: TextView = itemView.findViewById(R.id.locationText)
        private val skillsText: TextView = itemView.findViewById(R.id.skillsText)
        private val experienceText: TextView = itemView.findViewById(R.id.experienceText)
        private val viewProfileButton: MaterialButton = itemView.findViewById(R.id.viewProfileButton)
        private fun calculateExperienceYears(experience: Experience): Long {
            val startDate = try {
                Date(experience.startDate.toLong())
            } catch (e: NumberFormatException) {
                Date()
            }
            val endDate = try {
                if (experience.endDate.isNotEmpty()) {
                    Date(experience.endDate.toLong())
                } else {
                    Date()
                }
            } catch (e: NumberFormatException) {
                Date()
            }
            val diffInMillis = endDate.time - startDate.time
            return TimeUnit.MILLISECONDS.toDays(diffInMillis) / 365
        }
        fun bind(candidate: User) {
            // Load profile image using ImageUtils (handles both URL and base64)
            ImageUtils.loadProfileImage(
                context = itemView.context,
                imageView = profileImage,
                user = candidate,
                isCircular = true
            )

            nameText.text = "${candidate.name} ${candidate.surname}"
            val highestEducation = candidate.education.maxByOrNull { it.degree }?.degree ?: "No education specified"
            educationText.text = highestEducation
            locationText.text = candidate.province
            val skillsFormatted = if (candidate.skills.isNotEmpty()) {
                "Skills: ${candidate.skills.joinToString(", ")}"
            } else {
                "No skills specified"
            }
            skillsText.text = skillsFormatted
            val experienceFormatted = if (candidate.experience.isNotEmpty()) {
                val totalExperience = candidate.experience.sumOf { exp ->
                    calculateExperienceYears(exp).toInt()
                }
                "Total Experience: $totalExperience years"
            } else {
                "No experience specified"
            }
            experienceText.text = experienceFormatted
            viewProfileButton.setOnClickListener {
                onCandidateClick(candidate)
            }
        }
    }
    private class CandidateDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.idNumber == newItem.idNumber
        }
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}