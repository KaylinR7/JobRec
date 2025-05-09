package com.example.jobrec

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import androidx.appcompat.widget.Toolbar
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneNumberInput: TextInputEditText
    private lateinit var addressInput: TextInputEditText
    private lateinit var summaryInput: TextInputEditText
    private lateinit var skillsInput: TextInputEditText
    private lateinit var linkedinInput: TextInputEditText
    private lateinit var githubInput: TextInputEditText
    private lateinit var portfolioInput: TextInputEditText
    private lateinit var profileImage: ShapeableImageView
    private lateinit var changePhotoButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var provinceInput: AutoCompleteTextView
    private lateinit var yearsOfExperienceInput: AutoCompleteTextView
    private lateinit var certificateInput: AutoCompleteTextView
    private lateinit var expectedSalaryInput: AutoCompleteTextView
    private lateinit var fieldInput: AutoCompleteTextView
    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var experienceRecyclerView: RecyclerView
    private lateinit var educationRecyclerView: RecyclerView
    private lateinit var addExperienceButton: MaterialButton
    private lateinit var addEducationButton: MaterialButton
    private lateinit var experienceAdapter: ExperienceAdapter
    private lateinit var educationAdapter: EducationAdapter
    private lateinit var referencesContainer: LinearLayout
    private lateinit var addReferenceButton: MaterialButton
    private val TAG = "ProfileActivity"

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImage(uri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission required to change profile picture", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Set up toolbar with back button
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Profile"
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize views
        initializeViews()

        // Set up back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Set up button click listeners
        setupClickListeners()

        // Setup dropdown menus
        setupDropdowns()

        // Load user data
        loadUserData()
    }

    private fun initializeViews() {
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        addressInput = findViewById(R.id.addressInput)
        summaryInput = findViewById(R.id.summaryInput)
        skillsInput = findViewById(R.id.skillsInput)
        linkedinInput = findViewById(R.id.linkedinInput)
        githubInput = findViewById(R.id.githubInput)
        portfolioInput = findViewById(R.id.portfolioInput)
        profileImage = findViewById(R.id.profileImage)
        changePhotoButton = findViewById(R.id.changePhotoButton)
        saveButton = findViewById(R.id.saveButton)
        skillsChipGroup = findViewById(R.id.skillsChipGroup)
        experienceRecyclerView = findViewById(R.id.experienceRecyclerView)
        educationRecyclerView = findViewById(R.id.educationRecyclerView)
        addExperienceButton = findViewById(R.id.addExperienceButton)
        addEducationButton = findViewById(R.id.addEducationButton)
        referencesContainer = findViewById(R.id.referencesContainer)
        addReferenceButton = findViewById(R.id.addReferenceButton)

        // Initialize dropdown fields
        provinceInput = findViewById(R.id.provinceInput)
        yearsOfExperienceInput = findViewById(R.id.yearsOfExperienceInput)
        certificateInput = findViewById(R.id.certificateInput)
        expectedSalaryInput = findViewById(R.id.expectedSalaryInput)
        fieldInput = findViewById(R.id.fieldInput)

        // Set up RecyclerViews
        experienceRecyclerView.layoutManager = LinearLayoutManager(this)
        educationRecyclerView.layoutManager = LinearLayoutManager(this)
        experienceAdapter = ExperienceAdapter()
        educationAdapter = EducationAdapter()
        experienceRecyclerView.adapter = experienceAdapter
        educationRecyclerView.adapter = educationAdapter
    }

    private fun setupClickListeners() {
        changePhotoButton.setOnClickListener {
            checkPermissionAndPickImage()
        }

        saveButton.setOnClickListener {
            saveProfile()
        }

        addExperienceButton.setOnClickListener {
            experienceAdapter.addNewExperience()
        }

        addEducationButton.setOnClickListener {
            educationAdapter.addNewEducation()
        }

        addReferenceButton.setOnClickListener {
            addReferenceField()
        }

        skillsInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val skill = skillsInput.text.toString().trim()
                if (skill.isNotEmpty()) {
                    addSkillChip(skill)
                    skillsInput.text?.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun addReferenceField() {
        val referenceLayout = layoutInflater.inflate(R.layout.item_reference, referencesContainer, false)

        // Set up remove button
        referenceLayout.findViewById<MaterialButton>(R.id.btnRemoveReference)?.setOnClickListener {
            referencesContainer.removeView(referenceLayout)
        }

        referencesContainer.addView(referenceLayout)
    }

    private fun getReferencesList(): List<Map<String, String>> {
        val references = mutableListOf<Map<String, String>>()
        for (i in 0 until referencesContainer.childCount) {
            val referenceView = referencesContainer.getChildAt(i)
            val name = referenceView.findViewById<TextInputEditText>(R.id.etReferenceName).text.toString()
            val position = referenceView.findViewById<TextInputEditText>(R.id.etReferencePosition).text.toString()
            val company = referenceView.findViewById<TextInputEditText>(R.id.etReferenceCompany).text.toString()
            val email = referenceView.findViewById<TextInputEditText>(R.id.etReferenceEmail).text.toString()
            val phone = referenceView.findViewById<TextInputEditText>(R.id.etReferencePhone).text.toString()

            if (name.isNotEmpty() || position.isNotEmpty() || company.isNotEmpty() || email.isNotEmpty() || phone.isNotEmpty()) {
                references.add(mapOf(
                    "name" to name,
                    "position" to position,
                    "company" to company,
                    "email" to email,
                    "phone" to phone
                ))
            }
        }
        return references
    }

    private fun setupDropdowns() {
        // Province options
        val provinces = arrayOf(
            "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal",
            "Limpopo", "Mpumalanga", "Northern Cape", "North West", "Western Cape"
        )
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, provinces)
        provinceInput.setAdapter(provinceAdapter)

        // Years of Experience options
        val yearsOptions = arrayOf("0-1 years", "1-3 years", "3-5 years", "5-10 years", "10+ years")
        val yearsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearsOptions)
        yearsOfExperienceInput.setAdapter(yearsAdapter)

        // Certificate options
        val certificateOptions = arrayOf(
            "None",
            "High School Diploma",
            "Associate's Degree",
            "Bachelor's Degree",
            "Master's Degree",
            "PhD",
            "Professional Certification",
            "Technical Certification"
        )
        val certificateAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, certificateOptions)
        certificateInput.setAdapter(certificateAdapter)

        // Expected Salary options
        val salaryOptions = arrayOf(
            "R0 - R10,000",
            "R10,000 - R20,000",
            "R20,000 - R30,000",
            "R30,000 - R40,000",
            "R40,000 - R50,000",
            "R50,000+"
        )
        val salaryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salaryOptions)
        expectedSalaryInput.setAdapter(salaryAdapter)

        // Field options
        val fieldOptions = arrayOf(
            "Information Technology",
            "Healthcare",
            "Law",
            "Education",
            "Engineering",
            "Business",
            "Finance",
            "Marketing",
            "Sales",
            "Customer Service",
            "Manufacturing",
            "Construction",
            "Transportation",
            "Hospitality",
            "Other"
        )
        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fieldOptions)
        fieldInput.setAdapter(fieldAdapter)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d(TAG, "Loading user data for userId: $userId")
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d(TAG, "Document exists: ${document.exists()}")

                        // Load basic information
                        nameInput.setText(document.getString("name"))
                        emailInput.setText(document.getString("email"))
                        phoneNumberInput.setText(document.getString("phoneNumber"))
                        addressInput.setText(document.getString("address"))
                        summaryInput.setText(document.getString("summary"))

                        // Load dropdown values
                        provinceInput.setText(document.getString("province") ?: "")
                        yearsOfExperienceInput.setText(document.getString("yearsOfExperience") ?: "")
                        certificateInput.setText(document.getString("certificate") ?: "")
                        expectedSalaryInput.setText(document.getString("expectedSalary") ?: "")
                        fieldInput.setText(document.getString("field") ?: "")

                        // Load social links
                        linkedinInput.setText(document.getString("linkedin"))
                        githubInput.setText(document.getString("github"))
                        portfolioInput.setText(document.getString("portfolio"))

                        // Load skills
                        val skills = document.get("skills") as? List<String>
                        skills?.forEach { skill ->
                            addSkillChip(skill)
                        }

                        // Load experience
                        val experienceList = document.get("experience") as? List<Map<String, Any>>
                        experienceList?.forEach { experience ->
                            experienceAdapter.addExperience(
                                ExperienceAdapter.Experience(
                                    experience["title"] as? String ?: "",
                                    experience["company"] as? String ?: "",
                                    experience["startDate"] as? String ?: "",
                                    experience["endDate"] as? String ?: "",
                                    experience["description"] as? String ?: ""
                                )
                            )
                        }

                        // Load education
                        val educationList = document.get("education") as? List<Map<String, Any>>
                        educationList?.forEach { education ->
                            educationAdapter.addEducation(
                                EducationAdapter.Education(
                                    education["institution"] as? String ?: "",
                                    education["degree"] as? String ?: "",
                                    education["startDate"] as? String ?: "",
                                    education["endDate"] as? String ?: "",
                                    education["description"] as? String ?: ""
                                )
                            )
                        }

                        // Load references
                        val referencesList = document.get("references") as? List<Map<String, String>>
                        referencesList?.forEach { reference ->
                            val referenceLayout = layoutInflater.inflate(R.layout.item_reference, referencesContainer, false)
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferenceName).setText(reference["name"])
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferencePosition).setText(reference["position"])
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferenceCompany).setText(reference["company"])
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferenceEmail).setText(reference["email"])
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferencePhone).setText(reference["phone"])

                            // Set up remove button
                            referenceLayout.findViewById<MaterialButton>(R.id.btnRemoveReference).setOnClickListener {
                                referencesContainer.removeView(referenceLayout)
                            }

                            referencesContainer.addView(referenceLayout)
                        }

                        // Load profile image
                        val imageUrl = document.getString("profileImageUrl")
                        if (!imageUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(imageUrl)
                                .transform(CircleCrop())
                                .into(profileImage)
                        }
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user data", e)
                    Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userData = hashMapOf(
                "name" to nameInput.text.toString().trim(),
                "email" to emailInput.text.toString().trim(),
                "phoneNumber" to phoneNumberInput.text.toString().trim(),
                "address" to addressInput.text.toString().trim(),
                "summary" to summaryInput.text.toString().trim(),
                "linkedin" to linkedinInput.text.toString().trim(),
                "github" to githubInput.text.toString().trim(),
                "portfolio" to portfolioInput.text.toString().trim(),
                "skills" to getSkillsList(),
                "experience" to experienceAdapter.getExperienceList(),
                "education" to educationAdapter.getEducationList(),
                "references" to getReferencesList(),
                "province" to provinceInput.text.toString().trim(),
                "yearsOfExperience" to yearsOfExperienceInput.text.toString().trim(),
                "certificate" to certificateInput.text.toString().trim(),
                "expectedSalary" to expectedSalaryInput.text.toString().trim(),
                "field" to fieldInput.text.toString().trim()
            )

            db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving profile", e)
                    Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getSkillsList(): List<String> {
        val skills = mutableListOf<String>()
        for (i in 0 until skillsChipGroup.childCount) {
            val chip = skillsChipGroup.getChildAt(i) as? Chip
            chip?.text?.toString()?.let { skills.add(it) }
        }
        return skills
    }

    private fun addSkillChip(skill: String) {
        val chip = Chip(this).apply {
            text = skill
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                skillsChipGroup.removeView(this)
            }
        }
        skillsChipGroup.addView(chip)
    }

    private fun checkPermissionAndPickImage() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun uploadImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("profile_images/$userId.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Update profile image in Firestore
                    db.collection("users").document(userId)
                        .update("profileImageUrl", downloadUri.toString())
                        .addOnSuccessListener {
                            // Update image in ImageView
                            Glide.with(this)
                                .load(downloadUri)
                                .transform(CircleCrop())
                                .into(profileImage)
                            Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading image", e)
                Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            R.id.action_save_profile -> {
                saveProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}