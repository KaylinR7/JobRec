package com.example.jobrec
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import de.hdodenhof.circleimageview.CircleImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.adapters.MessageAdapter
import com.example.jobrec.models.Conversation
import com.example.jobrec.models.Message
import com.example.jobrec.repositories.ConversationRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
class ChatActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var jobTitleText: TextView
    private lateinit var participantNameText: TextView
    private lateinit var participantImageView: CircleImageView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var messageInput: TextInputEditText
    private lateinit var sendButton: FloatingActionButton
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var conversationId: String? = null
    private var applicationId: String? = null
    private var conversation: Conversation? = null
    private var messagesListener: ListenerRegistration? = null
    private var currentUserId: String = ""
    private var receiverId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        conversationRepository = ConversationRepository()
        conversationId = intent.getStringExtra("conversationId")
        applicationId = intent.getStringExtra("applicationId")
        if (conversationId == null && applicationId == null) {
            Toast.makeText(this, "Error: Missing conversation information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadConversationData()
    }
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        jobTitleText = findViewById(R.id.jobTitleText)
        participantNameText = findViewById(R.id.participantNameText)
        participantImageView = findViewById(R.id.participantImageView)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
    }
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Messages"
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
    }
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(
            onMeetingAccept = { message ->
                lifecycleScope.launch {
                    try {
                        val repo = ConversationRepository()
                        repo.updateMeetingStatus(message.id, "accepted")
                        Toast.makeText(this@ChatActivity, "Meeting accepted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onMeetingDecline = { message ->
                lifecycleScope.launch {
                    try {
                        val repo = ConversationRepository()
                        repo.updateMeetingStatus(message.id, "rejected")
                        Toast.makeText(this@ChatActivity, "Meeting declined", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }
    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val messageContent = messageInput.text.toString().trim()
            if (messageContent.isNotEmpty() && conversationId != null && receiverId.isNotEmpty()) {
                sendMessage(messageContent)
            }
        }
    }
    private fun loadConversationData() {
        lifecycleScope.launch {
            try {
                if (conversationId != null) {
                    conversation = conversationRepository.getConversationById(conversationId!!)
                }
                else if (applicationId != null) {
                    conversation = conversationRepository.getConversationByApplicationId(applicationId!!)
                    if (conversation == null) {
                        val applicationDoc = db.collection("applications")
                            .document(applicationId!!)
                            .get()
                            .await()
                        if (applicationDoc.exists()) {
                            val jobId = applicationDoc.getString("jobId") ?: ""
                            val jobTitle = applicationDoc.getString("jobTitle") ?: ""
                            val candidateId = applicationDoc.getString("userId") ?: ""
                            val candidateName = applicationDoc.getString("applicantName") ?: ""
                            val companyIdFromApp = applicationDoc.getString("companyId") ?: ""
                            var companyName = applicationDoc.getString("companyName") ?: ""
                            android.util.Log.d("ChatActivity", "Company ID from application: $companyIdFromApp")
                            android.util.Log.d("ChatActivity", "Company name from application: $companyName")
                            val companyDoc = db.collection("companies")
                                .whereEqualTo("companyId", companyIdFromApp)
                                .get()
                                .await()
                            val companyUserId: String
                            if (!companyDoc.isEmpty) {
                                val companyDocument = companyDoc.documents[0]
                                val userId = companyDocument.getString("userId")
                                android.util.Log.d("ChatActivity", "Found company user ID: $userId")
                                companyUserId = userId ?: companyIdFromApp
                                if (companyName.isEmpty()) {
                                    companyName = companyDocument.getString("companyName") ?: "Company"
                                    android.util.Log.d("ChatActivity", "Using company name from company document: $companyName")
                                }
                            } else {
                                android.util.Log.d("ChatActivity", "Company not found, using original ID")
                                companyUserId = companyIdFromApp
                                if (companyName.isEmpty()) {
                                    companyName = "Company"
                                    android.util.Log.d("ChatActivity", "Using default company name: $companyName")
                                }
                            }
                            var candidateInfo = ""
                            try {
                                val userDoc = db.collection("users")
                                    .document(candidateId)
                                    .get()
                                    .await()
                                if (userDoc.exists()) {
                                    val skills = userDoc.get("skills") as? List<String>
                                    val education = userDoc.get("education") as? List<Map<String, Any>>
                                    val province = userDoc.getString("province")
                                    val infoBuilder = StringBuilder()
                                    if (!province.isNullOrBlank()) {
                                        infoBuilder.append(province)
                                    }
                                    val highestEducation = education?.maxByOrNull {
                                        (it["degree"] as? String)?.length ?: 0
                                    }
                                    highestEducation?.let {
                                        val degree = it["degree"] as? String
                                        if (!degree.isNullOrBlank()) {
                                            if (infoBuilder.isNotEmpty()) infoBuilder.append(" • ")
                                            infoBuilder.append(degree)
                                        }
                                    }
                                    if (!skills.isNullOrEmpty() && skills.isNotEmpty()) {
                                        if (infoBuilder.isNotEmpty()) infoBuilder.append(" • ")
                                        infoBuilder.append("Skills: ${skills.take(3).joinToString(", ")}")
                                    }
                                    candidateInfo = infoBuilder.toString()
                                }
                            } catch (e: Exception) {
                                Log.e("ChatActivity", "Error getting candidate info", e)
                            }
                            conversationId = conversationRepository.createConversation(
                                applicationId = applicationId!!,
                                jobId = jobId,
                                jobTitle = jobTitle,
                                candidateId = candidateId,
                                candidateName = candidateName,
                                companyId = companyUserId,
                                companyName = companyName,
                                candidateInfo = candidateInfo
                            )
                            conversation = conversationRepository.getConversationById(conversationId!!)
                        }
                    } else {
                        conversationId = conversation?.id
                    }
                }
                if (conversation != null) {
                    updateConversationUI(conversation!!)
                    startMessagesListener()
                    val repo = ConversationRepository()
                    repo.markConversationAsRead(conversationId!!)
                } else {
                    Toast.makeText(this@ChatActivity, "Error: Conversation not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun updateConversationUI(conversation: Conversation) {
        checkIfCompanyUser { isCompanyUser ->
            findViewById<View>(R.id.chatHeader).visibility = View.GONE
            if (isCompanyUser) {
                receiverId = conversation.candidateId
                val candidateName = when {
                    conversation.candidateName.isNullOrEmpty() -> "Student"
                    conversation.candidateName == "!!!!!" -> "Student"
                    conversation.candidateName == "Company" -> "Student"
                    else -> conversation.candidateName
                }
                supportActionBar?.title = candidateName
                Log.d("ChatActivity", "Setting title to candidate name: $candidateName")
                if (conversation.candidateName.isNullOrEmpty() ||
                    conversation.candidateName == "!!!!!" ||
                    conversation.candidateName == "Company") {
                    lifecycleScope.launch {
                        try {
                            val userDoc = db.collection("users")
                                .document(conversation.candidateId)
                                .get()
                                .await()
                            if (userDoc.exists()) {
                                val name = userDoc.getString("name") ?: ""
                                val surname = userDoc.getString("surname") ?: ""
                                val fullName = when {
                                    name.isNotEmpty() && surname.isNotEmpty() -> "$name $surname"
                                    name.isNotEmpty() -> name
                                    surname.isNotEmpty() -> surname
                                    else -> "Student" 
                                }
                                if (fullName.isNotEmpty() && fullName != "Student") {
                                    db.collection("conversations")
                                        .document(conversationId!!)
                                        .update("candidateName", fullName)
                                        .await()
                                    runOnUiThread {
                                        supportActionBar?.title = fullName
                                        Log.d("ChatActivity", "Updated candidate name to: $fullName")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ChatActivity", "Error updating candidate name", e)
                        }
                    }
                }
            } else {
                receiverId = conversation.companyId
                getCompanyNameFromFirestore(conversation.companyId)
            }
        }
    }
    private fun checkIfCompanyUser(callback: (Boolean) -> Unit) {
        val currentUserEmail = auth.currentUser?.email
        if (currentUserEmail != null) {
            db.collection("companies")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Log.d("ChatActivity", "User is company (by email): true")
                        callback(true)
                        invalidateOptionsMenu() 
                    } else {
                        db.collection("companies")
                            .whereEqualTo("userId", currentUserId)
                            .get()
                            .addOnSuccessListener { userIdDocs ->
                                if (!userIdDocs.isEmpty) {
                                    Log.d("ChatActivity", "User is company (by userId): true")
                                    callback(true)
                                    invalidateOptionsMenu() 
                                } else {
                                    Log.d("ChatActivity", "User is not a company")
                                    callback(false)
                                    invalidateOptionsMenu() 
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ChatActivity", "Error checking company by userId", e)
                                callback(false)
                                invalidateOptionsMenu() 
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Error checking company by email", e)
                    callback(false)
                    invalidateOptionsMenu() 
                }
        } else {
            Log.d("ChatActivity", "No email available for current user, defaulting to student")
            callback(false)
            invalidateOptionsMenu() 
        }
    }
    private fun getCompanyNameFromFirestore(companyId: String) {
        db.collection("companies")
            .document(companyId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val companyName = document.getString("companyName")
                    if (!companyName.isNullOrEmpty()) {
                        supportActionBar?.title = companyName
                        Log.d("ChatActivity", "Set company name from direct document query: $companyName")
                        conversation?.let { conv ->
                            if (conv.companyName != companyName) {
                                conversation = conv.copy(companyName = companyName)
                                Log.d("ChatActivity", "Updated conversation company name in memory: $companyName")
                            }
                        }
                        return@addOnSuccessListener
                    }
                }
                db.collection("companies")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val companyName = documents.documents[0].getString("companyName")
                            if (!companyName.isNullOrEmpty()) {
                                supportActionBar?.title = companyName
                                Log.d("ChatActivity", "Set company name from query: $companyName")
                                conversation?.let { conv ->
                                    if (conv.companyName != companyName) {
                                        conversation = conv.copy(companyName = companyName)
                                        Log.d("ChatActivity", "Updated conversation company name in memory: $companyName")
                                    }
                                }
                                return@addOnSuccessListener
                            }
                        }
                        val fallbackName = if (conversation?.companyName.isNullOrEmpty()) "Company" else conversation?.companyName
                        supportActionBar?.title = fallbackName
                        Log.d("ChatActivity", "Using fallback company name: $fallbackName")
                    }
                    .addOnFailureListener { e ->
                        val fallbackName = if (conversation?.companyName.isNullOrEmpty()) "Company" else conversation?.companyName
                        supportActionBar?.title = fallbackName
                        Log.d("ChatActivity", "Error querying company, using fallback: $fallbackName", e)
                    }
            }
            .addOnFailureListener { e ->
                val fallbackName = if (conversation?.companyName.isNullOrEmpty()) "Company" else conversation?.companyName
                supportActionBar?.title = fallbackName
                Log.d("ChatActivity", "Error getting company document, using fallback: $fallbackName", e)
            }
    }
    private fun startMessagesListener() {
        messagesListener?.remove()
        messagesListener = db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val messages = snapshot.toObjects(Message::class.java)
                        .sortedBy { it.createdAt.seconds } 
                    messageAdapter.submitList(messages)
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                    emptyView.visibility = View.GONE
                    lifecycleScope.launch {
                        try {
                            val repo = ConversationRepository()
                            repo.markConversationAsRead(conversationId!!)
                        } catch (e: Exception) {
                        }
                    }
                } else {
                    messageAdapter.submitList(emptyList())
                    emptyView.visibility = View.VISIBLE
                }
            }
    }
    private fun sendMessage(content: String) {
        lifecycleScope.launch {
            try {
                val repo = ConversationRepository()
                repo.sendMessage(conversationId!!, content, receiverId)
                messageInput.text?.clear()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error sending message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showScheduleMeetingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_schedule_meeting, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        val durationInput = dialogView.findViewById<AutoCompleteTextView>(R.id.durationInput)
        val meetingTypeInput = dialogView.findViewById<AutoCompleteTextView>(R.id.meetingTypeInput)
        val locationInputLayout = dialogView.findViewById<TextInputLayout>(R.id.locationInputLayout)
        val locationInput = dialogView.findViewById<TextInputEditText>(R.id.locationInput)
        val meetingLinkInputLayout = dialogView.findViewById<TextInputLayout>(R.id.meetingLinkInputLayout)
        val meetingLinkInput = dialogView.findViewById<TextInputEditText>(R.id.meetingLinkInput)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val scheduleButton = dialogView.findViewById<Button>(R.id.scheduleButton)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        dateInput.setText(dateFormat.format(calendar.time))
        dateInput.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                dateInput.setText(dateFormat.format(calendar.time))
            }, year, month, day).show()
        }
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        timeInput.setText(timeFormat.format(calendar.time))
        timeInput.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                timeInput.setText(timeFormat.format(calendar.time))
            }, hour, minute, false).show()
        }
        val durations = arrayOf("30 minutes", "45 minutes", "1 hour", "1.5 hours", "2 hours")
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, durations)
        durationInput.setAdapter(durationAdapter)
        durationInput.setText(durations[2], false) 
        val meetingTypes = arrayOf("Online", "In-person")
        val meetingTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, meetingTypes)
        meetingTypeInput.setAdapter(meetingTypeAdapter)
        meetingTypeInput.setText(meetingTypes[0], false) 
        meetingTypeInput.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) { 
                locationInputLayout.visibility = View.GONE
                meetingLinkInputLayout.visibility = View.VISIBLE
            } else { 
                locationInputLayout.visibility = View.VISIBLE
                meetingLinkInputLayout.visibility = View.GONE
            }
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        scheduleButton.setOnClickListener {
            val selectedDate = calendar.time
            val selectedTime = timeInput.text.toString()
            val selectedDuration = durationInput.text.toString()
            val selectedType = meetingTypeInput.text.toString().lowercase()
            val selectedLocation = locationInput.text.toString()
            val selectedLink = meetingLinkInput.text.toString()
            val durationMinutes = when (selectedDuration) {
                "30 minutes" -> 30
                "45 minutes" -> 45
                "1 hour" -> 60
                "1.5 hours" -> 90
                "2 hours" -> 120
                else -> 60
            }
            if (selectedDate.before(Date())) {
                Toast.makeText(this, "Please select a future date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedType == "in-person" && selectedLocation.isEmpty()) {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedType == "online" && selectedLink.isEmpty()) {
                Toast.makeText(this, "Please enter a meeting link", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val repo = ConversationRepository()
                    repo.sendMeetingInvite(
                        conversationId = conversationId!!,
                        receiverId = receiverId,
                        date = Timestamp(selectedDate),
                        time = selectedTime,
                        duration = durationMinutes,
                        type = selectedType,
                        location = if (selectedType == "in-person") selectedLocation else null,
                        meetingLink = if (selectedType == "online") selectedLink else null
                    )
                    dialog.dismiss()
                    Toast.makeText(this@ChatActivity, "Meeting invitation sent", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }
    private var isCompanyUser = false
    private var menuInstance: Menu? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        menuInstance = menu
        menu.findItem(R.id.action_schedule_meeting)?.isVisible = false
        checkIfCompanyUser { isCompany ->
            isCompanyUser = isCompany
            runOnUiThread {
                menuInstance?.findItem(R.id.action_schedule_meeting)?.isVisible = isCompany
            }
        }
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_schedule_meeting)?.isVisible = isCompanyUser
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_schedule_meeting -> {
                showScheduleMeetingDialog()
                true
            }
            R.id.action_delete_chat -> {
                showDeleteChatConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun showDeleteChatConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete this conversation? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteConversation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun deleteConversation() {
        if (conversationId == null) return
        lifecycleScope.launch {
            try {
                val messagesSnapshot = db.collection("messages")
                    .whereEqualTo("conversationId", conversationId)
                    .get()
                    .await()
                for (document in messagesSnapshot.documents) {
                    db.collection("messages").document(document.id).delete().await()
                }
                db.collection("conversations").document(conversationId!!).delete().await()
                Toast.makeText(this@ChatActivity, "Conversation deleted", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error deleting conversation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }
}
