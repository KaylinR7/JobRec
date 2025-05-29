package com.example.jobrec
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.widget.LinearLayout
import java.io.ByteArrayOutputStream
import java.io.InputStream
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
import android.widget.Filter
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.jobrec.models.FieldCategories
import com.example.jobrec.adapters.CertificateAdapter
import com.example.jobrec.adapters.CertificateBadgeAdapter
import com.example.jobrec.adapters.CertificateBadge
import com.example.jobrec.utils.ImageUtils
import com.example.jobrec.utils.LocationUtils
class ProfileActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var nameInput: TextInputEditText
    private lateinit var surnameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneNumberInput: TextInputEditText
    private lateinit var cityInput: AutoCompleteTextView
    private lateinit var summaryInput: TextInputEditText
    private lateinit var skillsInput: AutoCompleteTextView
    private lateinit var linkedinInput: TextInputEditText
    private lateinit var githubInput: TextInputEditText
    private lateinit var portfolioInput: TextInputEditText
    private lateinit var profileImage: ShapeableImageView
    private lateinit var changePhotoButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var provinceInput: AutoCompleteTextView
    private lateinit var yearsOfExperienceInput: AutoCompleteTextView
    private lateinit var expectedSalaryInput: AutoCompleteTextView
    private lateinit var fieldInput: AutoCompleteTextView
    private lateinit var subFieldInput: AutoCompleteTextView
    private lateinit var skillsChipGroup: ChipGroup
    private val selectedSkills = mutableListOf<String>()
    private var selectedProvince: String = ""
    private var selectedCity: String = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var experienceRecyclerView: RecyclerView
    private lateinit var educationRecyclerView: RecyclerView
    private lateinit var addExperienceButton: MaterialButton
    private lateinit var addEducationButton: MaterialButton
    private lateinit var experienceAdapter: ExperienceAdapter
    private lateinit var educationAdapter: EducationAdapter
    private lateinit var certificateAdapter: CertificateAdapter
    private lateinit var certificateBadgeAdapter: CertificateBadgeAdapter
    private lateinit var referencesContainer: LinearLayout
    private lateinit var addReferenceButton: MaterialButton
    private lateinit var certificatesRecyclerView: RecyclerView
    private lateinit var certificateBadgesRecyclerView: RecyclerView
    private lateinit var addCertificateButton: MaterialButton
    private lateinit var getSuggestionsButton: MaterialButton
    private val TAG = "ProfileActivity"
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    result.data?.data?.let { uri ->
                        Log.d(TAG, "Image selected successfully: $uri")
                        uploadImage(uri)
                    } ?: run {
                        Log.e(TAG, "No image data received")
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "Image selection cancelled by user")
                }
                else -> {
                    Log.e(TAG, "Image selection failed with result code: ${result.resultCode}")
                    Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling image selection result", e)
            Toast.makeText(this, "Error processing selected image: ${e.message}", Toast.LENGTH_SHORT).show()
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
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Profile"
        }
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize Firebase Storage with explicit bucket configuration
        try {
            storage = FirebaseStorage.getInstance("gs://careerworx-f5bc6.firebasestorage.app")
            Log.d(TAG, "Firebase Storage initialized with bucket: ${storage.reference.bucket}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Storage with custom bucket, using default", e)
            storage = FirebaseStorage.getInstance()
        }

        Log.d(TAG, "Current user: ${auth.currentUser?.uid}")
        initializeViews()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        setupClickListeners()
        setupDropdowns()
        loadUserData()
    }
    private fun initializeViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        nameInput = findViewById(R.id.nameInput)
        surnameInput = findViewById(R.id.surnameInput)
        emailInput = findViewById(R.id.emailInput)
        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        cityInput = findViewById(R.id.cityInput)
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
        getSuggestionsButton = findViewById(R.id.getSuggestionsButton)
        provinceInput = findViewById(R.id.provinceInput)
        yearsOfExperienceInput = findViewById(R.id.yearsOfExperienceInput)
        expectedSalaryInput = findViewById(R.id.expectedSalaryInput)
        fieldInput = findViewById(R.id.fieldInput)
        subFieldInput = findViewById(R.id.subFieldInput)
        experienceRecyclerView.layoutManager = LinearLayoutManager(this)
        educationRecyclerView.layoutManager = LinearLayoutManager(this)
        experienceAdapter = ExperienceAdapter()
        educationAdapter = EducationAdapter()
        experienceRecyclerView.adapter = experienceAdapter
        educationRecyclerView.adapter = educationAdapter

        // Initialize certificate components
        certificatesRecyclerView = findViewById(R.id.certificatesRecyclerView)
        certificateBadgesRecyclerView = findViewById(R.id.certificateBadgesRecyclerView)
        addCertificateButton = findViewById(R.id.addCertificateButton)

        // Setup certificate adapters
        certificateAdapter = CertificateAdapter()
        certificateBadgeAdapter = CertificateBadgeAdapter { badge ->
            // Handle badge click - could show details dialog
            showCertificateBadgeDetails(badge)
        }

        certificatesRecyclerView.layoutManager = LinearLayoutManager(this)
        certificatesRecyclerView.adapter = certificateAdapter

        certificateBadgesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        certificateBadgesRecyclerView.adapter = certificateBadgeAdapter
    }
    private fun setupClickListeners() {
        // Setup pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshProfile()
        }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.primary_dark,
            R.color.accent
        )

        changePhotoButton.setOnClickListener {
            // Hide keyboard and clear focus to prevent IME conflicts
            hideKeyboard()
            // Add a small delay to ensure keyboard is hidden before opening picker
            changePhotoButton.postDelayed({
                checkPermissionAndPickImage()
            }, 150)
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
        addCertificateButton.setOnClickListener {
            certificateAdapter.addNewCertificate()
            updateCertificateBadges()
        }
        getSuggestionsButton.setOnClickListener {
            showSummarySuggestions()
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
        val yearsOptions = arrayOf("0-1 years", "1-3 years", "3-5 years", "5-10 years", "10+ years")
        val yearsAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            yearsOptions
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = yearsOptions
                        results.count = yearsOptions.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
        yearsOfExperienceInput.setAdapter(yearsAdapter)
        yearsOfExperienceInput.setOnClickListener {
            yearsOfExperienceInput.showDropDown()
        }
        // Setup cascading location dropdowns
        LocationUtils.setupCascadingLocationSpinners(
            context = this,
            provinceSpinner = provinceInput,
            citySpinner = cityInput
        ) { province, city ->
            selectedProvince = province
            selectedCity = city
            Log.d(TAG, "Location selected: $city, $province")
        }
        val salaryOptions = arrayOf(
            "R0 - R10,000",
            "R10,000 - R20,000",
            "R20,000 - R30,000",
            "R30,000 - R40,000",
            "R40,000 - R50,000",
            "R50,000+"
        )
        val salaryAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            salaryOptions
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = salaryOptions
                        results.count = salaryOptions.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
        expectedSalaryInput.setAdapter(salaryAdapter)
        expectedSalaryInput.setOnClickListener {
            expectedSalaryInput.showDropDown()
        }
        val fieldOptions = FieldCategories.fields.keys.toTypedArray()
        val fieldAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            fieldOptions
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = fieldOptions
                        results.count = fieldOptions.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
        fieldInput.setAdapter(fieldAdapter)
        fieldInput.setOnClickListener {
            fieldInput.showDropDown()
        }
        fieldInput.setOnItemClickListener { _, _, _, _ ->
            val selectedField = fieldInput.text.toString()
            updateSubFieldDropdown(selectedField)
            updateSkillsDropdown(selectedField)
            updateCertificateOptionsForField(selectedField)
        }
        subFieldInput.isEnabled = false
        skillsInput.isEnabled = false
    }
    private fun refreshProfile() {
        clearProfileData()
        loadUserData()
    }

    private fun clearProfileData() {
        // Clear all input fields
        nameInput.setText("")
        surnameInput.setText("")
        emailInput.setText("")
        phoneNumberInput.setText("")
        cityInput.setText("")
        summaryInput.setText("")
        linkedinInput.setText("")
        githubInput.setText("")
        portfolioInput.setText("")
        provinceInput.setText("")
        yearsOfExperienceInput.setText("")
        expectedSalaryInput.setText("")
        fieldInput.setText("")
        subFieldInput.setText("")

        // Clear skills
        selectedSkills.clear()
        skillsChipGroup.removeAllViews()

        // Clear adapters
        experienceAdapter.clearExperienceList()
        educationAdapter.clearEducationList()
        certificateAdapter.clearCertificates()

        // Clear references
        referencesContainer.removeAllViews()

        // Reset profile image
        profileImage.setImageResource(R.drawable.ic_person)

        // Reset dropdowns
        subFieldInput.isEnabled = false
        skillsInput.isEnabled = false
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d(TAG, "Loading user data for userId: $userId")
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    swipeRefreshLayout.isRefreshing = false
                    if (document.exists()) {
                        Log.d(TAG, "Document exists: ${document.exists()}")
                        nameInput.setText(document.getString("name"))
                        surnameInput.setText(document.getString("surname"))
                        emailInput.setText(document.getString("email"))
                        phoneNumberInput.setText(document.getString("phoneNumber"))
                        summaryInput.setText(document.getString("summary"))

                        // Load location data and setup cascading dropdowns
                        val province = document.getString("province") ?: ""
                        val city = document.getString("city") ?: ""

                        if (province.isNotEmpty() || city.isNotEmpty()) {
                            // Reinitialize dropdowns with loaded data
                            LocationUtils.setupCascadingLocationSpinners(
                                context = this@ProfileActivity,
                                provinceSpinner = provinceInput,
                                citySpinner = cityInput,
                                selectedProvince = province,
                                selectedCity = city
                            ) { selectedProv, selectedCit ->
                                selectedProvince = selectedProv
                                selectedCity = selectedCit
                                Log.d(TAG, "Location loaded: $selectedCit, $selectedProv")
                            }
                        }
                        yearsOfExperienceInput.setText(document.getString("yearsOfExperience") ?: "")
                        expectedSalaryInput.setText(document.getString("expectedSalary") ?: "")
                        val field = document.getString("field") ?: ""
                        fieldInput.setText(field)
                        if (field.isNotEmpty()) {
                            updateSubFieldDropdown(field)
                            updateSkillsDropdown(field)
                            updateCertificateOptionsForField(field)
                            subFieldInput.setText(document.getString("subField") ?: "")
                        }
                        linkedinInput.setText(document.getString("linkedin"))
                        githubInput.setText(document.getString("github"))
                        portfolioInput.setText(document.getString("portfolio"))
                        val skills = document.get("skills") as? List<String>
                        skills?.forEach { skill ->
                            selectedSkills.add(skill)
                            addSkillChip(skill)
                        }
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
                        val referencesList = document.get("references") as? List<Map<String, String>>
                        referencesList?.forEach { reference ->
                            val referenceLayout = layoutInflater.inflate(R.layout.item_reference, referencesContainer, false)
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferenceName).setText(reference["name"])
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferencePosition).setText(reference["position"])
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferenceCompany).setText(reference["company"])
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferenceEmail).setText(reference["email"])
                            referenceLayout.findViewById<TextInputEditText>(R.id.etReferencePhone).setText(reference["phone"])
                            referenceLayout.findViewById<MaterialButton>(R.id.btnRemoveReference).setOnClickListener {
                                referencesContainer.removeView(referenceLayout)
                            }
                            referencesContainer.addView(referenceLayout)
                        }

                        // Load certificates
                        val certificatesList = document.get("certificates") as? List<Map<String, String>>
                        certificatesList?.forEach { certificate ->
                            certificateAdapter.addCertificate(
                                CertificateAdapter.Certificate(
                                    certificate["name"] ?: "",
                                    certificate["issuer"] ?: "",
                                    certificate["year"] ?: "",
                                    certificate["description"] ?: ""
                                )
                            )
                        }
                        updateCertificateBadges()

                        // Load profile image using ImageUtils
                        val imageUrl = document.getString("profileImageUrl")
                        val imageBase64 = document.getString("profileImageBase64")

                        ImageUtils.loadProfileImage(
                            context = this,
                            imageView = profileImage,
                            imageUrl = imageUrl,
                            imageBase64 = imageBase64,
                            isCircular = true
                        )
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { e ->
                    swipeRefreshLayout.isRefreshing = false
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
                "surname" to surnameInput.text.toString().trim(),
                "email" to emailInput.text.toString().trim(),
                "phoneNumber" to phoneNumberInput.text.toString().trim(),
                "city" to selectedCity,
                "summary" to summaryInput.text.toString().trim(),
                "linkedin" to linkedinInput.text.toString().trim(),
                "github" to githubInput.text.toString().trim(),
                "portfolio" to portfolioInput.text.toString().trim(),
                "skills" to getSkillsList(),
                "experience" to experienceAdapter.getExperienceList(),
                "education" to educationAdapter.getEducationList(),
                "references" to getReferencesList(),
                "certificates" to certificateAdapter.getCertificatesList(),
                "province" to selectedProvince,
                "yearsOfExperience" to yearsOfExperienceInput.text.toString().trim(),
                "expectedSalary" to expectedSalaryInput.text.toString().trim(),
                "field" to fieldInput.text.toString().trim(),
                "subField" to subFieldInput.text.toString().trim()
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
        return selectedSkills.toList()
    }
    private fun addSkillChip(skill: String) {
        val chip = Chip(this).apply {
            text = skill
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                selectedSkills.remove(skill)
                skillsChipGroup.removeView(this)
            }
        }
        skillsChipGroup.addView(chip)
    }
    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openImagePicker()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                    showPermissionExplanationDialog(Manifest.permission.READ_MEDIA_IMAGES)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openImagePicker()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionExplanationDialog(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    private fun showPermissionExplanationDialog(permission: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage("We need access to your photos to set a profile picture. Please grant this permission to continue.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Permission is required to change profile picture", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    private fun hideKeyboard() {
        try {
            currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding keyboard", e)
        }
    }

    private fun openImagePicker() {
        try {
            // Ensure keyboard is hidden before opening image picker
            hideKeyboard()
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/jpg"))
            }
            getContent.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening image picker", e)
            Toast.makeText(this, "Error opening image picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun uploadImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot upload image: User ID is null")
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Use the correct path format that matches our storage rules
            val fileName = "$userId.jpg"
            val storageRef = storage.reference.child("profile_images/$fileName")

            Log.d(TAG, "Uploading image to path: profile_images/$fileName")
            Log.d(TAG, "Storage bucket: ${storage.reference.bucket}")
            Log.d(TAG, "User ID: $userId")

            Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show()

            storageRef.putFile(imageUri)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    Log.d(TAG, "Upload progress: $progress%")
                }
                .addOnSuccessListener { taskSnapshot ->
                    Log.d(TAG, "Image uploaded successfully to Firebase Storage")
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        Log.d(TAG, "Download URL obtained: $downloadUri")
                        db.collection("users").document(userId)
                            .update("profileImageUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                Log.d(TAG, "Profile image URL updated in Firestore")
                                Glide.with(this)
                                    .load(downloadUri)
                                    .transform(CircleCrop())
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(profileImage)
                                Toast.makeText(this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error updating profile image URL in Firestore", e)
                                Toast.makeText(this, "Error saving profile picture URL: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error getting download URL", e)
                        Toast.makeText(this, "Error getting download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error uploading image to Firebase Storage", e)
                    when {
                        e.message?.contains("Object does not exist") == true ||
                        e.message?.contains("404") == true ||
                        e.message?.contains("bucket") == true -> {
                            Log.e(TAG, "Storage bucket not available - using Firestore fallback")
                            // Use Firestore as fallback for image storage
                            uploadImageToFirestore(imageUri)
                        }
                        e.message?.contains("403") == true -> {
                            Log.e(TAG, "Permission denied - check storage rules")
                            Toast.makeText(this, "Permission denied. Please check storage permissions.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Log.e(TAG, "Storage upload failed, trying Firestore fallback")
                            uploadImageToFirestore(imageUri)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during image upload", e)
            Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToFirestore(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        try {
            Toast.makeText(this, "Processing image for upload...", Toast.LENGTH_SHORT).show()

            // Convert image to base64 and store in Firestore
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap == null) {
                Toast.makeText(this, "Error: Could not process image", Toast.LENGTH_SHORT).show()
                return
            }

            // Resize image to reduce size (max 512x512)
            val resizedBitmap = resizeBitmap(bitmap, 512, 512)

            // Convert to base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            Log.d(TAG, "Image converted to base64, size: ${base64String.length} characters")

            // Store in Firestore
            db.collection("users").document(userId)
                .update("profileImageBase64", base64String)
                .addOnSuccessListener {
                    Log.d(TAG, "Profile image stored in Firestore successfully")
                    // Load the image into the ImageView
                    profileImage.setImageBitmap(resizedBitmap)
                    Toast.makeText(this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error storing image in Firestore", e)
                    Toast.makeText(this, "Error saving profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image for Firestore upload", e)
            Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val aspectRatio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun retryUploadWithDifferentStorage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        try {
            // Try with default Firebase Storage instance
            val defaultStorage = FirebaseStorage.getInstance()
            val fileName = "$userId.jpg"
            val storageRef = defaultStorage.reference.child("profile_images/$fileName")

            Log.d(TAG, "Retrying upload with default storage instance")
            Toast.makeText(this, "Retrying upload...", Toast.LENGTH_SHORT).show()

            storageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    Log.d(TAG, "Retry upload successful")
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        db.collection("users").document(userId)
                            .update("profileImageUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                Glide.with(this)
                                    .load(downloadUri)
                                    .transform(CircleCrop())
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(profileImage)
                                Toast.makeText(this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error updating profile image URL in Firestore on retry", e)
                                Toast.makeText(this, "Error saving profile picture URL: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Retry upload also failed", e)
                    Toast.makeText(this, "Upload failed. Firebase Storage may not be configured properly.", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error during retry upload", e)
            Toast.makeText(this, "Retry failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
    private fun updateSubFieldDropdown(field: String) {
        val subFields = FieldCategories.fields[field] ?: listOf()
        if (subFields.isNotEmpty()) {
            val subFieldAdapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                subFields.toTypedArray()
            ) {
                override fun getFilter(): Filter {
                    return object : Filter() {
                        override fun performFiltering(constraint: CharSequence?): FilterResults {
                            val results = FilterResults()
                            results.values = subFields.toTypedArray()
                            results.count = subFields.size
                            return results
                        }
                        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                            notifyDataSetChanged()
                        }
                    }
                }
            }
            subFieldInput.isEnabled = true
            subFieldInput.setText("", false)
            subFieldInput.setAdapter(subFieldAdapter)
            subFieldInput.setOnClickListener {
                subFieldInput.showDropDown()
            }
        } else {
            subFieldInput.setText("")
            subFieldInput.isEnabled = false
        }
    }

    private fun updateSkillsDropdown(field: String) {
        val skills = FieldCategories.skills[field] ?: listOf()
        if (skills.isNotEmpty()) {
            val skillsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, skills)
            skillsInput.setAdapter(skillsAdapter)
            skillsInput.isEnabled = true
            skillsInput.setText("", false)

            skillsInput.setOnItemClickListener { _, _, position, _ ->
                val selectedSkill = skills[position]
                if (!selectedSkills.contains(selectedSkill)) {
                    selectedSkills.add(selectedSkill)
                    addSkillChip(selectedSkill)
                    skillsInput.setText("", false)
                }
            }
        } else {
            skillsInput.setText("")
            skillsInput.isEnabled = false
        }
    }

    private fun updateCertificateOptionsForField(field: String) {
        certificateAdapter.updateCertificateOptionsForField(field)
    }
    private fun showSummarySuggestions() {
        val field = fieldInput.text.toString()
        val subField = subFieldInput.text.toString()
        val experience = yearsOfExperienceInput.text.toString()
        val skills = getSkillsList()
        val suggestions = generateSummarySuggestions(field, subField, experience, skills)
        if (suggestions.isEmpty()) {
            Toast.makeText(this, "Please fill in more profile details to get suggestions", Toast.LENGTH_SHORT).show()
            return
        }
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Professional Summary Suggestions")
        builder.setItems(suggestions.toTypedArray()) { _, which ->
            summaryInput.setText(suggestions[which])
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
    private fun generateSummarySuggestions(field: String, subField: String, experience: String, skills: List<String>): List<String> {
        val suggestions = mutableListOf<String>()
        if (field.isEmpty()) {
            return suggestions
        }
        val years = when {
            experience.contains("0-1") -> "entry-level"
            experience.contains("1-3") -> "junior"
            experience.contains("3-5") -> "mid-level"
            experience.contains("5-10") -> "senior"
            experience.contains("10+") -> "expert"
            else -> ""
        }
        val skillsText = if (skills.isNotEmpty()) {
            "with expertise in ${skills.take(3).joinToString(", ")}" +
            if (skills.size > 3) " and other areas" else ""
        } else ""
        if (field.isNotEmpty() && years.isNotEmpty()) {
            val fieldSpecific = if (subField.isNotEmpty()) "$field specializing in $subField" else field
            suggestions.add("$years $fieldSpecific professional $skillsText. Passionate about delivering high-quality work and continuously improving my skills to stay at the forefront of the industry.")
            suggestions.add("Results-driven $years professional in the $fieldSpecific field $skillsText. Committed to excellence and innovation in every project I undertake.")
            suggestions.add("Dedicated $fieldSpecific professional with ${experience.lowercase()} experience $skillsText. Seeking opportunities to apply my skills and knowledge to make a meaningful impact.")
        }
        return suggestions
    }

    private fun updateCertificateBadges() {
        val certificates = certificateAdapter.getCertificatesList()
        val badges = certificates.map { cert ->
            CertificateBadge(
                name = cert["name"] ?: "",
                issuer = cert["issuer"] ?: "",
                year = cert["year"] ?: "",
                description = cert["description"] ?: ""
            )
        }.filter { it.name.isNotEmpty() } // Only show badges with names

        certificateBadgeAdapter.submitList(badges)
    }

    private fun showCertificateBadgeDetails(badge: CertificateBadge) {
        MaterialAlertDialogBuilder(this)
            .setTitle(badge.name)
            .setMessage(buildString {
                append("Issuer: ${badge.issuer}\n")
                append("Year: ${badge.year}")
                if (badge.description.isNotEmpty()) {
                    append("\n\nDescription: ${badge.description}")
                }
            })
            .setPositiveButton("OK", null)
            .show()
    }
}