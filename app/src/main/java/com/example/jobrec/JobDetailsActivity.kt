package com.example.jobrec

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import java.util.Date
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class JobDetailsActivity : AppCompatActivity() {
    private lateinit var jobTitle: TextView
    private lateinit var companyName: TextView
    private lateinit var jobLocation: TextView
    private lateinit var jobType: TextView
    private lateinit var jobSalary: TextView
    private lateinit var jobDescription: TextView
    private lateinit var jobRequirements: TextView
    private lateinit var applyButton: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var jobId: String? = null
    private var companyId: String? = null
    private var currentCvUrl: String? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadCv(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_detail)

        // Set up toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Job Details"
        }

        // Initialize views
        jobTitle = findViewById(R.id.jobTitle)
        companyName = findViewById(R.id.companyName)
        jobLocation = findViewById(R.id.jobLocation)
        jobType = findViewById(R.id.jobType)
        jobSalary = findViewById(R.id.jobSalary)
        jobDescription = findViewById(R.id.jobDescription)
        jobRequirements = findViewById(R.id.jobRequirements)
        applyButton = findViewById(R.id.applyButton)

        // Set up back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Get job ID from intent
        jobId = intent.getStringExtra("jobId")
        if (jobId != null) {
            loadJobDetails(jobId!!)
        } else {
            Toast.makeText(this, "Error: Job not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup apply button
        applyButton.setOnClickListener {
            if (auth.currentUser != null) {
                checkIfAlreadyApplied()
            } else {
                Toast.makeText(this, "Please sign in to apply", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to login screen
            }
        }
    }

    private fun checkIfAlreadyApplied() {
        val userId = auth.currentUser?.uid ?: return
        val jobId = jobId ?: return

        db.collection("applications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("jobId", jobId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showApplicationDialog()
                } else {
                    Toast.makeText(this, "You have already applied for this job", Toast.LENGTH_SHORT).show()
                    applyButton.isEnabled = false
                    applyButton.text = "Already Applied"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking application status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showApplicationDialog() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_apply, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetView.findViewById<MaterialCardView>(R.id.profileCard).setOnClickListener {
            bottomSheetDialog.dismiss()
            useProfileAsCv()
        }

        bottomSheetView.findViewById<MaterialCardView>(R.id.uploadCard).setOnClickListener {
            bottomSheetDialog.dismiss()
            selectCv()
        }

        bottomSheetDialog.show()
    }

    private fun useProfileAsCv() {
        val userId = auth.currentUser?.uid ?: return
        // First get the user's ID number from the Users collection using their email
        db.collection("users")
            .whereEqualTo("email", auth.currentUser?.email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    val user = userDoc.toObject(User::class.java)
                    user?.let {
                        // Create a CV-like document from the user's profile
                        val cvContent = buildString {
                            append("${it.name} ${it.surname}\n")
                            append("${it.email}\n")
                            append("${it.phoneNumber}\n")
                            append("${it.address}\n\n")
                            
                            append("SUMMARY\n")
                            append("${it.summary}\n\n")
                            
                            append("SKILLS\n")
                            append(it.skills.joinToString(", ").plus("\n\n"))
                            
                            append("EDUCATION\n")
                            it.education.forEach { education ->
                                append("${education.institution} - ${education.degree}\n")
                                append("${education.startDate} to ${education.endDate}\n\n")
                            }
                            
                            append("EXPERIENCE\n")
                            it.experience.forEach { experience ->
                                append("${experience.position} at ${experience.company}\n")
                                append("${experience.startDate} to ${experience.endDate}\n")
                                append("${experience.description}\n\n")
                            }
                        }
                        
                        // Store the CV content in Firestore
                        val cvRef = db.collection("cvs").document()
                        cvRef.set(hashMapOf(
                            "userId" to userId,
                            "content" to cvContent,
                            "createdAt" to System.currentTimeMillis()
                        )).addOnSuccessListener {
                            currentCvUrl = cvRef.id
                            showCoverLetterDialog()
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Error creating CV from profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            applyButton.isEnabled = true
                            applyButton.text = "Apply Now"
                        }
                    }
                } else {
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                    applyButton.isEnabled = true
                    applyButton.text = "Apply Now"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                applyButton.isEnabled = true
                applyButton.text = "Apply Now"
            }
    }

    private fun selectCv() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf"))
        }
        getContent.launch(intent)
    }

    private fun uploadCv(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val cvFileName = "CV_${userId}_${UUID.randomUUID()}.pdf"
        val cvRef = storage.reference.child("cvs/$cvFileName")

        applyButton.isEnabled = false
        applyButton.text = "Uploading CV..."

        cvRef.putFile(uri)
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                applyButton.text = "Uploading CV... ${progress.toInt()}%"
            }
            .addOnSuccessListener {
                cvRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    currentCvUrl = downloadUri.toString()
                    showCoverLetterDialog()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error uploading CV: ${e.message}", Toast.LENGTH_SHORT).show()
                applyButton.isEnabled = true
                applyButton.text = "Apply Now"
            }
    }

    private fun showCoverLetterDialog() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_cover_letter, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        val coverLetterText = bottomSheetView.findViewById<TextInputEditText>(R.id.coverLetterText)
        val submitButton = bottomSheetView.findViewById<MaterialButton>(R.id.submitButton)

        submitButton.setOnClickListener {
            val coverLetter = coverLetterText.text?.toString() ?: ""
            if (coverLetter.isNotEmpty()) {
                bottomSheetDialog.dismiss()
                submitApplication(coverLetter)
            } else {
                coverLetterText.error = "Please provide a cover letter"
            }
        }

        bottomSheetDialog.show()
    }

    private fun submitApplication(coverLetter: String) {
        val userId = auth.currentUser?.uid ?: return
        val jobId = jobId ?: return
        val companyId = companyId ?: return
        val cvUrl = currentCvUrl ?: return

        // First get the user's name
        db.collection("users")
            .whereEqualTo("email", auth.currentUser?.email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    val user = userDoc.toObject(User::class.java)
                    val applicantName = "${user?.name} ${user?.surname}"

                    val application = JobApplication(
                        id = "",
                        jobId = jobId,
                        jobTitle = jobTitle.text.toString(),
                        userId = userId,
                        applicantName = applicantName,
                        companyId = companyId,
                        appliedDate = Date(),
                        status = ApplicationStatus.PENDING.name,
                        cvUrl = cvUrl,
                        coverLetter = coverLetter
                    )

                    db.collection("applications")
                        .add(application)
                        .addOnSuccessListener { documentReference ->
                            // Create notification for company
                            val notification = hashMapOf(
                                "type" to "new_application",
                                "applicationId" to documentReference.id,
                                "jobId" to jobId,
                                "userId" to userId,
                                "companyId" to companyId,
                                "timestamp" to System.currentTimeMillis(),
                                "read" to false
                            )

                            db.collection("notifications")
                                .add(notification)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_SHORT).show()
                                    applyButton.isEnabled = false
                                    applyButton.text = "Applied"
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error submitting application: ${e.message}", Toast.LENGTH_SHORT).show()
                            applyButton.isEnabled = true
                            applyButton.text = "Apply Now"
                        }
                } else {
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    applyButton.isEnabled = true
                    applyButton.text = "Apply Now"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading user profile: ${e.message}", Toast.LENGTH_SHORT).show()
                applyButton.isEnabled = true
                applyButton.text = "Apply Now"
            }
    }

    private fun loadJobDetails(jobId: String) {
        Log.d("JobDetailsActivity", "Loading job details for ID: $jobId")
        if (jobId.isBlank()) {
            Log.e("JobDetailsActivity", "Invalid job ID: $jobId")
            Toast.makeText(this, "Invalid job ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("jobs")
            .document(jobId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val job = document.toObject(Job::class.java)
                    job?.let { 
                        Log.d("JobDetailsActivity", "Successfully loaded job: ${it.title}")
                        displayJobDetails(it)
                        companyId = it.companyId
                    }
                } else {
                    Log.e("JobDetailsActivity", "No job found with ID: $jobId")
                    Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("JobDetailsActivity", "Error loading job details", e)
                Toast.makeText(this, "Error loading job details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayJobDetails(job: Job) {
        jobTitle.text = job.title
        companyName.text = job.companyName
        jobLocation.text = job.location
        jobType.text = job.type
        jobSalary.text = job.salary
        jobDescription.text = job.description
        jobRequirements.text = job.getRequirementsList().joinToString("\n• ", "• ")

        // Check if current user is a company
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("companies")
                .whereEqualTo("email", currentUser.email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // Hide apply button for company users
                        applyButton.visibility = View.GONE
                    } else {
                        // Show apply button for regular users
                        applyButton.visibility = View.VISIBLE
                        applyButton.setOnClickListener {
                            if (auth.currentUser != null) {
                                checkIfAlreadyApplied()
                            } else {
                                Toast.makeText(this, "Please sign in to apply", Toast.LENGTH_SHORT).show()
                                // TODO: Navigate to login screen
                            }
                        }
                    }
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 