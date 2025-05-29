package com.example.jobrec

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.jobrec.adapters.CalendarEventAdapter
import com.example.jobrec.adapters.UpcomingEventAdapter
import com.example.jobrec.models.CalendarEvent
import com.example.jobrec.models.Conversation
import com.example.jobrec.repositories.ConversationRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class CompanyCalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var upcomingInterviewsRecyclerView: RecyclerView
    private lateinit var tomorrowInterviewsRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var addEventFab: FloatingActionButton
    private lateinit var noEventsText: TextView
    private lateinit var noUpcomingInterviewsText: TextView
    private lateinit var noTomorrowInterviewsText: TextView
    private lateinit var tomorrowInterviewsHeader: TextView

    private lateinit var eventAdapter: CalendarEventAdapter
    private lateinit var upcomingInterviewsAdapter: UpcomingEventAdapter
    private lateinit var tomorrowInterviewsAdapter: CalendarEventAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var conversationRepository: ConversationRepository

    private var selectedDate: Date = Date()
    private var companyId: String = ""
    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    companion object {
        private const val TAG = "CompanyCalendarActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "CompanyCalendarActivity onCreate called")

        try {
            setContentView(R.layout.activity_company_calendar)
            Log.d(TAG, "Layout set successfully")

            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            conversationRepository = ConversationRepository()
            Log.d(TAG, "Firebase initialized")

            // Get company ID
            companyId = intent.getStringExtra("companyId") ?: auth.currentUser?.uid ?: ""
            Log.d(TAG, "Company ID: $companyId")

            // Initialize views
            initViews()
            Log.d(TAG, "Views initialized")

            setupToolbar()
            setupCalendarView()
            setupRecyclerView()
            setupSwipeRefresh()
            setupAddEventFab()
            Log.d(TAG, "Setup completed")

            // Load events for today
            loadEventsForDate(selectedDate)
            Log.d(TAG, "Initial data loading started")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing calendar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        calendarView = findViewById(R.id.calendarView)
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView)
        upcomingInterviewsRecyclerView = findViewById(R.id.upcomingInterviewsRecyclerView)
        tomorrowInterviewsRecyclerView = findViewById(R.id.tomorrowInterviewsRecyclerView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        addEventFab = findViewById(R.id.addEventFab)
        noEventsText = findViewById(R.id.noEventsText)
        noUpcomingInterviewsText = findViewById(R.id.noUpcomingInterviewsText)
        noTomorrowInterviewsText = findViewById(R.id.noTomorrowInterviewsText)
        tomorrowInterviewsHeader = findViewById(R.id.tomorrowInterviewsHeader)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Company Calendar"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupCalendarView() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            loadEventsForDate(selectedDate)
        }

        // Load upcoming interviews and tomorrow's interviews initially
        loadUpcomingInterviews()
        loadTomorrowInterviews()
    }

    private fun setupRecyclerView() {
        eventAdapter = CalendarEventAdapter(
            onEventClick = { event -> showEventDetails(event) },
            onEventEdit = { event -> showEditEventDialog(event) },
            onEventDelete = { event -> deleteEvent(event) }
        )

        upcomingInterviewsAdapter = UpcomingEventAdapter(
            onEventClick = { event -> showEventDetails(event) },
            onEventEdit = { event -> showEditEventDialog(event) },
            onEventDelete = { event -> deleteEvent(event) }
        )

        tomorrowInterviewsAdapter = CalendarEventAdapter(
            onEventClick = { event -> showEventDetails(event) },
            onEventEdit = { event -> showEditEventDialog(event) },
            onEventDelete = { event -> deleteEvent(event) }
        )

        eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CompanyCalendarActivity)
            adapter = eventAdapter
        }

        upcomingInterviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CompanyCalendarActivity)
            adapter = upcomingInterviewsAdapter
        }

        tomorrowInterviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CompanyCalendarActivity)
            adapter = tomorrowInterviewsAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadEventsForDate(selectedDate)
            loadUpcomingInterviews()
            loadTomorrowInterviews()
        }
    }

    private fun setupAddEventFab() {
        addEventFab.setOnClickListener {
            showEventTypeSelectionDialog()
        }
    }

    private fun showEventTypeSelectionDialog() {
        val options = arrayOf("Add Company Event", "Schedule Meeting with Student")
        AlertDialog.Builder(this)
            .setTitle("What would you like to create?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddEventDialog() // Regular company event
                    1 -> showScheduleMeetingDialog() // Meeting invitation to student
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadEventsForDate(date: Date) {
        lifecycleScope.launch {
            try {
                swipeRefreshLayout.isRefreshing = true

                // Create start and end of day timestamps for filtering
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = calendar.time

                // Query for company events and interviews
                val eventsSnapshot = db.collection("calendar_events")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                // Filter events for the selected date on client side
                val events = eventsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(CalendarEvent::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    val eventDate = event.date.toDate()
                    eventDate.time >= startOfDay.time && eventDate.time <= endOfDay.time
                }.sortedWith(compareBy<CalendarEvent> { it.date.toDate() }.thenBy { it.time })

                eventAdapter.submitList(events)

                // Show/hide no events message
                if (events.isEmpty()) {
                    noEventsText.visibility = View.VISIBLE
                    eventsRecyclerView.visibility = View.GONE
                } else {
                    noEventsText.visibility = View.GONE
                    eventsRecyclerView.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading events", e)
                val errorMessage = when {
                    e.message?.contains("index", ignoreCase = true) == true ->
                        "Setting up calendar... Please try again in a moment."
                    e.message?.contains("permission", ignoreCase = true) == true ->
                        "Permission denied. Please check your account."
                    else -> "Unable to load events. Please check your connection."
                }
                Toast.makeText(this@CompanyCalendarActivity, errorMessage, Toast.LENGTH_SHORT).show()

                // Show empty state
                eventAdapter.submitList(emptyList())
                noEventsText.visibility = View.VISIBLE
                eventsRecyclerView.visibility = View.GONE
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadUpcomingInterviews() {
        lifecycleScope.launch {
            try {
                // Get current date and time
                val now = Date()

                // Query for company interviews and events
                val eventsSnapshot = db.collection("calendar_events")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                // Filter for upcoming interviews and events (from now onwards) and limit to next 10
                val upcomingInterviews = eventsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(CalendarEvent::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    val eventDate = event.date.toDate()
                    eventDate.time >= now.time
                }.sortedWith(compareBy<CalendarEvent> { it.date.toDate() }.thenBy { it.time })
                .take(10) // Limit to next 10 upcoming interviews/events

                upcomingInterviewsAdapter.submitList(upcomingInterviews)

                // Show/hide no upcoming interviews message
                if (upcomingInterviews.isEmpty()) {
                    noUpcomingInterviewsText.visibility = View.VISIBLE
                    upcomingInterviewsRecyclerView.visibility = View.GONE
                } else {
                    noUpcomingInterviewsText.visibility = View.GONE
                    upcomingInterviewsRecyclerView.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading upcoming interviews", e)
                // Show empty state for upcoming interviews
                upcomingInterviewsAdapter.submitList(emptyList())
                noUpcomingInterviewsText.visibility = View.VISIBLE
                upcomingInterviewsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun loadTomorrowInterviews() {
        lifecycleScope.launch {
            try {
                // Calculate tomorrow's date range
                val tomorrow = Calendar.getInstance()
                tomorrow.add(Calendar.DAY_OF_MONTH, 1)
                tomorrow.set(Calendar.HOUR_OF_DAY, 0)
                tomorrow.set(Calendar.MINUTE, 0)
                tomorrow.set(Calendar.SECOND, 0)
                tomorrow.set(Calendar.MILLISECOND, 0)
                val startOfTomorrow = tomorrow.time

                tomorrow.set(Calendar.HOUR_OF_DAY, 23)
                tomorrow.set(Calendar.MINUTE, 59)
                tomorrow.set(Calendar.SECOND, 59)
                tomorrow.set(Calendar.MILLISECOND, 999)
                val endOfTomorrow = tomorrow.time

                // Query for company interviews and events
                val eventsSnapshot = db.collection("calendar_events")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                // Filter for tomorrow's interviews and events
                val tomorrowInterviews = eventsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(CalendarEvent::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    val eventDate = event.date.toDate()
                    eventDate.time >= startOfTomorrow.time && eventDate.time <= endOfTomorrow.time
                }.sortedWith(compareBy<CalendarEvent> { it.date.toDate() }.thenBy { it.time })

                tomorrowInterviewsAdapter.submitList(tomorrowInterviews)

                // Update header with special indicators
                updateTomorrowInterviewsHeader(tomorrowInterviews)

                // Show/hide no tomorrow interviews message
                if (tomorrowInterviews.isEmpty()) {
                    noTomorrowInterviewsText.visibility = View.VISIBLE
                    tomorrowInterviewsRecyclerView.visibility = View.GONE
                } else {
                    noTomorrowInterviewsText.visibility = View.GONE
                    tomorrowInterviewsRecyclerView.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading tomorrow's interviews", e)
                // Show empty state for tomorrow's interviews
                tomorrowInterviewsAdapter.submitList(emptyList())
                noTomorrowInterviewsText.visibility = View.VISIBLE
                tomorrowInterviewsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun updateTomorrowInterviewsHeader(events: List<CalendarEvent>) {
        val interviewCount = events.count { it.isInterview }
        val totalCount = events.size

        val headerText = when {
            totalCount == 0 -> "Tomorrow's Schedule"
            interviewCount > 0 -> "Tomorrow's Schedule ($totalCount) • $interviewCount Interview${if (interviewCount > 1) "s" else ""}"
            else -> "Tomorrow's Schedule ($totalCount)"
        }

        tomorrowInterviewsHeader.text = headerText

        // Change text color if there are interviews
        val textColor = if (interviewCount > 0) {
            getColor(R.color.status_interviewing)
        } else {
            getColor(R.color.text_primary)
        }
        tomorrowInterviewsHeader.setTextColor(textColor)
    }

    private fun showAddEventDialog() {
        showEventDialog(null)
    }

    private fun showEditEventDialog(event: CalendarEvent) {
        showEventDialog(event)
    }

    private fun showEventDialog(existingEvent: CalendarEvent?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(if (existingEvent == null) "Add Company Event" else "Edit Event")
            .create()

        // Initialize dialog views
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        val durationSpinner = dialogView.findViewById<Spinner>(R.id.durationSpinner)
        val meetingTypeSpinner = dialogView.findViewById<Spinner>(R.id.meetingTypeSpinner)
        val locationInput = dialogView.findViewById<TextInputEditText>(R.id.locationInput)
        val meetingLinkInput = dialogView.findViewById<TextInputEditText>(R.id.meetingLinkInput)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.notesInput)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        // Setup spinners
        setupDurationSpinner(durationSpinner)
        setupMeetingTypeSpinner(meetingTypeSpinner, locationInput, meetingLinkInput)

        // Pre-fill data if editing
        val eventCalendar = Calendar.getInstance()
        if (existingEvent != null) {
            titleInput.setText(existingEvent.title)
            descriptionInput.setText(existingEvent.description)
            eventCalendar.time = existingEvent.date.toDate()
            timeInput.setText(existingEvent.time)
            notesInput.setText(existingEvent.notes)
            locationInput.setText(existingEvent.location ?: "")
            meetingLinkInput.setText(existingEvent.meetingLink ?: "")
        } else {
            eventCalendar.time = selectedDate
        }

        dateInput.setText(dateFormat.format(eventCalendar.time))
        if (existingEvent == null) {
            timeInput.setText(timeFormat.format(eventCalendar.time))
        }

        // Setup date picker
        dateInput.setOnClickListener {
            val year = eventCalendar.get(Calendar.YEAR)
            val month = eventCalendar.get(Calendar.MONTH)
            val day = eventCalendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                eventCalendar.set(selectedYear, selectedMonth, selectedDay)
                dateInput.setText(dateFormat.format(eventCalendar.time))
            }, year, month, day).show()
        }

        // Setup time picker
        timeInput.setOnClickListener {
            val hour = eventCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = eventCalendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                eventCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                eventCalendar.set(Calendar.MINUTE, selectedMinute)
                timeInput.setText(timeFormat.format(eventCalendar.time))
            }, hour, minute, false).show()
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            saveCompanyEvent(dialog, existingEvent, titleInput, descriptionInput, eventCalendar,
                     timeInput, durationSpinner, meetingTypeSpinner, locationInput,
                     meetingLinkInput, notesInput)
        }

        dialog.show()
    }

    private fun setupDurationSpinner(spinner: Spinner) {
        val durations = arrayOf("30 minutes", "45 minutes", "1 hour", "1.5 hours", "2 hours")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(2) // Default to 1 hour
    }

    private fun setupMeetingTypeSpinner(spinner: Spinner, locationInput: TextInputEditText, meetingLinkInput: TextInputEditText) {
        val types = arrayOf("Online", "In-Person")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> { // Online
                        locationInput.visibility = View.GONE
                        meetingLinkInput.visibility = View.VISIBLE
                    }
                    1 -> { // In-Person
                        locationInput.visibility = View.VISIBLE
                        meetingLinkInput.visibility = View.GONE
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun saveCompanyEvent(
        dialog: AlertDialog,
        existingEvent: CalendarEvent?,
        titleInput: TextInputEditText,
        descriptionInput: TextInputEditText,
        eventCalendar: Calendar,
        timeInput: TextInputEditText,
        durationSpinner: Spinner,
        meetingTypeSpinner: Spinner,
        locationInput: TextInputEditText,
        meetingLinkInput: TextInputEditText,
        notesInput: TextInputEditText
    ) {
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val time = timeInput.text.toString().trim()
        val notes = notesInput.text.toString().trim()

        if (title.isEmpty()) {
            titleInput.error = "Title is required"
            return
        }

        if (time.isEmpty()) {
            timeInput.error = "Time is required"
            return
        }

        // Get duration in minutes
        val durationText = durationSpinner.selectedItem.toString()
        val duration = when (durationText) {
            "30 minutes" -> 30
            "45 minutes" -> 45
            "1 hour" -> 60
            "1.5 hours" -> 90
            "2 hours" -> 120
            else -> 60
        }

        // Get meeting type and location/link
        val meetingType = if (meetingTypeSpinner.selectedItemPosition == 0) "online" else "in-person"
        val location = if (meetingType == "in-person") locationInput.text.toString().trim() else null
        val meetingLink = if (meetingType == "online") meetingLinkInput.text.toString().trim() else null

        val event = if (existingEvent != null) {
            existingEvent.copy(
                title = title,
                description = description,
                date = Timestamp(eventCalendar.time),
                time = time,
                duration = duration,
                meetingType = meetingType,
                location = location,
                meetingLink = meetingLink,
                notes = notes,
                updatedAt = Timestamp.now()
            )
        } else {
            CalendarEvent(
                companyId = companyId,
                title = title,
                description = description,
                date = Timestamp(eventCalendar.time),
                time = time,
                duration = duration,
                meetingType = meetingType,
                location = location,
                meetingLink = meetingLink,
                notes = notes
            )
        }

        lifecycleScope.launch {
            try {
                if (existingEvent != null) {
                    db.collection("calendar_events").document(existingEvent.id).set(event).await()
                    Toast.makeText(this@CompanyCalendarActivity, "Event updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    db.collection("calendar_events").add(event).await()
                    Toast.makeText(this@CompanyCalendarActivity, "Event added successfully", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
                loadEventsForDate(selectedDate)
                loadUpcomingInterviews()
                loadTomorrowInterviews()

            } catch (e: Exception) {
                Log.e(TAG, "Error saving event", e)
                Toast.makeText(this@CompanyCalendarActivity, "Error saving event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEventDetails(event: CalendarEvent) {
        val message = buildString {
            append("Title: ${event.title}\n\n")
            if (event.description.isNotEmpty()) {
                append("Description: ${event.description}\n\n")
            }
            append("Date: ${dateFormat.format(event.date.toDate())}\n")
            append("Time: ${event.time}\n")
            append("Duration: ${event.duration} minutes\n")
            append("Type: ${event.meetingType.capitalize()}\n")

            if (event.meetingType == "in-person" && !event.location.isNullOrEmpty()) {
                append("Location: ${event.location}\n")
            } else if (event.meetingType == "online" && !event.meetingLink.isNullOrEmpty()) {
                append("Meeting Link: ${event.meetingLink}\n")
            }

            if (event.notes.isNotEmpty()) {
                append("\nNotes: ${event.notes}")
            }

            if (event.isInterview) {
                append("\n\n🎯 This is an interview appointment")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Event Details")
            .setMessage(message)
            .setPositiveButton("Edit") { _, _ -> showEditEventDialog(event) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun deleteEvent(event: CalendarEvent) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.collection("calendar_events").document(event.id).delete().await()
                        Toast.makeText(this@CompanyCalendarActivity, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                        loadEventsForDate(selectedDate)
                        loadUpcomingInterviews()
                        loadTomorrowInterviews()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting event", e)
                        Toast.makeText(this@CompanyCalendarActivity, "Error deleting event: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showScheduleMeetingDialog() {
        lifecycleScope.launch {
            try {
                // Get all conversations for this company to show student selection
                val conversations = conversationRepository.getUserConversations(companyId)

                if (conversations.isEmpty()) {
                    Toast.makeText(this@CompanyCalendarActivity, "No active conversations with students found. Start a conversation first.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Show student selection dialog
                showStudentSelectionDialog(conversations)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
                Toast.makeText(this@CompanyCalendarActivity, "Error loading student conversations: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showStudentSelectionDialog(conversations: List<Conversation>) {
        val studentNames = conversations.map { conversation ->
            val studentName = when {
                conversation.candidateName.isNullOrEmpty() -> "Student"
                conversation.candidateName == "!!!!!" -> "Student"
                conversation.candidateName == "Company" -> "Student"
                else -> conversation.candidateName
            }
            "$studentName (${conversation.jobTitle})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Student to Invite")
            .setItems(studentNames) { _, which ->
                val selectedConversation = conversations[which]
                showMeetingInvitationDialog(selectedConversation)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMeetingInvitationDialog(conversation: Conversation) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_schedule_meeting, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Schedule Meeting with ${conversation.candidateName}")
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
        calendar.time = selectedDate

        // Set default values
        dateInput.setText(dateFormat.format(calendar.time))
        timeInput.setText(timeFormat.format(calendar.time))

        // Setup date picker
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
        meetingTypeInput.setText(meetingTypes[0], false) // Default to Online

        // Handle meeting type selection
        meetingTypeInput.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) { // Online
                locationInputLayout.visibility = View.GONE
                meetingLinkInputLayout.visibility = View.VISIBLE
            } else { // In-person
                locationInputLayout.visibility = View.VISIBLE
                meetingLinkInputLayout.visibility = View.GONE
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        scheduleButton.setOnClickListener {
            sendMeetingInvitation(dialog, conversation, calendar, timeInput, durationInput, meetingTypeInput, locationInput, meetingLinkInput)
        }

        dialog.show()
    }

    private fun sendMeetingInvitation(
        dialog: AlertDialog,
        conversation: Conversation,
        calendar: Calendar,
        timeInput: TextInputEditText,
        durationInput: AutoCompleteTextView,
        meetingTypeInput: AutoCompleteTextView,
        locationInput: TextInputEditText,
        meetingLinkInput: TextInputEditText
    ) {
        val selectedDate = calendar.time
        val selectedTime = timeInput.text.toString()
        val selectedDuration = durationInput.text.toString()
        val selectedType = meetingTypeInput.text.toString().lowercase()
        val selectedLocation = locationInput.text.toString()
        val selectedLink = meetingLinkInput.text.toString()

        // Validation
        if (selectedDate.before(Date())) {
            Toast.makeText(this, "Please select a future date", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedType == "in-person" && selectedLocation.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedType == "online" && selectedLink.isEmpty()) {
            Toast.makeText(this, "Please enter a meeting link", Toast.LENGTH_SHORT).show()
            return
        }

        val durationMinutes = when (selectedDuration) {
            "30 minutes" -> 30
            "45 minutes" -> 45
            "1 hour" -> 60
            "1.5 hours" -> 90
            "2 hours" -> 120
            else -> 60
        }

        lifecycleScope.launch {
            try {
                // Send meeting invitation through chat
                val messageId = conversationRepository.sendMeetingInvite(
                    conversationId = conversation.id,
                    receiverId = conversation.candidateId,
                    date = Timestamp(selectedDate),
                    time = selectedTime,
                    duration = durationMinutes,
                    type = selectedType,
                    location = if (selectedType == "in-person") selectedLocation else null,
                    meetingLink = if (selectedType == "online") selectedLink else null
                )

                // Create pending calendar event for company
                createPendingCalendarEventForCompany(
                    companyId = companyId,
                    messageId = messageId,
                    conversation = conversation,
                    date = Timestamp(selectedDate),
                    time = selectedTime,
                    duration = durationMinutes,
                    meetingType = selectedType,
                    location = if (selectedType == "in-person") selectedLocation else null,
                    meetingLink = if (selectedType == "online") selectedLink else null
                )

                dialog.dismiss()
                Toast.makeText(this@CompanyCalendarActivity, "Meeting invitation sent to ${conversation.candidateName}", Toast.LENGTH_SHORT).show()

                // Refresh calendar views
                loadEventsForDate(selectedDate)
                loadUpcomingInterviews()
                loadTomorrowInterviews()

            } catch (e: Exception) {
                Log.e(TAG, "Error sending meeting invitation", e)
                Toast.makeText(this@CompanyCalendarActivity, "Error sending invitation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun createPendingCalendarEventForCompany(
        companyId: String,
        messageId: String,
        conversation: Conversation,
        date: Timestamp,
        time: String,
        duration: Int,
        meetingType: String,
        location: String?,
        meetingLink: String?
    ) {
        try {
            val eventTitle = "Interview - ${conversation.jobTitle} (Pending Response)"
            val eventDescription = "Interview with ${conversation.candidateName} for ${conversation.jobTitle} position - Waiting for student confirmation"

            val calendarEvent = CalendarEvent(
                companyId = companyId,
                title = eventTitle,
                description = eventDescription,
                date = date,
                time = time,
                duration = duration,
                meetingType = meetingType,
                location = location,
                meetingLink = meetingLink,
                notes = "Scheduled via CareerWorx messaging - Pending student acceptance",
                isInterview = true,
                jobId = conversation.jobId,
                status = "pending",
                invitationMessageId = messageId
            )

            db.collection("calendar_events").add(calendarEvent).await()
            Log.d(TAG, "Pending calendar event created for company: $companyId")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating pending calendar event for company", e)
            throw e
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}