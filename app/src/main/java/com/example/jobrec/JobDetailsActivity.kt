package com.example.jobrec
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import java.util.Date
import android.util.Base64
import java.io.InputStream
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.example.jobrec.databinding.ActivityJobDetailsBinding
class JobDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJobDetailsBinding
    private lateinit var jobTitle: TextView
    private lateinit var companyName: TextView
    private lateinit var jobLocation: TextView
    private lateinit var jobType: TextView
    private lateinit var jobSalary: TextView
    private lateinit var jobDescription: TextView
    private lateinit var jobRequirements: TextView
    private lateinit var jobField: TextView
    private lateinit var jobProvince: TextView
    private lateinit var jobExperience: TextView
    private lateinit var jobPostedDate: TextView
    private lateinit var applyButton: MaterialButton
    private lateinit var saveJobButton: MaterialButton
    private var isJobSaved = false
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance("gs://careerworx-f5bc6.firebasestorage.app")
        } catch (e: Exception) {
            Log.e("JobDetailsActivity", "Error initializing Firebase Storage with custom bucket, using default", e)
            FirebaseStorage.getInstance()
        }
    }
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
        binding = ActivityJobDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Job Details"
        }
        binding.toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
        initializeViews()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        jobId = intent.getStringExtra("jobId")
        if (jobId != null) {
            loadJobDetails(jobId!!)
        } else {
            Toast.makeText(this, "Error: Job not found", Toast.LENGTH_SHORT).show()
            finish()
        }
        applyButton.setOnClickListener {
            if (auth.currentUser != null) {
                checkIfAlreadyApplied()
            } else {
                Toast.makeText(this, "Please sign in to apply", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun initializeViews() {
        jobTitle = binding.jobTitle
        companyName = binding.companyName
        jobLocation = binding.jobLocation
        jobType = binding.jobType
        jobSalary = binding.jobSalary
        jobDescription = binding.jobDescription
        jobRequirements = binding.jobRequirements
        jobField = binding.jobField
        jobProvince = binding.jobProvince
        jobExperience = binding.jobExperience
        jobPostedDate = binding.jobPostedDate
        applyButton = binding.applyButton
        saveJobButton = binding.saveJobButton
        saveJobButton.setOnClickListener {
            toggleSaveJob()
        }
    }
    private fun checkIfAlreadyApplied(showDialog: Boolean = true) {
        val userId = auth.currentUser?.uid ?: return
        val jobId = jobId ?: return
        db.collection("applications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("jobId", jobId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    if (showDialog) {
                        showApplicationDialog()
                    }
                } else {
                    if (showDialog) {
                        Toast.makeText(this, "You have already applied for this job", Toast.LENGTH_SHORT).show()
                    }
                    applyButton.isEnabled = false
                    applyButton.text = "Already Applied"
                }
            }
            .addOnFailureListener { e ->
                Log.e("JobDetailsActivity", "Error checking application status", e)
                if (showDialog) {
                    Toast.makeText(this, "Error checking application status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                applyButton.isEnabled = true
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
        val userEmail = auth.currentUser?.email ?: return
        Log.d("JobDetailsActivity", "Creating CV from profile for user: $userEmail (ID: $userId)")
        db.collection("users")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    try {
                        val user = userDoc.toObject(User::class.java)
                        if (user != null) {
                            Log.d("JobDetailsActivity", "Successfully retrieved user profile for CV creation")
                            val cvContent = buildString {
                                append("${user.name} ${user.surname}\n")
                                append("${user.email}\n")
                                append("${user.phoneNumber}\n")
                                append("${user.city}, ${user.province}\n\n")
                                append("SUMMARY\n")
                                append("${user.summary}\n\n")
                                append("SKILLS\n")
                                append(user.skills.joinToString(", ").plus("\n\n"))
                                append("EDUCATION\n")
                                if (user.education.isNotEmpty()) {
                                    user.education.forEach { education ->
                                        append("${education.institution} - ${education.degree}\n")
                                        append("${education.startDate} to ${education.endDate}\n\n")
                                    }
                                } else {
                                    append("No education information provided\n\n")
                                }
                                append("EXPERIENCE\n")
                                if (user.experience.isNotEmpty()) {
                                    user.experience.forEach { experience ->
                                        append("${experience.position} at ${experience.company}\n")
                                        append("${experience.startDate} to ${experience.endDate}\n")
                                        append("${experience.description}\n\n")
                                    }
                                } else {
                                    append("No experience information provided\n\n")
                                }
                            }
                            val cvRef = db.collection("cvs").document()
                            Log.d("JobDetailsActivity", "Creating CV document with ID: ${cvRef.id}")
                            cvRef.set(hashMapOf(
                                "userId" to userId,
                                "content" to cvContent,
                                "createdAt" to System.currentTimeMillis()
                            )).addOnSuccessListener {
                                Log.d("JobDetailsActivity", "Successfully created CV document: ${cvRef.id}")
                                currentCvUrl = cvRef.id
                                showCoverLetterDialog()
                            }.addOnFailureListener { e ->
                                Log.e("JobDetailsActivity", "Error creating CV from profile", e)
                                Toast.makeText(this, "Error creating CV from profile: ${e.message}", Toast.LENGTH_SHORT).show()
                                applyButton.isEnabled = true
                                applyButton.text = "Apply Now"
                            }
                        } else {
                            Log.e("JobDetailsActivity", "User object is null after conversion")
                            Toast.makeText(this, "Error creating profile: Invalid user data", Toast.LENGTH_SHORT).show()
                            applyButton.isEnabled = true
                            applyButton.text = "Apply Now"
                        }
                    } catch (e: Exception) {
                        Log.e("JobDetailsActivity", "Error converting user document to User object", e)
                        Toast.makeText(this, "Error processing profile data: ${e.message}", Toast.LENGTH_SHORT).show()
                        applyButton.isEnabled = true
                        applyButton.text = "Apply Now"
                    }
                } else {
                    Log.e("JobDetailsActivity", "No user document found for email: $userEmail")
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                    applyButton.isEnabled = true
                    applyButton.text = "Apply Now"
                }
            }
            .addOnFailureListener { e ->
                Log.e("JobDetailsActivity", "Error fetching user profile", e)
                Toast.makeText(this, "Error fetching profile: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("JobDetailsActivity", "Cannot upload CV: User ID is null")
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("JobDetailsActivity", "Starting CV upload for user: $userId")
        try {
            val cvFileName = "CV_${userId}_${UUID.randomUUID()}.pdf"
            val cvRef = storage.reference.child("cvs/$cvFileName")
            applyButton.isEnabled = false
            applyButton.text = "Uploading CV..."
            Log.d("JobDetailsActivity", "Uploading CV from URI: $uri")
            cvRef.putFile(uri)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    Log.d("JobDetailsActivity", "Upload progress: ${progress.toInt()}%")
                    applyButton.text = "Uploading CV... ${progress.toInt()}%"
                }
                .addOnSuccessListener {
                    Log.d("JobDetailsActivity", "CV file uploaded successfully, getting download URL")
                    cvRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            Log.d("JobDetailsActivity", "Got download URL: $downloadUri")
                            currentCvUrl = downloadUri.toString()
                            if (currentCvUrl.isNullOrEmpty() || !currentCvUrl!!.startsWith("http")) {
                                Log.e("JobDetailsActivity", "Invalid download URL: $currentCvUrl")
                                Toast.makeText(this, "Error: Could not get valid CV URL", Toast.LENGTH_SHORT).show()
                                applyButton.isEnabled = true
                                applyButton.text = "Apply Now"
                                return@addOnSuccessListener
                            }
                            showCoverLetterDialog()
                        }
                        .addOnFailureListener { e ->
                            Log.e("JobDetailsActivity", "Failed to get download URL", e)
                            Toast.makeText(this, "Error getting CV download link: ${e.message}", Toast.LENGTH_SHORT).show()
                            applyButton.isEnabled = true
                            applyButton.text = "Apply Now"
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("JobDetailsActivity", "Failed to upload CV file", e)
                    when {
                        e.message?.contains("Object does not exist") == true ||
                        e.message?.contains("404") == true ||
                        e.message?.contains("bucket") == true -> {
                            Log.e("JobDetailsActivity", "Storage bucket not available - using Firestore fallback")
                            uploadCvToFirestore(uri)
                        }
                        e.message?.contains("403") == true -> {
                            Log.e("JobDetailsActivity", "Permission denied - check storage rules")
                            Toast.makeText(this, "Permission denied. Please check storage permissions.", Toast.LENGTH_LONG).show()
                            applyButton.isEnabled = true
                            applyButton.text = "Apply Now"
                        }
                        else -> {
                            Log.e("JobDetailsActivity", "Storage upload failed, trying Firestore fallback")
                            uploadCvToFirestore(uri)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("JobDetailsActivity", "Exception during CV upload setup", e)
            Toast.makeText(this, "Error preparing CV upload: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("JobDetailsActivity", "Cannot submit application: User ID is null")
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            applyButton.isEnabled = true
            applyButton.text = "Apply Now"
            return
        }
        val jobId = jobId
        if (jobId == null) {
            Log.e("JobDetailsActivity", "Cannot submit application: Job ID is null")
            Toast.makeText(this, "Error: Job information missing", Toast.LENGTH_SHORT).show()
            applyButton.isEnabled = true
            applyButton.text = "Apply Now"
            return
        }
        val companyId = companyId
        if (companyId == null) {
            Log.e("JobDetailsActivity", "Cannot submit application: Company ID is null")
            Toast.makeText(this, "Error: Company information missing", Toast.LENGTH_SHORT).show()
            applyButton.isEnabled = true
            applyButton.text = "Apply Now"
            return
        }
        val cvUrl = currentCvUrl
        if (cvUrl == null) {
            Log.e("JobDetailsActivity", "Cannot submit application: CV URL is null")
            Toast.makeText(this, "Error: Resume information missing", Toast.LENGTH_SHORT).show()
            applyButton.isEnabled = true
            applyButton.text = "Apply Now"
            return
        }
        Log.d("JobDetailsActivity", "Submitting application - userId: $userId, jobId: $jobId, companyId: $companyId, cvUrl: $cvUrl")
        db.collection("users")
            .whereEqualTo("email", auth.currentUser?.email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    try {
                        val user = userDoc.toObject(User::class.java)
                        if (user != null) {
                            Log.d("JobDetailsActivity", "Successfully retrieved user profile for application")
                            val application = hashMapOf(
                                "jobId" to jobId,
                                "jobTitle" to jobTitle.text.toString(),
                                "userId" to userId,
                                "applicantName" to "${user.name} ${user.surname}",
                                "applicantEmail" to user.email,
                                "applicantPhone" to user.phoneNumber,
                                "applicantAddress" to "${user.city}, ${user.province}",
                                "applicantSummary" to user.summary,
                                "applicantSkills" to user.skills,
                                "applicantEducation" to user.education,
                                "applicantExperience" to user.experience,
                                "companyId" to companyId,
                                "companyName" to companyName.text.toString(),
                                "timestamp" to com.google.firebase.Timestamp.now(),
                                "applieddate" to com.google.firebase.Timestamp.now(),
                                "status" to "pending",
                                "resumeUrl" to cvUrl,
                                "coverLetter" to coverLetter
                            )
                            Log.d("JobDetailsActivity", "Adding application to Firestore")
                            db.collection("applications")
                                .add(application)
                                .addOnSuccessListener { documentReference ->
                                    Log.d("JobDetailsActivity", "Application submitted successfully with ID: ${documentReference.id}")



                                    Toast.makeText(this, "Application submitted successfully", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("JobDetailsActivity", "Error submitting application", e)
                                    Toast.makeText(this, "Error submitting application: ${e.message}", Toast.LENGTH_SHORT).show()
                                    applyButton.isEnabled = true
                                    applyButton.text = "Apply Now"
                                }
                        } else {
                            Log.e("JobDetailsActivity", "User object is null after conversion")
                            Toast.makeText(this, "Error submitting application: Invalid user data", Toast.LENGTH_SHORT).show()
                            applyButton.isEnabled = true
                            applyButton.text = "Apply Now"
                        }
                    } catch (e: Exception) {
                        Log.e("JobDetailsActivity", "Error converting user document to User object", e)
                        Toast.makeText(this, "Error processing user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        applyButton.isEnabled = true
                        applyButton.text = "Apply Now"
                    }
                } else {
                    Log.e("JobDetailsActivity", "No user document found for email: ${auth.currentUser?.email}")
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    applyButton.isEnabled = true
                    applyButton.text = "Apply Now"
                }
            }
            .addOnFailureListener { e ->
                Log.e("JobDetailsActivity", "Error fetching user profile", e)
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
        jobLocation.text = job.city
        jobType.text = job.type
        jobSalary.text = job.salary
        jobDescription.text = job.description
        jobRequirements.text = job.getRequirementsList().joinToString("\n• ", "• ")
        jobField.text = job.jobField.ifEmpty { "Not specified" }
        jobProvince.text = job.province.ifEmpty { "Not specified" }
        jobExperience.text = job.experienceLevel.ifEmpty { "Not specified" }
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        jobPostedDate.text = dateFormat.format(job.postedDate.toDate())
        checkIfJobIsSaved()
        if (auth.currentUser != null) {
            checkIfAlreadyApplied(false)
        }
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("companies")
                .whereEqualTo("email", currentUser.email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        applyButton.visibility = View.GONE
                        val managementLayout = findViewById<View>(R.id.jobManagementLayout)
                        managementLayout.visibility = View.VISIBLE
                        findViewById<MaterialButton>(R.id.editJobButton).setOnClickListener {
                            showEditJobDialog(job)
                        }
                        findViewById<MaterialButton>(R.id.deleteJobButton).setOnClickListener {
                            showDeleteConfirmationDialog(job)
                        }
                    } else {
                        applyButton.visibility = View.VISIBLE
                        findViewById<View>(R.id.jobManagementLayout).visibility = View.GONE
                        applyButton.setOnClickListener {
                            if (auth.currentUser != null) {
                                checkIfAlreadyApplied()
                            } else {
                                Toast.makeText(this, "Please sign in to apply", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
        }
    }
    private fun showEditJobDialog(job: Job) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_job, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Edit Job")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val updatedTitle = dialogView.findViewById<TextInputEditText>(R.id.editJobTitle).text.toString()
                val updatedDescription = dialogView.findViewById<TextInputEditText>(R.id.editJobDescription).text.toString()
                val updatedRequirements = dialogView.findViewById<TextInputEditText>(R.id.editJobRequirements).text.toString()
                val updatedSalary = dialogView.findViewById<TextInputEditText>(R.id.editJobSalary).text.toString()
                val updatedLocation = dialogView.findViewById<TextInputEditText>(R.id.editJobLocation).text.toString()
                val updatedType = dialogView.findViewById<TextInputEditText>(R.id.editJobType).text.toString()
                val jobRef = db.collection("jobs").document(job.id)
                val updates = mapOf(
                    "title" to updatedTitle,
                    "description" to updatedDescription,
                    "requirements" to updatedRequirements.split("\n"),
                    "salary" to updatedSalary,
                    "city" to updatedLocation,
                    "type" to updatedType,
                    "lastUpdated" to System.currentTimeMillis()
                )
                jobRef.update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Job updated successfully", Toast.LENGTH_SHORT).show()
                        loadJobDetails(job.id)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating job: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialogView.findViewById<TextInputEditText>(R.id.editJobTitle).setText(job.title)
        dialogView.findViewById<TextInputEditText>(R.id.editJobDescription).setText(job.description)
        dialogView.findViewById<TextInputEditText>(R.id.editJobRequirements).setText(job.getRequirementsList().joinToString("\n"))
        dialogView.findViewById<TextInputEditText>(R.id.editJobSalary).setText(job.salary)
        dialogView.findViewById<TextInputEditText>(R.id.editJobLocation).setText(job.city)
        dialogView.findViewById<TextInputEditText>(R.id.editJobType).setText(job.type)
        dialog.show()
    }
    private fun showDeleteConfirmationDialog(job: Job) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Job")
            .setMessage("Are you sure you want to delete this job posting? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteJob(job)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun deleteJob(job: Job) {
        db.collection("jobs").document(job.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Job deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    private fun checkIfJobIsSaved() {
        val userId = auth.currentUser?.uid ?: return
        val jobId = jobId ?: return
        db.collection("savedJobs")
            .whereEqualTo("userId", userId)
            .whereEqualTo("jobId", jobId)
            .get()
            .addOnSuccessListener { documents ->
                isJobSaved = !documents.isEmpty
                updateSaveButtonIcon()
            }
            .addOnFailureListener { e ->
                Log.e("JobDetailsActivity", "Error checking if job is saved", e)
                isJobSaved = false
                updateSaveButtonIcon()
            }
    }
    private fun updateSaveButtonIcon() {
        if (isJobSaved) {
            saveJobButton.setIconResource(R.drawable.ic_bookmark)
        } else {
            saveJobButton.setIconResource(R.drawable.ic_bookmark_border)
        }
    }
    private fun toggleSaveJob() {
        val userId = auth.currentUser?.uid
        val jobId = jobId
        if (userId == null || jobId == null) {
            Toast.makeText(this, "Please sign in to save jobs", Toast.LENGTH_SHORT).show()
            return
        }
        if (isJobSaved) {
            db.collection("savedJobs")
                .whereEqualTo("userId", userId)
                .whereEqualTo("jobId", jobId)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val document = documents.documents[0]
                        db.collection("savedJobs").document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                isJobSaved = false
                                updateSaveButtonIcon()
                                Toast.makeText(this, "Job removed from saved jobs", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("JobDetailsActivity", "Error removing job from saved jobs", e)
                                Toast.makeText(this, "Error removing job from saved jobs", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("JobDetailsActivity", "Error finding saved job", e)
                    Toast.makeText(this, "Error removing job from saved jobs", Toast.LENGTH_SHORT).show()
                }
        } else {
            val savedJob = hashMapOf(
                "userId" to userId,
                "jobId" to jobId,
                "savedAt" to com.google.firebase.Timestamp.now()
            )
            db.collection("savedJobs")
                .add(savedJob)
                .addOnSuccessListener {
                    isJobSaved = true
                    updateSaveButtonIcon()
                    Toast.makeText(this, "Job saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("JobDetailsActivity", "Error saving job", e)
                    Toast.makeText(this, "Error saving job", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun uploadCvToFirestore(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        try {
            Toast.makeText(this, "Processing CV for upload...", Toast.LENGTH_SHORT).show()

            // Read the PDF file and convert to base64
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null) {
                Toast.makeText(this, "Error: Could not read CV file", Toast.LENGTH_SHORT).show()
                applyButton.isEnabled = true
                applyButton.text = "Apply Now"
                return
            }

            // Check file size (limit to 5MB for Firestore)
            if (bytes.size > 5 * 1024 * 1024) {
                Toast.makeText(this, "Error: CV file is too large (max 5MB)", Toast.LENGTH_LONG).show()
                applyButton.isEnabled = true
                applyButton.text = "Apply Now"
                return
            }

            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

            Log.d("JobDetailsActivity", "CV converted to base64, size: ${base64String.length} characters")

            // Store in Firestore
            val cvRef = db.collection("cvs").document()
            val cvData = hashMapOf(
                "userId" to userId,
                "cvBase64" to base64String,
                "fileName" to "CV_${userId}_${System.currentTimeMillis()}.pdf",
                "uploadedAt" to com.google.firebase.Timestamp.now(),
                "fileSize" to bytes.size
            )

            cvRef.set(cvData)
                .addOnSuccessListener {
                    Log.d("JobDetailsActivity", "CV stored in Firestore successfully")
                    currentCvUrl = cvRef.id // Use document ID as reference
                    showCoverLetterDialog()
                }
                .addOnFailureListener { e ->
                    Log.e("JobDetailsActivity", "Error storing CV in Firestore", e)
                    Toast.makeText(this, "Error saving CV: ${e.message}", Toast.LENGTH_SHORT).show()
                    applyButton.isEnabled = true
                    applyButton.text = "Apply Now"
                }

        } catch (e: Exception) {
            Log.e("JobDetailsActivity", "Error processing CV for Firestore upload", e)
            Toast.makeText(this, "Error processing CV: ${e.message}", Toast.LENGTH_SHORT).show()
            applyButton.isEnabled = true
            applyButton.text = "Apply Now"
        }
    }
}