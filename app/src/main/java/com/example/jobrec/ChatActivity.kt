package com.example.jobrec

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
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
    private lateinit var scheduleMeetingFab: FloatingActionButton

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

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        // Initialize repository
        conversationRepository = ConversationRepository()

        // Get conversation ID from intent
        conversationId = intent.getStringExtra("conversationId")
        applicationId = intent.getStringExtra("applicationId")

        if (conversationId == null && applicationId == null) {
            Toast.makeText(this, "Error: Missing conversation information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()

        // Load conversation data
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
        scheduleMeetingFab = findViewById(R.id.scheduleMeetingFab)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat"
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(
            onMeetingAccept = { message ->
                lifecycleScope.launch {
                    try {
                        conversationRepository.updateMeetingStatus(message.id, "accepted")
                        Toast.makeText(this@ChatActivity, "Meeting accepted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onMeetingDecline = { message ->
                lifecycleScope.launch {
                    try {
                        conversationRepository.updateMeetingStatus(message.id, "rejected")
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

        scheduleMeetingFab.setOnClickListener {
            showScheduleMeetingDialog()
        }
    }

    private fun loadConversationData() {
        lifecycleScope.launch {
            try {
                // If we have a conversation ID, load it directly
                if (conversationId != null) {
                    conversation = conversationRepository.getConversationById(conversationId!!)
                }
                // If we have an application ID, find or create the conversation
                else if (applicationId != null) {
                    conversation = conversationRepository.getConversationByApplicationId(applicationId!!)

                    if (conversation == null) {
                        // We need to create a new conversation
                        // First, get application details
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
                            val companyName = applicationDoc.getString("companyName") ?: ""

                            // Log the company ID from the application
                            android.util.Log.d("ChatActivity", "Company ID from application: $companyIdFromApp")

                            // Look up the company's Firebase Auth user ID
                            val companyDoc = db.collection("companies")
                                .whereEqualTo("companyId", companyIdFromApp)
                                .get()
                                .await()

                            // Get the company's user ID
                            val companyUserId = if (!companyDoc.isEmpty) {
                                val userId = companyDoc.documents[0].getString("userId")
                                android.util.Log.d("ChatActivity", "Found company user ID: $userId")
                                userId ?: companyIdFromApp
                            } else {
                                android.util.Log.d("ChatActivity", "Company not found, using original ID")
                                companyIdFromApp
                            }

                            // Create the conversation
                            conversationId = conversationRepository.createConversation(
                                applicationId = applicationId!!,
                                jobId = jobId,
                                jobTitle = jobTitle,
                                candidateId = candidateId,
                                candidateName = candidateName,
                                companyId = companyUserId,
                                companyName = companyName
                            )

                            // Load the newly created conversation
                            conversation = conversationRepository.getConversationById(conversationId!!)
                        }
                    } else {
                        conversationId = conversation?.id
                    }
                }

                // Update UI with conversation data
                if (conversation != null) {
                    updateConversationUI(conversation!!)
                    startMessagesListener()

                    // Mark conversation as read
                    conversationRepository.markConversationAsRead(conversationId!!)
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
        // Determine if current user is the candidate or the company
        val isCompanyView = currentUserId == conversation.companyId

        if (isCompanyView) {
            // Company viewing candidate
            jobTitleText.text = conversation.candidateName
            participantNameText.text = conversation.candidateName
            receiverId = conversation.candidateId
            // Use a person icon for candidates
            participantImageView.setImageResource(R.drawable.ic_person)
            // Show schedule meeting button for companies only
            scheduleMeetingFab.visibility = View.VISIBLE
        } else {
            // Candidate viewing company
            jobTitleText.text = conversation.companyName
            participantNameText.text = conversation.companyName
            receiverId = conversation.companyId
            // Use a building icon for companies
            participantImageView.setImageResource(R.drawable.ic_company_placeholder)
            // Hide schedule meeting button for students
            scheduleMeetingFab.visibility = View.GONE
        }
    }

    private fun startMessagesListener() {
        // Stop any existing listener
        messagesListener?.remove()

        // Start new listener
        messagesListener = db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val messages = snapshot.toObjects(Message::class.java)
                        .sortedBy { it.createdAt.seconds } // Sort in memory

                    messageAdapter.submitList(messages)
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                    emptyView.visibility = View.GONE

                    // Mark messages as read
                    lifecycleScope.launch {
                        try {
                            conversationRepository.markConversationAsRead(conversationId!!)
                        } catch (e: Exception) {
                            // Ignore errors here
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
                conversationRepository.sendMessage(conversationId!!, content, receiverId)
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

        // Initialize dialog views
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

        // Setup date picker
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

        // Setup time picker
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

        // Setup duration dropdown
        val durations = arrayOf("30 minutes", "45 minutes", "1 hour", "1.5 hours", "2 hours")
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, durations)
        durationInput.setAdapter(durationAdapter)
        durationInput.setText(durations[2], false) // Default to 1 hour

        // Setup meeting type dropdown
        val meetingTypes = arrayOf("Online", "In-person")
        val meetingTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, meetingTypes)
        meetingTypeInput.setAdapter(meetingTypeAdapter)
        meetingTypeInput.setText(meetingTypes[0], false) // Default to online

        // Show/hide location or link based on meeting type
        meetingTypeInput.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) { // Online
                locationInputLayout.visibility = View.GONE
                meetingLinkInputLayout.visibility = View.VISIBLE
            } else { // In-person
                locationInputLayout.visibility = View.VISIBLE
                meetingLinkInputLayout.visibility = View.GONE
            }
        }

        // Set button click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        scheduleButton.setOnClickListener {
            // Validate inputs
            val selectedDate = calendar.time
            val selectedTime = timeInput.text.toString()
            val selectedDuration = durationInput.text.toString()
            val selectedType = meetingTypeInput.text.toString().lowercase()
            val selectedLocation = locationInput.text.toString()
            val selectedLink = meetingLinkInput.text.toString()

            // Convert duration string to minutes
            val durationMinutes = when (selectedDuration) {
                "30 minutes" -> 30
                "45 minutes" -> 45
                "1 hour" -> 60
                "1.5 hours" -> 90
                "2 hours" -> 120
                else -> 60
            }

            // Validate required fields
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

            // Send meeting invitation
            lifecycleScope.launch {
                try {
                    conversationRepository.sendMeetingInvite(
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }
}
