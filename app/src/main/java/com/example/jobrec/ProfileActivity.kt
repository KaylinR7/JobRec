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
    private lateinit var surnameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneNumberInput: TextInputEditText
    private lateinit var addressInput: TextInputEditText
    private lateinit var summaryInput: TextInputEditText
    private lateinit var skillsInput: AutoCompleteTextView
    private lateinit var hobbiesInput: AutoCompleteTextView
    private lateinit var achievementsInput: TextInputEditText
    private lateinit var linkedinInput: TextInputEditText
    private lateinit var githubInput: TextInputEditText
    private lateinit var portfolioInput: TextInputEditText
    private lateinit var profileImage: ShapeableImageView
    private lateinit var changePhotoButton: MaterialButton
    private lateinit var addEducationButton: MaterialButton
    private lateinit var addExperienceButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var hobbiesChipGroup: ChipGroup
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var currentUser: User? = null
    
    // Add RecyclerViews and adapters
    private lateinit var educationRecyclerView: RecyclerView
    private lateinit var experienceRecyclerView: RecyclerView
    private lateinit var educationAdapter: EducationAdapter
    private lateinit var experienceAdapter: ExperienceAdapter
    private lateinit var referencesRecyclerView: RecyclerView
    private lateinit var referenceAdapter: ReferenceAdapter
    private lateinit var addReferenceButton: MaterialButton

    // Common skills and hobbies suggestions
    private val commonSkills = listOf(
        "Java", "Kotlin", "Python", "JavaScript", "TypeScript", "React", "Angular", "Vue.js",
        "Node.js", "Spring Boot", "Django", "Flask", "SQL", "NoSQL", "MongoDB", "PostgreSQL",
        "AWS", "Azure", "GCP", "Docker", "Kubernetes", "Git", "CI/CD", "Agile", "Scrum",
        "Project Management", "UI/UX Design", "Mobile Development", "iOS", "Android", "Flutter",
        "React Native", "Machine Learning", "Data Science", "Artificial Intelligence", "DevOps",
        "System Architecture", "Microservices", "REST APIs", "GraphQL", "Web Development",
        "Frontend Development", "Backend Development", "Full Stack Development", "Cloud Computing",
        "Cybersecurity", "Network Administration", "Database Administration", "Quality Assurance",
        "Testing", "Automation", "Business Analysis", "Product Management", "Technical Writing",
        "Documentation", "Public Speaking", "Leadership", "Team Management", "Problem Solving",
        "Critical Thinking", "Communication", "Collaboration", "Time Management", "Adaptability"
    )

    private val commonHobbies = listOf(
        "Reading", "Writing", "Photography", "Painting", "Drawing", "Music", "Playing Instruments",
        "Singing", "Dancing", "Cooking", "Baking", "Gardening", "Hiking", "Camping", "Traveling",
        "Sports", "Fitness", "Yoga", "Meditation", "Chess", "Board Games", "Video Games",
        "Coding", "DIY Projects", "Woodworking", "Knitting", "Sewing", "Fishing", "Swimming",
        "Cycling", "Running", "Martial Arts", "Dancing", "Theater", "Movies", "TV Shows",
        "Podcasts", "Blogging", "Social Media", "Volunteering", "Community Service", "Learning Languages",
        "Astronomy", "Bird Watching", "Collecting", "Crafting", "Pottery", "Calligraphy",
        "Origami", "Puzzle Solving", "Sudoku", "Crossword Puzzles", "Magic Tricks", "Juggling",
        "Stand-up Comedy", "Poetry", "Storytelling", "Public Speaking", "Debating", "Research",
        "History", "Science", "Technology", "Art", "Architecture", "Design", "Fashion",
        "Beauty", "Health", "Wellness", "Nutrition", "Fitness", "Sports", "Outdoor Activities",
        "Indoor Activities", "Social Activities", "Cultural Activities", "Educational Activities"
    )

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
        
        // Set up RecyclerViews
        setupRecyclerViews()
        
        // Set up skills and hobbies input
        setupSkillsInput()
        setupHobbiesInput()

        // Set up back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Set up button click listeners
        setupClickListeners()

        // Load user data
        loadUserData()
    }

    private fun initializeViews() {
        nameInput = findViewById(R.id.nameInput)
        surnameInput = findViewById(R.id.surnameInput)
        emailInput = findViewById(R.id.emailInput)
        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        addressInput = findViewById(R.id.addressInput)
        summaryInput = findViewById(R.id.summaryInput)
        skillsInput = findViewById(R.id.skillsInput)
        hobbiesInput = findViewById(R.id.hobbiesInput)
        achievementsInput = findViewById(R.id.achievementsInput)
        linkedinInput = findViewById(R.id.linkedinInput)
        githubInput = findViewById(R.id.githubInput)
        portfolioInput = findViewById(R.id.portfolioInput)
        profileImage = findViewById(R.id.profileImage)
        changePhotoButton = findViewById(R.id.changePhotoButton)
        addEducationButton = findViewById(R.id.addEducationButton)
        addExperienceButton = findViewById(R.id.addExperienceButton)
        saveButton = findViewById(R.id.saveButton)
        skillsChipGroup = findViewById(R.id.skillsChipGroup)
        hobbiesChipGroup = findViewById(R.id.hobbiesChipGroup)
    }

    private fun setupRecyclerViews() {
        educationRecyclerView = findViewById(R.id.educationRecyclerView)
        experienceRecyclerView = findViewById(R.id.experienceRecyclerView)
        
        educationAdapter = EducationAdapter()
        experienceAdapter = ExperienceAdapter()
        
        educationRecyclerView.layoutManager = LinearLayoutManager(this)
        educationRecyclerView.adapter = educationAdapter
        
        experienceRecyclerView.layoutManager = LinearLayoutManager(this)
        experienceRecyclerView.adapter = experienceAdapter
        
        // Setup References RecyclerView
        referencesRecyclerView = findViewById(R.id.referencesRecyclerView)
        referenceAdapter = ReferenceAdapter()
        referencesRecyclerView.layoutManager = LinearLayoutManager(this)
        referencesRecyclerView.adapter = referenceAdapter
        
        addReferenceButton = findViewById(R.id.addReferenceButton)
        addReferenceButton.setOnClickListener {
            referenceAdapter.addNewReference()
        }
    }

    private fun setupSkillsInput() {
        val skillsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, commonSkills)
        skillsInput.setAdapter(skillsAdapter)
        
        skillsInput.setOnItemClickListener { _, _, position, _ ->
            val selectedSkill = commonSkills[position]
            addSkillChip(selectedSkill)
            skillsInput.text?.clear()
        }
        
        skillsInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
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

    private fun setupHobbiesInput() {
        val hobbiesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, commonHobbies)
        hobbiesInput.setAdapter(hobbiesAdapter)
        
        hobbiesInput.setOnItemClickListener { _, _, position, _ ->
            val selectedHobby = commonHobbies[position]
            addHobbyChip(selectedHobby)
            hobbiesInput.text?.clear()
        }
        
        hobbiesInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val hobby = hobbiesInput.text.toString().trim()
                if (hobby.isNotEmpty()) {
                    addHobbyChip(hobby)
                    hobbiesInput.text?.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun addSkillChip(skill: String) {
        val chip = Chip(this)
        chip.text = skill
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            skillsChipGroup.removeView(chip)
        }
        skillsChipGroup.addView(chip)
    }

    private fun addHobbyChip(hobby: String) {
        val chip = Chip(this)
        chip.text = hobby
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            hobbiesChipGroup.removeView(chip)
        }
        hobbiesChipGroup.addView(chip)
    }

    private fun setupClickListeners() {
        changePhotoButton.setOnClickListener {
            checkPermissionAndPickImage()
        }

        addEducationButton.setOnClickListener {
            educationAdapter.addNewEducation()
        }

        addExperienceButton.setOnClickListener {
            experienceAdapter.addNewExperience()
        }

        saveButton.setOnClickListener {
            saveProfile()
        }
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

    private fun loadUserData() {
        Log.d("ProfileActivity", "Starting to load user data")
        
        // Show loading state
        nameInput.setText("Loading...")
        surnameInput.setText("Loading...")
        emailInput.setText("Loading...")
        phoneNumberInput.setText("Loading...")
        addressInput.setText("Loading...")
        summaryInput.setText("Loading...")
        skillsInput.setText("Loading...")
        achievementsInput.setText("Loading...")
        linkedinInput.setText("Loading...")
        githubInput.setText("Loading...")
        portfolioInput.setText("Loading...")

        // Check if userId was passed in intent
        val userId = intent.getStringExtra("userId")
        if (!userId.isNullOrEmpty()) {
            Log.d("ProfileActivity", "Loading user by ID: $userId")
            loadUserById(userId)
            return
        }

        // Get current user's email
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val email = currentUser.email
            Log.d("ProfileActivity", "Current user email: $email")
            if (email != null) {
                loadUserByEmail(email)
            } else {
                Log.e("ProfileActivity", "Current user email is null")
                Toast.makeText(this, "Error: User email not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("ProfileActivity", "No current user found")
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserById(userId: String) {
        Log.d("ProfileActivity", "Loading user by ID: $userId")
        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        Log.d("ProfileActivity", "User found by ID: ${user.name} ${user.surname}")
                        this.currentUser = user
                        updateUIWithUserData(user)
                    } else {
                        Log.e("ProfileActivity", "User document exists but could not be converted to User object")
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("ProfileActivity", "No user found with ID: $userId")
                    Toast.makeText(this, "No user data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error loading user by ID", e)
                Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserByEmail(email: String) {
        Log.d("ProfileActivity", "Loading user by email: $email")
        db.collection("Users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("ProfileActivity", "No user found by email: $email")
                    Toast.makeText(this, "No user data found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                Log.d("ProfileActivity", "User found by email, document count: ${documents.size()}")
                for (document in documents) {
                    val user = document.toObject(User::class.java)
                    Log.d("ProfileActivity", "Loaded user data: ${user.name} ${user.surname}")
                    Log.d("ProfileActivity", "Education count: ${user.education.size}")
                    Log.d("ProfileActivity", "Experience count: ${user.experience.size}")
                    
                    this.currentUser = user
                    
                    // Update UI with user data
                    updateUIWithUserData(user)
                    break // Just use the first matching user
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error loading user by email", e)
                Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUIWithUserData(user: User) {
        Log.d("ProfileActivity", "Updating UI with user data: ${user.name} ${user.surname}")
        
        nameInput.setText(user.name)
        surnameInput.setText(user.surname)
        emailInput.setText(user.email)
        phoneNumberInput.setText(user.phoneNumber)
        addressInput.setText(user.address)
        summaryInput.setText(user.summary)
        achievementsInput.setText(user.achievements)
        linkedinInput.setText(user.linkedin)
        githubInput.setText(user.github)
        portfolioInput.setText(user.portfolio)
        
        // Handle skills list
        skillsChipGroup.removeAllViews()
        user.skills.forEach { skill ->
            addSkillChip(skill)
        }

        // Handle hobbies list
        hobbiesChipGroup.removeAllViews()
        user.hobbies.forEach { hobby ->
            addHobbyChip(hobby)
        }

        // Load profile image if exists
        user.profileImageUrl?.let { url ->
            Log.d("ProfileActivity", "Loading profile image from URL: $url")
            Glide.with(this)
                .load(url)
                .transform(CircleCrop())
                .into(profileImage)
        } ?: run {
            Log.d("ProfileActivity", "No profile image URL available")
        }
        
        // Update education and experience lists
        Log.d("ProfileActivity", "Updating education list with ${user.education.size} items")
        educationAdapter.clearEducationList()
        user.education.forEach { education ->
            Log.d("ProfileActivity", "Adding education: ${education.institution} - ${education.degree}")
            val adapterEducation = EducationAdapter.Education(
                institution = education.institution,
                degree = education.degree,
                startDate = education.startDate,
                endDate = education.endDate
            )
            educationAdapter.addEducation(adapterEducation)
        }
        
        Log.d("ProfileActivity", "Updating experience list with ${user.experience.size} items")
        experienceAdapter.clearExperienceList()
        user.experience.forEach { experience ->
            Log.d("ProfileActivity", "Adding experience: ${experience.company} - ${experience.position}")
            val adapterExperience = ExperienceAdapter.Experience(
                company = experience.company,
                position = experience.position,
                startDate = experience.startDate,
                endDate = experience.endDate,
                description = experience.description
            )
            experienceAdapter.addExperience(adapterExperience)
        }

        // Update references
        referenceAdapter.clearReferenceList()
        user.references.forEach { reference ->
            val adapterReference = ReferenceAdapter.Reference(
                name = reference.name,
                position = reference.position,
                company = reference.company,
                email = reference.email,
                phone = reference.phone
            )
            referenceAdapter.addReference(adapterReference)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        getContent.launch(intent)
    }

    private fun uploadImage(imageUri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val storageRef = storage.reference.child("profile_images/${currentUser.uid}")
            
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        updateProfileImage(downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateProfileImage(imageUrl: String) {
        val currentUser = auth.currentUser
        if (currentUser != null && this.currentUser != null) {
            val updatedUser = this.currentUser!!.copy(profileImageUrl = imageUrl)
            
            db.collection("Users")
                .document(this.currentUser!!.idNumber)
                .set(updatedUser)
                .addOnSuccessListener {
                    this.currentUser = updatedUser
                    Glide.with(this)
                        .load(imageUrl)
                        .transform(CircleCrop())
                        .into(profileImage)
                    Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null && this.currentUser != null) {
            // Get skills from chips
            val skills = mutableListOf<String>()
            for (i in 0 until skillsChipGroup.childCount) {
                val chip = skillsChipGroup.getChildAt(i) as? Chip
                chip?.text?.toString()?.let { skills.add(it) }
            }
            
            // Get hobbies from chips
            val hobbies = mutableListOf<String>()
            for (i in 0 until hobbiesChipGroup.childCount) {
                val chip = hobbiesChipGroup.getChildAt(i) as? Chip
                chip?.text?.toString()?.let { hobbies.add(it) }
            }
            
            // Get education and experience from adapters
            val educationList = educationAdapter.getEducationList().map { adapterEducation ->
                Education(
                    institution = adapterEducation.institution,
                    degree = adapterEducation.degree,
                    fieldOfStudy = "", // Not available in adapter
                    startDate = adapterEducation.startDate,
                    endDate = adapterEducation.endDate,
                    description = "" // Not available in adapter
                )
            }
            
            val experienceList = experienceAdapter.getExperienceList().map { adapterExperience ->
                Experience(
                    company = adapterExperience.company,
                    position = adapterExperience.position,
                    startDate = adapterExperience.startDate,
                    endDate = adapterExperience.endDate,
                    description = adapterExperience.description
                )
            }
            
            // Get references from adapter
            val references = referenceAdapter.getReferenceList().map { adapterReference ->
                Reference(
                    name = adapterReference.name,
                    position = adapterReference.position,
                    company = adapterReference.company,
                    email = adapterReference.email,
                    phone = adapterReference.phone
                )
            }
            
            val updatedUser = this.currentUser!!.copy(
                name = nameInput.text.toString().trim(),
                surname = surnameInput.text.toString().trim(),
                phoneNumber = phoneNumberInput.text.toString().trim(),
                address = addressInput.text.toString().trim(),
                summary = summaryInput.text.toString().trim(),
                skills = skills,
                hobbies = hobbies,
                education = educationList,
                experience = experienceList,
                achievements = achievementsInput.text.toString().trim(),
                linkedin = linkedinInput.text.toString().trim(),
                github = githubInput.text.toString().trim(),
                portfolio = portfolioInput.text.toString().trim(),
                references = references
            )
            
            db.collection("Users")
                .document(this.currentUser!!.idNumber)
                .set(updatedUser)
                .addOnSuccessListener {
                    this.currentUser = updatedUser
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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