package com.example.jobrec
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
class CompanyApplicationDetailsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var jobTitleText: TextView
    private lateinit var applicantNameText: TextView
    private lateinit var applicantEmailText: TextView
    private lateinit var applicantPhoneText: TextView
    private lateinit var statusText: TextView
    private lateinit var appliedDateText: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var viewResumeButton: Button
    private lateinit var chatButton: Button
    private lateinit var reviewCvButton: Button
    private var applicationId: String? = null
    private var resumeUrl: String? = null
    private var applicantId: String? = null
    private var jobId: String? = null
    private var jobTitle: String? = null
    private var companyId: String? = null
    private var companyName: String? = null
    private var applicantName: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_application_details)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Application Review"
        }
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
        applicationId = intent.getStringExtra("applicationId")
        jobTitleText = findViewById(R.id.jobTitleText)
        applicantNameText = findViewById(R.id.applicantNameText)
        applicantEmailText = findViewById(R.id.applicantEmailText)
        applicantPhoneText = findViewById(R.id.applicantPhoneText)
        statusText = findViewById(R.id.statusText)
        appliedDateText = findViewById(R.id.appliedDateText)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)
        viewResumeButton = findViewById(R.id.viewResumeButton)
        chatButton = findViewById(R.id.chatButton)
        reviewCvButton = findViewById(R.id.reviewCvButton)
        db = FirebaseFirestore.getInstance()
        loadApplicationDetails()
        setupButtons()
    }
    private fun setupButtons() {
        acceptButton.setOnClickListener {
            updateApplicationStatus("accepted")
        }
        rejectButton.setOnClickListener {
            updateApplicationStatus("rejected")
        }
        viewResumeButton.setOnClickListener {
            android.util.Log.d("CompanyApplicationDetails", "View resume button clicked. " +
                    "applicantId=$applicantId, resumeUrl=$resumeUrl")
            if (!applicantId.isNullOrEmpty()) {
                android.util.Log.d("CompanyApplicationDetails", "Showing applicant profile")
                showApplicantProfile(applicantId!!)
            } else if (!resumeUrl.isNullOrEmpty()) {
                android.util.Log.d("CompanyApplicationDetails", "Resume URL found: $resumeUrl")
                try {
                    if (resumeUrl!!.startsWith("http")) {
                        android.util.Log.d("CompanyApplicationDetails", "Opening resume as URL")
                        openResume(resumeUrl!!)
                    } else {
                        android.util.Log.d("CompanyApplicationDetails", "Loading CV content from Firestore")
                        loadCvContent(resumeUrl!!)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CompanyApplicationDetails", "Error processing resume URL: ${e.message}", e)
                    Toast.makeText(this, "Error loading resume: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                android.util.Log.d("CompanyApplicationDetails", "No resume available")
                Toast.makeText(this, "No resume available", Toast.LENGTH_SHORT).show()
            }
        }
        reviewCvButton.setOnClickListener {
            android.util.Log.d("CompanyApplicationDetails", "Review CV button clicked. " +
                    "applicantId=$applicantId, resumeUrl=$resumeUrl")
            updateApplicationStatus("reviewed")
            if (!applicantId.isNullOrEmpty()) {
                android.util.Log.d("CompanyApplicationDetails", "Showing applicant profile for review")
                showApplicantProfile(applicantId!!)
            } else if (!resumeUrl.isNullOrEmpty()) {
                android.util.Log.d("CompanyApplicationDetails", "Resume URL found for review: $resumeUrl")
                try {
                    if (resumeUrl!!.startsWith("http")) {
                        android.util.Log.d("CompanyApplicationDetails", "Opening resume as URL for review")
                        openResume(resumeUrl!!)
                    } else {
                        android.util.Log.d("CompanyApplicationDetails", "Loading CV content from Firestore for review")
                        loadCvContent(resumeUrl!!)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CompanyApplicationDetails", "Error processing resume URL for review: ${e.message}", e)
                    Toast.makeText(this, "Error loading resume for review: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                android.util.Log.d("CompanyApplicationDetails", "No CV available to review")
                Toast.makeText(this, "No CV available to review", Toast.LENGTH_SHORT).show()
            }
        }
        chatButton.setOnClickListener {
            if (applicationId != null && applicantId != null) {
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("applicationId", applicationId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot start messaging: Missing application information", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadCvContent(cvId: String) {
        if (!applicantId.isNullOrEmpty()) {
            showApplicantProfile(applicantId!!)
            return
        }
        if (cvId.startsWith("http")) {
            openResume(cvId)
            return
        }
        android.util.Log.d("CompanyApplicationDetails", "Loading CV with ID: $cvId")
        db.collection("cvs")
            .document(cvId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val cvContent = document.getString("content")
                    if (!cvContent.isNullOrEmpty()) {
                        showCvContent(cvContent)
                    } else {
                        android.util.Log.e("CompanyApplicationDetails", "CV document exists but has no content")
                        Toast.makeText(this, "No resume content available", Toast.LENGTH_SHORT).show()
                        if (!applicantId.isNullOrEmpty()) {
                            showApplicantProfile(applicantId!!)
                        }
                    }
                } else {
                    android.util.Log.e("CompanyApplicationDetails", "CV document does not exist: $cvId")
                    Toast.makeText(this, "Resume not found", Toast.LENGTH_SHORT).show()
                    if (!applicantId.isNullOrEmpty()) {
                        showApplicantProfile(applicantId!!)
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CompanyApplicationDetails", "Error loading CV: ${e.message}", e)
                Toast.makeText(this, "Error loading resume: ${e.message}", Toast.LENGTH_SHORT).show()
                if (!applicantId.isNullOrEmpty()) {
                    showApplicantProfile(applicantId!!)
                }
            }
    }
    private fun showCvContent(content: String) {
        val intent = Intent(this, ViewCvActivity::class.java).apply {
            putExtra("cvContent", content)
        }
        startActivity(intent)
    }
    private fun showApplicantProfile(userId: String) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val user = document.toObject(User::class.java)?.copy(id = document.id)
                        if (user != null) {
                            showCandidateProfileDialog(user)
                        } else {
                            Toast.makeText(this, "Error loading user profile", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error parsing user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading user profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showCandidateProfileDialog(candidate: User) {
        val view = createCandidateProfileView(candidate)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("${candidate.name} ${candidate.surname}")
            .setView(view)
            .setPositiveButton("Contact") { dialog, which ->
                contactCandidate(candidate)
            }
            .setNegativeButton("Close", null)
            .create()
        dialog.show()
    }
    private fun createCandidateProfileView(candidate: User): View {
        val view = layoutInflater.inflate(R.layout.dialog_candidate_profile, null)
        val nameText = view.findViewById<TextView>(R.id.nameText)
        nameText.text = "${candidate.name} ${candidate.surname}"
        val emailText = view.findViewById<TextView>(R.id.emailText)
        emailText.text = candidate.email
        val phoneText = view.findViewById<TextView>(R.id.phoneText)
        phoneText.text = candidate.phoneNumber
        val locationText = view.findViewById<TextView>(R.id.locationText)
        locationText.text = candidate.address
        val summaryText = view.findViewById<TextView>(R.id.summaryText)
        summaryText.text = candidate.summary
        val skillsText = view.findViewById<TextView>(R.id.skillsText)
        if (candidate.skills.isNotEmpty()) {
            skillsText.text = candidate.skills.joinToString(", ")
        } else {
            skillsText.text = "No skills specified"
        }
        val educationText = view.findViewById<TextView>(R.id.educationText)
        if (candidate.education.isNotEmpty()) {
            val educationList = candidate.education.joinToString("\n") { edu ->
                "${edu.degree} at ${edu.institution} (${edu.fieldOfStudy})"
            }
            educationText.text = educationList
        } else {
            educationText.text = "No education specified"
        }
        val experienceText = view.findViewById<TextView>(R.id.experienceText)
        if (candidate.experience.isNotEmpty()) {
            val experienceList = candidate.experience.joinToString("\n\n") { exp ->
                "${exp.position} at ${exp.company}\n" +
                "${exp.startDate} - ${exp.endDate}\n" +
                exp.description
            }
            experienceText.text = experienceList
        } else {
            experienceText.text = "No experience specified"
        }
        val fieldText = view.findViewById<TextView>(R.id.fieldText)
        if (candidate.field.isNotEmpty()) {
            val fieldInfo = if (candidate.subField.isNotEmpty()) {
                "${candidate.field} - ${candidate.subField}"
            } else {
                candidate.field
            }
            fieldText.text = fieldInfo
        } else {
            fieldText.text = "Not specified"
        }
        val yearsOfExperienceText = view.findViewById<TextView>(R.id.yearsOfExperienceText)
        yearsOfExperienceText.text = if (candidate.yearsOfExperience.isNotEmpty()) {
            candidate.yearsOfExperience
        } else {
            "Not specified"
        }
        val certificateText = view.findViewById<TextView>(R.id.certificateText)
        certificateText.text = if (candidate.certificate.isNotEmpty()) {
            candidate.certificate
        } else {
            "Not specified"
        }
        val expectedSalaryText = view.findViewById<TextView>(R.id.expectedSalaryText)
        expectedSalaryText.text = if (candidate.expectedSalary.isNotEmpty()) {
            candidate.expectedSalary
        } else {
            "Not specified"
        }
        val socialLinksText = view.findViewById<TextView>(R.id.socialLinksText)
        val socialLinks = mutableListOf<String>()
        if (candidate.linkedin.isNotEmpty()) {
            socialLinks.add("LinkedIn: ${candidate.linkedin}")
        }
        if (candidate.github.isNotEmpty()) {
            socialLinks.add("GitHub: ${candidate.github}")
        }
        if (candidate.portfolio.isNotEmpty()) {
            socialLinks.add("Portfolio: ${candidate.portfolio}")
        }
        socialLinksText.text = if (socialLinks.isNotEmpty()) {
            socialLinks.joinToString("\n")
        } else {
            "No social links provided"
        }
        return view
    }
    private fun contactCandidate(candidate: User) {
        val options = arrayOf("Email", "Phone")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Contact ${candidate.name}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { 
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${candidate.email}")
                            putExtra(Intent.EXTRA_SUBJECT, "Job Application: $jobTitle")
                        }
                        startActivity(intent)
                    }
                    1 -> { 
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${candidate.phoneNumber}")
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }
    private fun openResume(url: String) {
        if (!applicantId.isNullOrEmpty()) {
            android.util.Log.d("CompanyApplicationDetails", "Using applicant profile instead of resume URL")
            showApplicantProfile(applicantId!!)
            return
        }
        android.util.Log.d("CompanyApplicationDetails", "Opening resume URL: $url")
        if (!url.startsWith("http")) {
            android.util.Log.e("CompanyApplicationDetails", "Invalid resume URL format: $url")
            Toast.makeText(this, "Invalid resume URL format", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = Uri.parse(url)
            android.util.Log.d("CompanyApplicationDetails", "Opening PDF with URI: $uri")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                android.util.Log.e("CompanyApplicationDetails", "No PDF viewer app found")
                Toast.makeText(this, "No PDF viewer app found. Please install a PDF viewer.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("CompanyApplicationDetails", "Error opening PDF: ${e.message}", e)
            Toast.makeText(this, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } catch (e2: Exception) {
                android.util.Log.e("CompanyApplicationDetails", "Error opening in browser: ${e2.message}", e2)
                Toast.makeText(this, "Could not open the resume. Please check if the URL is valid.", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun loadApplicationDetails() {
        applicationId?.let { id ->
            android.util.Log.d("CompanyApplicationDetails", "Loading application with ID: $id")
            db.collection("applications").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        jobTitle = document.getString("jobTitle")
                        jobTitleText.text = jobTitle
                        applicantName = document.getString("applicantName")
                        applicantNameText.text = applicantName
                        applicantEmailText.text = document.getString("applicantEmail") ?: "Not provided"
                        applicantPhoneText.text = document.getString("applicantPhone") ?: "Not provided"
                        val status = document.getString("status") ?: "pending"
                        statusText.text = status.capitalize()
                        statusText.setTextColor(getStatusColor(status))
                        resumeUrl = document.getString("resumeUrl")
                        applicantId = document.getString("userId")
                        jobId = document.getString("jobId")
                        companyId = document.getString("companyId")
                        companyName = document.getString("companyName")
                        android.util.Log.d("CompanyApplicationDetails", "Application data: " +
                                "resumeUrl=$resumeUrl, applicantId=$applicantId, " +
                                "jobId=$jobId, companyId=$companyId")
                        val timestamp = document.getTimestamp("timestamp")?.toDate()
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        appliedDateText.text = timestamp?.let { dateFormat.format(it) }
                        val isActionable = status == "pending" || status == "reviewed"
                        acceptButton.isEnabled = isActionable
                        rejectButton.isEnabled = isActionable
                        chatButton.visibility = android.view.View.VISIBLE
                        viewResumeButton.isEnabled = !resumeUrl.isNullOrEmpty() || !applicantId.isNullOrEmpty()
                        if (resumeUrl.isNullOrEmpty() && !applicantId.isNullOrEmpty()) {
                            android.util.Log.d("CompanyApplicationDetails", "No resume URL, will use profile data")
                        }
                    } else {
                        android.util.Log.e("CompanyApplicationDetails", "Application document does not exist: $id")
                        Toast.makeText(this, "Application not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CompanyApplicationDetails", "Error loading application: ${e.message}", e)
                    Toast.makeText(this, "Error loading application: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        } ?: run {
            android.util.Log.e("CompanyApplicationDetails", "No application ID provided")
            Toast.makeText(this, "No application ID provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    private fun updateApplicationStatus(newStatus: String) {
        applicationId?.let { id ->
            db.collection("applications").document(id)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "lastUpdated" to com.google.firebase.Timestamp.now()
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Application $newStatus", Toast.LENGTH_SHORT).show()
                    loadApplicationDetails() 
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating application: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun getStatusColor(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> resources.getColor(R.color.status_pending, theme)
            "accepted" -> resources.getColor(R.color.status_accepted, theme)
            "rejected" -> resources.getColor(R.color.status_rejected, theme)
            else -> resources.getColor(R.color.status_pending, theme)
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}