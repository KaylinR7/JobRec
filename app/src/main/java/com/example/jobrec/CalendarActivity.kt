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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var upcomingEventsRecyclerView: RecyclerView
    private lateinit var tomorrowEventsRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var addEventFab: FloatingActionButton
    private lateinit var noEventsText: TextView
    private lateinit var noUpcomingEventsText: TextView
    private lateinit var noTomorrowEventsText: TextView
    private lateinit var tomorrowEventsHeader: TextView

    private lateinit var eventAdapter: CalendarEventAdapter
    private lateinit var upcomingEventAdapter: UpcomingEventAdapter
    private lateinit var tomorrowEventAdapter: CalendarEventAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedDate: Date = Date()
    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    companion object {
        private const val TAG = "CalendarActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "CalendarActivity onCreate called")

        try {
            setContentView(R.layout.activity_calendar)
            Log.d(TAG, "Layout set successfully")

            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firebase initialized")

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
        upcomingEventsRecyclerView = findViewById(R.id.upcomingEventsRecyclerView)
        tomorrowEventsRecyclerView = findViewById(R.id.tomorrowEventsRecyclerView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        addEventFab = findViewById(R.id.addEventFab)
        noEventsText = findViewById(R.id.noEventsText)
        noUpcomingEventsText = findViewById(R.id.noUpcomingEventsText)
        noTomorrowEventsText = findViewById(R.id.noTomorrowEventsText)
        tomorrowEventsHeader = findViewById(R.id.tomorrowEventsHeader)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Calendar"
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

        // Load upcoming events and tomorrow's events initially
        loadUpcomingEvents()
        loadTomorrowEvents()
    }

    private fun setupRecyclerView() {
        eventAdapter = CalendarEventAdapter(
            onEventClick = { event -> showEventDetails(event) },
            onEventEdit = { event -> showEditEventDialog(event) },
            onEventDelete = { event -> deleteEvent(event) },
            allowEditDelete = false // Disable editing/deleting for students
        )

        upcomingEventAdapter = UpcomingEventAdapter(
            onEventClick = { event -> showEventDetails(event) },
            onEventEdit = { event -> showEditEventDialog(event) },
            onEventDelete = { event -> deleteEvent(event) },
            allowEditDelete = false // Disable editing/deleting for students
        )

        tomorrowEventAdapter = CalendarEventAdapter(
            onEventClick = { event -> showEventDetails(event) },
            onEventEdit = { event -> showEditEventDialog(event) },
            onEventDelete = { event -> deleteEvent(event) },
            allowEditDelete = false // Disable editing/deleting for students
        )

        eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CalendarActivity)
            adapter = eventAdapter
        }

        upcomingEventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CalendarActivity)
            adapter = upcomingEventAdapter
        }

        tomorrowEventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CalendarActivity)
            adapter = tomorrowEventAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadEventsForDate(selectedDate)
            loadUpcomingEvents()
            loadTomorrowEvents()
        }
    }

    private fun setupAddEventFab() {
        addEventFab.setOnClickListener {
            showAddEventDialog()
        }
    }

    private fun loadEventsForDate(date: Date) {
        val userId = auth.currentUser?.uid ?: return

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

                // Simple query to get all events for the user
                val eventsSnapshot = db.collection("calendar_events")
                    .whereEqualTo("userId", userId)
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
                // Show a more user-friendly error message
                val errorMessage = when {
                    e.message?.contains("index", ignoreCase = true) == true ->
                        "Setting up calendar... Please try again in a moment."
                    e.message?.contains("permission", ignoreCase = true) == true ->
                        "Permission denied. Please check your account."
                    else -> "Unable to load events. Please check your connection."
                }
                Toast.makeText(this@CalendarActivity, errorMessage, Toast.LENGTH_SHORT).show()

                // Show empty state
                eventAdapter.submitList(emptyList())
                noEventsText.visibility = View.VISIBLE
                eventsRecyclerView.visibility = View.GONE
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadUpcomingEvents() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                // Get current date and time
                val now = Date()

                // Simple query to get all events for the user
                val eventsSnapshot = db.collection("calendar_events")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                // Filter for upcoming events (from now onwards) and limit to next 10 events
                val upcomingEvents = eventsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(CalendarEvent::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    val eventDate = event.date.toDate()
                    eventDate.time >= now.time
                }.sortedWith(compareBy<CalendarEvent> { it.date.toDate() }.thenBy { it.time })
                .take(10) // Limit to next 10 upcoming events

                upcomingEventAdapter.submitList(upcomingEvents)

                // Show/hide no upcoming events message
                if (upcomingEvents.isEmpty()) {
                    noUpcomingEventsText.visibility = View.VISIBLE
                    upcomingEventsRecyclerView.visibility = View.GONE
                } else {
                    noUpcomingEventsText.visibility = View.GONE
                    upcomingEventsRecyclerView.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading upcoming events", e)
                // Show empty state for upcoming events
                upcomingEventAdapter.submitList(emptyList())
                noUpcomingEventsText.visibility = View.VISIBLE
                upcomingEventsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun loadTomorrowEvents() {
        val userId = auth.currentUser?.uid ?: return

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

                // Simple query to get all events for the user
                val eventsSnapshot = db.collection("calendar_events")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                // Filter for tomorrow's events
                val tomorrowEvents = eventsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(CalendarEvent::class.java)?.copy(id = doc.id)
                }.filter { event ->
                    val eventDate = event.date.toDate()
                    eventDate.time >= startOfTomorrow.time && eventDate.time <= endOfTomorrow.time
                }.sortedWith(compareBy<CalendarEvent> { it.date.toDate() }.thenBy { it.time })

                tomorrowEventAdapter.submitList(tomorrowEvents)

                // Update header with special indicators
                updateTomorrowEventsHeader(tomorrowEvents)

                // Show/hide no tomorrow events message
                if (tomorrowEvents.isEmpty()) {
                    noTomorrowEventsText.visibility = View.VISIBLE
                    tomorrowEventsRecyclerView.visibility = View.GONE
                } else {
                    noTomorrowEventsText.visibility = View.GONE
                    tomorrowEventsRecyclerView.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading tomorrow's events", e)
                // Show empty state for tomorrow's events
                tomorrowEventAdapter.submitList(emptyList())
                noTomorrowEventsText.visibility = View.VISIBLE
                tomorrowEventsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun updateTomorrowEventsHeader(events: List<CalendarEvent>) {
        val interviewCount = events.count { it.isInterview }
        val totalCount = events.size

        val headerText = when {
            totalCount == 0 -> "Tomorrow's Events"
            interviewCount > 0 -> "Tomorrow's Events ($totalCount) • $interviewCount Interview${if (interviewCount > 1) "s" else ""}"
            else -> "Tomorrow's Events ($totalCount)"
        }

        tomorrowEventsHeader.text = headerText

        // Change text color if there are interviews
        val textColor = if (interviewCount > 0) {
            getColor(R.color.status_interviewing)
        } else {
            getColor(R.color.text_primary)
        }
        tomorrowEventsHeader.setTextColor(textColor)
    }

    private fun showAddEventDialog() {
        showEventDialog(null)
    }

    private fun showEditEventDialog(event: CalendarEvent) {
        // Prevent editing of interview events
        if (event.isInterview) {
            Toast.makeText(this, "Interview appointments cannot be edited", Toast.LENGTH_SHORT).show()
            return
        }
        showEventDialog(event)
    }

    private fun showEventDialog(existingEvent: CalendarEvent?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(if (existingEvent == null) "Add Event" else "Edit Event")
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
            saveEvent(dialog, existingEvent, titleInput, descriptionInput, eventCalendar,
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

    private fun saveEvent(
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

        val userId = auth.currentUser?.uid ?: return

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
                userId = userId,
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
                    Toast.makeText(this@CalendarActivity, "Event updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    db.collection("calendar_events").add(event).await()
                    Toast.makeText(this@CalendarActivity, "Event added successfully", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
                loadEventsForDate(selectedDate)
                loadUpcomingEvents()
                loadTomorrowEvents()

            } catch (e: Exception) {
                Log.e(TAG, "Error saving event", e)
                Toast.makeText(this@CalendarActivity, "Error saving event: ${e.message}", Toast.LENGTH_SHORT).show()
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

            // Add interview information if applicable
            if (event.isInterview) {
                append("\n\n📅 This is an interview appointment")
                append("\n⚠️ Interview appointments cannot be edited or deleted")
            }
        }

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(if (event.isInterview) "Interview Details" else "Event Details")
            .setMessage(message)
            .setNegativeButton("Close", null)

        // Only show Edit button for non-interview events
        if (!event.isInterview) {
            dialogBuilder.setPositiveButton("Edit") { _, _ -> showEditEventDialog(event) }
        }

        dialogBuilder.show()
    }

    private fun deleteEvent(event: CalendarEvent) {
        // Prevent deletion of interview events
        if (event.isInterview) {
            Toast.makeText(this, "Interview appointments cannot be deleted", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.collection("calendar_events").document(event.id).delete().await()
                        Toast.makeText(this@CalendarActivity, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                        loadEventsForDate(selectedDate)
                        loadUpcomingEvents()
                        loadTomorrowEvents()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting event", e)
                        Toast.makeText(this@CalendarActivity, "Error deleting event: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
