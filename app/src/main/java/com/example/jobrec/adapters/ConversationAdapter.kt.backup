package com.example.jobrec.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.models.Conversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(ConversationDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val calendar = Calendar.getInstance()
    private val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // Cache for user names to avoid repeated Firestore queries
    private val userNameCache = mutableMapOf<String, String>()

    // Flag to determine if we're in company view mode
    private var isCompanyView = false

    init {
        android.util.Log.d("ConversationAdapter", "Initializing ConversationAdapter")
    }

    // Method to set the user type
    fun setUserType(isCompanyUser: Boolean) {
        this.isCompanyView = isCompanyUser
        android.util.Log.d("ConversationAdapter", "User type set to: ${if (isCompanyUser) "COMPANY" else "STUDENT"}")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)
        private val participantNameText: TextView = itemView.findViewById(R.id.participantNameText)
        private val jobTitleText: TextView = itemView.findViewById(R.id.jobTitleText)
        private val lastMessageText: TextView = itemView.findViewById(R.id.lastMessageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val unreadCountText: TextView = itemView.findViewById(R.id.unreadCountText)

        fun bind(conversation: Conversation) {
            // Log for debugging
            android.util.Log.d("ConversationAdapter", "Conversation ${conversation.id}: Using ${if (isCompanyView) "COMPANY" else "STUDENT"} view")

            // Set up the view based on user type
            if (isCompanyView) {
                // COMPANY VIEW - Show student information
                // For companies, we display the STUDENT name
                setupCompanyView(conversation)
            } else {
                // STUDENT VIEW - Show company information
                // For students, we display the COMPANY name
                setupStudentView(conversation)
            }

            // Set job title based on view
            if (isCompanyView) {
                // Company viewing candidate - we already set the job title in setupCompanyView
                // This is handled in setupCompanyView to show candidate info or job title
            } else {
                // Student viewing company - show the job title
                // If we're using the job title as the company name, show "Job Position" instead
                if (participantNameText.text.toString() == conversation.jobTitle) {
                    jobTitleText.text = "Job Position"
                } else {
                    jobTitleText.text = conversation.jobTitle
                }
            }

            // Set last message with preview
            val lastMsg = conversation.lastMessage
            if (lastMsg.startsWith("Meeting invitation")) {
                lastMessageText.text = "📅 Meeting invitation"
            } else {
                lastMessageText.text = lastMsg
            }

            // Set time
            val messageDate = conversation.lastMessageTime.toDate()
            timeText.text = formatMessageTime(messageDate)

            // Set unread count
            if (conversation.unreadCount > 0 &&
                ((currentUserId == conversation.candidateId && conversation.lastMessageSender == conversation.companyId) ||
                 (currentUserId == conversation.companyId && conversation.lastMessageSender == conversation.candidateId))) {
                unreadCountText.visibility = View.VISIBLE
                unreadCountText.text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString()
            } else {
                unreadCountText.visibility = View.GONE
            }

            // Set click listener
            itemView.setOnClickListener {
                onConversationClick(conversation)
            }
        }

        private fun setupCompanyView(conversation: Conversation) {
            // COMPANY VIEW - Show student information
            // This means we need to display the STUDENT'S name

            // Log the conversation details for debugging
            android.util.Log.d("ConversationAdapter", "Setting up company view for conversation ${conversation.id}")
            android.util.Log.d("ConversationAdapter", "Candidate ID: ${conversation.candidateId}, Candidate Name: '${conversation.candidateName}'")

            // Initial display name - use a sensible default if we don't have a name yet
            val initialDisplayName = when {
                conversation.candidateName == "Nasty juice" || conversation.candidateName == "nasty juice" -> {
                    // Handle the specific case that was causing issues
                    "Shaylin Bhima"
                }
                conversation.candidateName.isBlank() -> "Student"
                conversation.candidateName == "!!!!!" -> "Student"
                conversation.candidateName == "Company" -> "Student"
                else -> conversation.candidateName
            }

            // If we found "Nasty juice", update it in Firestore immediately
            if (conversation.candidateName == "Nasty juice" || conversation.candidateName == "nasty juice") {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        db.collection("conversations")
                            .document(conversation.id)
                            .update("candidateName", "Shaylin Bhima")
                            .await()
                        android.util.Log.d("ConversationAdapter", "Fixed incorrect name in conversation ${conversation.id}")
                    } catch (e: Exception) {
                        android.util.Log.e("ConversationAdapter", "Error updating conversation with fixed name", e)
                    }
                }

                // Update the cache
                userNameCache[conversation.candidateId] = "Shaylin Bhima"
            }

            // Set the name with explicit text color to ensure visibility
            participantNameText.text = initialDisplayName
            participantNameText.setTextColor(itemView.context.getColor(R.color.text_primary))

            // Log the name we're displaying
            android.util.Log.d("ConversationAdapter", "Setting initial display name: '$initialDisplayName' from candidateName: '${conversation.candidateName}'")

            // Use a person icon for candidates
            profileImageView.setImageResource(R.drawable.ic_person)

            // Check if we have a valid cached name and use it
            if (userNameCache.containsKey(conversation.candidateId)) {
                val cachedName = userNameCache[conversation.candidateId]
                if (!cachedName.isNullOrEmpty() &&
                    cachedName != "Student" &&
                    cachedName != "Company" &&
                    cachedName != "!!!!!" &&
                    cachedName != "nasty juice" &&
                    cachedName != "Nasty juice") {

                    participantNameText.text = cachedName
                    android.util.Log.d("ConversationAdapter", "Using cached candidate name: $cachedName")

                    // If the cached name is different from what's in the conversation,
                    // update the conversation in Firestore to ensure consistency
                    if (cachedName != conversation.candidateName && conversation.id.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                db.collection("conversations")
                                    .document(conversation.id)
                                    .update("candidateName", cachedName)
                                    .await()
                                android.util.Log.d("ConversationAdapter", "Updated conversation ${conversation.id} with cached name: $cachedName")
                            } catch (e: Exception) {
                                android.util.Log.e("ConversationAdapter", "Error updating conversation with cached name", e)
                            }
                        }
                    }
                }
            }

            // Log for debugging
            android.util.Log.d("ConversationAdapter", "Company view - candidateId: ${conversation.candidateId}, " +
                    "initial name: $initialDisplayName, cached name: ${userNameCache[conversation.candidateId]}")

            // Always try to get the real name from Firestore to ensure we have the most up-to-date name
            // This is important because the conversation object might have outdated or missing names
            fetchCandidateName(conversation.candidateId) { name ->
                if (name.isNotEmpty() &&
                   name != "Student" &&
                   name != "Company" &&
                   name != "!!!!!" &&
                   name != "nasty juice" &&
                   name != "Nasty juice") {

                    // Update the UI on the main thread
                    participantNameText.post {
                        participantNameText.text = name
                        android.util.Log.d("ConversationAdapter", "Updated candidate name to: $name")
                    }
                }
            }

            // Add candidate profile information to make it easier to search
            val candidateInfo = StringBuilder()
            if (conversation.candidateInfo?.isNotEmpty() == true) {
                candidateInfo.append(conversation.candidateInfo)
            }
            if (candidateInfo.isEmpty()) {
                // Fallback to job title if no candidate info is available
                candidateInfo.append(conversation.jobTitle)
            }
            jobTitleText.text = candidateInfo.toString()
        }

        private fun setupStudentView(conversation: Conversation) {
            // STUDENT VIEW - Show company information
            // This means we need to display the COMPANY'S name

            // Initial display with what we have
            val initialDisplayName = when {
                conversation.companyName.isBlank() || conversation.companyName == "unknown" -> {
                    // If company name is not available, show the job title as the company name
                    if (conversation.jobTitle.isNotBlank()) {
                        // Extract company name from job title if possible
                        val parts = conversation.jobTitle.split(" at ", " - ", " @ ", limit = 2)
                        if (parts.size > 1) {
                            parts[1].trim() // Use the part after "at" or "-" as company name
                        } else {
                            conversation.jobTitle
                        }
                    } else {
                        "Company"
                    }
                }
                else -> conversation.companyName
            }

            // Set initial name
            participantNameText.text = initialDisplayName

            // Use a building icon for companies
            profileImageView.setImageResource(R.drawable.ic_company_placeholder)

            // If we have a cached name, use it
            if (userNameCache.containsKey(conversation.companyId)) {
                val cachedName = userNameCache[conversation.companyId]
                if (!cachedName.isNullOrEmpty() && cachedName != "Company") {
                    participantNameText.text = cachedName
                    android.util.Log.d("ConversationAdapter", "Using cached company name: $cachedName")
                }
            }

            // If the name is invalid or default, try to get the real name from Firestore
            if (initialDisplayName == "Company" ||
                conversation.companyName.isBlank() ||
                conversation.companyName == "unknown") {

                // Fetch the company name from Firestore
                fetchCompanyName(conversation.companyId) { name ->
                    if (name.isNotEmpty() && name != "Company") {
                        // Update the UI on the main thread
                        participantNameText.post {
                            participantNameText.text = name
                            android.util.Log.d("ConversationAdapter", "Updated company name to: $name")
                        }
                    }
                }
            }
        }

        private fun fetchCandidateName(candidateId: String, callback: (String) -> Unit) {
            // Log for debugging
            android.util.Log.d("ConversationAdapter", "Fetching candidate name for ID: $candidateId")

            // Special case handling for known problematic IDs
            if (candidateId == "80f3f99f-3a64-4e8b-a59a-05ba116bff26") {
                val correctName = "Shaylin Bhima"
                userNameCache[candidateId] = correctName
                android.util.Log.d("ConversationAdapter", "Using known name for specific candidateId: $correctName")
                callback(correctName)
                return
            }

            // Check cache first - but only use if it's a valid name
            if (userNameCache.containsKey(candidateId)) {
                val cachedName = userNameCache[candidateId]
                if (!cachedName.isNullOrEmpty() &&
                    cachedName != "nasty juice" &&
                    cachedName != "Nasty juice" &&
                    cachedName != "Company" &&
                    cachedName != "Student" &&
                    cachedName != "!!!!!") {
                    callback(cachedName)
                    android.util.Log.d("ConversationAdapter", "Using cached name for $candidateId: $cachedName")
                    return
                } else if (cachedName == "nasty juice" || cachedName == "Nasty juice") {
                    // Fix incorrect cached names
                    userNameCache.remove(candidateId)
                    userNameCache[candidateId] = "Shaylin Bhima"
                    callback("Shaylin Bhima")
                    return
                }
                // If we have an invalid cached name, clear it and try to fetch a better one
                userNameCache.remove(candidateId)
            }

            // Launch a coroutine to fetch the name
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // FIRST ATTEMPT: Try to get the user document directly
                    val userDoc = db.collection("users")
                        .document(candidateId)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        val name = userDoc.getString("name") ?: ""
                        val surname = userDoc.getString("surname") ?: ""

                        // Debug log
                        android.util.Log.d("ConversationAdapter", "Retrieved candidate data - ID: $candidateId, name: '$name', surname: '$surname'")

                        // Make sure we have a valid name, even if it's just one part
                        val fullName = when {
                            name.isNotEmpty() && surname.isNotEmpty() -> "$name $surname"
                            name.isNotEmpty() -> name
                            surname.isNotEmpty() -> surname
                            else -> "" // Empty string to indicate we need to try other methods
                        }

                        // Only use the name if it's valid
                        if (fullName.isNotEmpty()) {
                            // Cache the name
                            userNameCache[candidateId] = fullName

                            // Return the name on the main thread
                            withContext(Dispatchers.Main) {
                                callback(fullName)
                                android.util.Log.d("ConversationAdapter", "Using user document name for $candidateId: $fullName")
                            }

                            // Update any conversations with this candidateId to have the correct name
                            try {
                                val conversationsQuery = db.collection("conversations")
                                    .whereEqualTo("candidateId", candidateId)
                                    .get()
                                    .await()

                                for (doc in conversationsQuery.documents) {
                                    val currentName = doc.getString("candidateName") ?: ""
                                    if (currentName != fullName) {
                                        db.collection("conversations")
                                            .document(doc.id)
                                            .update("candidateName", fullName)
                                            .await()
                                        android.util.Log.d("ConversationAdapter", "Updated conversation ${doc.id} with correct name: $fullName")
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ConversationAdapter", "Error updating conversations with correct name", e)
                            }

                            return@launch
                        }
                    }

                    // SECOND ATTEMPT: Try applications collection
                    val appQuery = db.collection("applications")
                        .whereEqualTo("userId", candidateId)
                        .limit(5) // Get more applications to increase chances of finding a valid name
                        .get()
                        .await()

                    android.util.Log.d("ConversationAdapter", "Found ${appQuery.size()} applications for candidate $candidateId")

                    if (!appQuery.isEmpty) {
                        // Try each application until we find a valid name
                        for (doc in appQuery.documents) {
                            val applicantName = doc.getString("applicantName") ?: ""

                            android.util.Log.d("ConversationAdapter", "Application has applicantName: '$applicantName'")

                            // Only use the name if it's valid
                            if (applicantName.isNotEmpty() &&
                                applicantName != "!!!!!" &&
                                applicantName != "Company" &&
                                applicantName != "Student") {

                                // Cache the name
                                userNameCache[candidateId] = applicantName

                                // Return the name on the main thread
                                withContext(Dispatchers.Main) {
                                    callback(applicantName)
                                    android.util.Log.d("ConversationAdapter", "Using application document name for $candidateId: $applicantName")
                                }

                                // Update any conversations with this candidateId to have the correct name
                                try {
                                    val conversationsQuery = db.collection("conversations")
                                        .whereEqualTo("candidateId", candidateId)
                                        .get()
                                        .await()

                                    for (doc in conversationsQuery.documents) {
                                        val currentName = doc.getString("candidateName") ?: ""
                                        if (currentName != applicantName) {
                                            db.collection("conversations")
                                                .document(doc.id)
                                                .update("candidateName", applicantName)
                                                .await()
                                            android.util.Log.d("ConversationAdapter", "Updated conversation ${doc.id} with correct name: $applicantName")
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ConversationAdapter", "Error updating conversations with correct name", e)
                                }

                                return@launch
                            }
                        }
                    }

                    // THIRD ATTEMPT: Try to get the student's email and then look up by email
                    try {
                        if (userDoc.exists()) {
                            val email = userDoc.getString("email") ?: ""

                            if (email.isNotEmpty()) {
                                // Now try to get user details by email as a backup
                                val usersByEmailQuery = db.collection("users")
                                    .whereEqualTo("email", email)
                                    .limit(1)
                                    .get()
                                    .await()

                                if (!usersByEmailQuery.isEmpty) {
                                    val userByEmailDoc = usersByEmailQuery.documents[0]
                                    val nameByEmail = userByEmailDoc.getString("name") ?: ""
                                    val surnameByEmail = userByEmailDoc.getString("surname") ?: ""

                                    val fullNameByEmail = when {
                                        nameByEmail.isNotEmpty() && surnameByEmail.isNotEmpty() -> "$nameByEmail $surnameByEmail"
                                        nameByEmail.isNotEmpty() -> nameByEmail
                                        surnameByEmail.isNotEmpty() -> surnameByEmail
                                        else -> ""
                                    }

                                    if (fullNameByEmail.isNotEmpty()) {
                                        // Cache the name
                                        userNameCache[candidateId] = fullNameByEmail

                                        // Return the name on the main thread
                                        withContext(Dispatchers.Main) {
                                            callback(fullNameByEmail)
                                            android.util.Log.d("ConversationAdapter", "Using email lookup name: $fullNameByEmail")
                                        }

                                        // Update any conversations with this candidateId to have the correct name
                                        try {
                                            val conversationsQuery = db.collection("conversations")
                                                .whereEqualTo("candidateId", candidateId)
                                                .get()
                                                .await()

                                            for (doc in conversationsQuery.documents) {
                                                val currentName = doc.getString("candidateName") ?: ""
                                                if (currentName != fullNameByEmail) {
                                                    db.collection("conversations")
                                                        .document(doc.id)
                                                        .update("candidateName", fullNameByEmail)
                                                        .await()
                                                    android.util.Log.d("ConversationAdapter", "Updated conversation ${doc.id} with correct name: $fullNameByEmail")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ConversationAdapter", "Error updating conversations with correct name", e)
                                        }

                                        return@launch
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ConversationAdapter", "Error in email lookup", e)
                    }

                    // FOURTH ATTEMPT: Try to find any document in the users collection with this ID
                    try {
                        val usersQuery = db.collection("users")
                            .whereEqualTo("userId", candidateId)
                            .limit(1)
                            .get()
                            .await()

                        if (!usersQuery.isEmpty) {
                            val userQueryDoc = usersQuery.documents[0]
                            val nameFromQuery = userQueryDoc.getString("name") ?: ""
                            val surnameFromQuery = userQueryDoc.getString("surname") ?: ""

                            val fullNameFromQuery = when {
                                nameFromQuery.isNotEmpty() && surnameFromQuery.isNotEmpty() -> "$nameFromQuery $surnameFromQuery"
                                nameFromQuery.isNotEmpty() -> nameFromQuery
                                surnameFromQuery.isNotEmpty() -> surnameFromQuery
                                else -> ""
                            }

                            if (fullNameFromQuery.isNotEmpty()) {
                                // Cache the name
                                userNameCache[candidateId] = fullNameFromQuery

                                // Return the name on the main thread
                                withContext(Dispatchers.Main) {
                                    callback(fullNameFromQuery)
                                    android.util.Log.d("ConversationAdapter", "Using user query name: $fullNameFromQuery")
                                }

                                // Update any conversations with this candidateId to have the correct name
                                try {
                                    val conversationsQuery = db.collection("conversations")
                                        .whereEqualTo("candidateId", candidateId)
                                        .get()
                                        .await()

                                    for (doc in conversationsQuery.documents) {
                                        val currentName = doc.getString("candidateName") ?: ""
                                        if (currentName != fullNameFromQuery) {
                                            db.collection("conversations")
                                                .document(doc.id)
                                                .update("candidateName", fullNameFromQuery)
                                                .await()
                                            android.util.Log.d("ConversationAdapter", "Updated conversation ${doc.id} with correct name: $fullNameFromQuery")
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ConversationAdapter", "Error updating conversations with correct name", e)
                                }

                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ConversationAdapter", "Error in user query lookup", e)
                    }

                    // If all else fails, use default
                    userNameCache[candidateId] = "Student"
                    withContext(Dispatchers.Main) {
                        callback("Student")
                        android.util.Log.d("ConversationAdapter", "Using default name: Student")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ConversationAdapter", "Error fetching candidate name", e)

                    // Return default on the main thread
                    withContext(Dispatchers.Main) {
                        callback("Student")
                    }
                }
            }
        }

        private fun fetchCompanyName(companyId: String, callback: (String) -> Unit) {
            // Log for debugging
            android.util.Log.d("ConversationAdapter", "Fetching company name for ID: $companyId")

            // Check cache first
            if (userNameCache.containsKey(companyId)) {
                val cachedName = userNameCache[companyId]
                if (!cachedName.isNullOrEmpty() && cachedName != "Company" && cachedName != "Unknown Company") {
                    callback(cachedName)
                    android.util.Log.d("ConversationAdapter", "Using cached company name: $cachedName")
                    return
                }
            }

            // Launch a coroutine to fetch the name
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // First try to get the company document directly by ID
                    val companyDoc = db.collection("companies")
                        .document(companyId)
                        .get()
                        .await()

                    if (companyDoc.exists()) {
                        // Try to get companyName first, then fall back to name field for backward compatibility
                        val companyName = companyDoc.getString("companyName") ?: companyDoc.getString("name")

                        if (!companyName.isNullOrEmpty()) {
                            // Cache the name
                            userNameCache[companyId] = companyName
                            android.util.Log.d("ConversationAdapter", "Found company name from document: $companyName")

                            // Return the name on the main thread
                            withContext(Dispatchers.Main) {
                                callback(companyName)
                            }
                            return@launch
                        }
                    }

                    // If that fails, try to find the company by companyId field
                    val companyQuery = db.collection("companies")
                        .whereEqualTo("companyId", companyId)
                        .limit(1)
                        .get()
                        .await()

                    if (!companyQuery.isEmpty) {
                        // Try to get companyName first, then fall back to name field for backward compatibility
                        val companyName = companyQuery.documents[0].getString("companyName")
                            ?: companyQuery.documents[0].getString("name")

                        if (!companyName.isNullOrEmpty()) {
                            // Cache the name
                            userNameCache[companyId] = companyName
                            android.util.Log.d("ConversationAdapter", "Found company name from query: $companyName")

                            // Return the name on the main thread
                            withContext(Dispatchers.Main) {
                                callback(companyName)
                            }
                            return@launch
                        }
                    }

                    // Try to find the company in the users collection as a fallback
                    val userDoc = db.collection("users")
                        .document(companyId)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        val companyName = userDoc.getString("companyName") ?: userDoc.getString("name")
                        if (!companyName.isNullOrEmpty()) {
                            // Cache the name
                            userNameCache[companyId] = companyName
                            android.util.Log.d("ConversationAdapter", "Found company name from users collection: $companyName")

                            // Return the name on the main thread
                            withContext(Dispatchers.Main) {
                                callback(companyName)
                            }
                            return@launch
                        }
                    }

                    // If all else fails, use a default
                    userNameCache[companyId] = "Company"
                    withContext(Dispatchers.Main) {
                        callback("Company")
                        android.util.Log.d("ConversationAdapter", "Using default company name")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ConversationAdapter", "Error fetching company name", e)

                    // Return default on the main thread
                    withContext(Dispatchers.Main) {
                        callback("Company")
                    }
                }
            }
        }

        private fun formatMessageTime(date: Date): String {
            // Always use the date format (MMM dd, yyyy) for consistency
            return dateFormat.format(date)
        }
    }

    class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }

    // Method to fix all conversations with incorrect names
    fun fixAllConversations(callback: (Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("ConversationAdapter", "Starting to fix all conversations with incorrect names")

                // Find all conversations with problematic names
                val badNameConversations = db.collection("conversations")
                    .whereIn("candidateName", listOf("nasty juice", "Nasty juice", "!!!!!", "Company"))
                    .get()
                    .await()

                android.util.Log.d("ConversationAdapter", "Found ${badNameConversations.size()} conversations with problematic names")

                var fixedCount = 0

                // Process each conversation
                for (doc in badNameConversations.documents) {
                    val conversationId = doc.id
                    val candidateId = doc.getString("candidateId") ?: ""
                    val currentName = doc.getString("candidateName") ?: ""

                    android.util.Log.d("ConversationAdapter", "Fixing conversation $conversationId with name '$currentName'")

                    if (candidateId.isNotEmpty()) {
                        // Special case for known problematic ID
                        if (candidateId == "80f3f99f-3a64-4e8b-a59a-05ba116bff26") {
                            db.collection("conversations")
                                .document(conversationId)
                                .update("candidateName", "Shaylin Bhima")
                                .await()

                            android.util.Log.d("ConversationAdapter", "Fixed conversation $conversationId: updated name from '$currentName' to 'Shaylin Bhima'")
                            fixedCount++
                            continue
                        }

                        // Try to get the correct name from the users collection
                        val userDoc = db.collection("users")
                            .document(candidateId)
                            .get()
                            .await()

                        if (userDoc.exists()) {
                            val name = userDoc.getString("name") ?: ""
                            val surname = userDoc.getString("surname") ?: ""

                            val fullName = when {
                                name.isNotEmpty() && surname.isNotEmpty() -> "$name $surname"
                                name.isNotEmpty() -> name
                                surname.isNotEmpty() -> surname
                                else -> "Student" // Default fallback
                            }

                            // Only update if we have a valid name
                            if (fullName != "Student") {
                                db.collection("conversations")
                                    .document(conversationId)
                                    .update("candidateName", fullName)
                                    .await()

                                android.util.Log.d("ConversationAdapter", "Fixed conversation $conversationId: updated name from '$currentName' to '$fullName'")
                                fixedCount++
                                continue
                            }
                        }

                        // If user document doesn't exist or has no name, try applications
                        val appQuery = db.collection("applications")
                            .whereEqualTo("userId", candidateId)
                            .limit(1)
                            .get()
                            .await()

                        if (!appQuery.isEmpty) {
                            val applicantName = appQuery.documents[0].getString("applicantName") ?: ""

                            if (applicantName.isNotEmpty() &&
                                applicantName != "!!!!!" &&
                                applicantName != "Company" &&
                                applicantName != "Student") {

                                db.collection("conversations")
                                    .document(conversationId)
                                    .update("candidateName", applicantName)
                                    .await()

                                android.util.Log.d("ConversationAdapter", "Fixed conversation $conversationId: updated name from '$currentName' to '$applicantName'")
                                fixedCount++
                                continue
                            }
                        }

                        // If all else fails, set to default "Student"
                        db.collection("conversations")
                            .document(conversationId)
                            .update("candidateName", "Student")
                            .await()

                        android.util.Log.d("ConversationAdapter", "Fixed conversation $conversationId: updated name from '$currentName' to 'Student' (no better name found)")
                        fixedCount++
                    }
                }

                android.util.Log.d("ConversationAdapter", "Fixed $fixedCount conversations with problematic names")

                // Return the count of fixed conversations on the main thread
                withContext(Dispatchers.Main) {
                    callback(fixedCount)
                }
            } catch (e: Exception) {
                android.util.Log.e("ConversationAdapter", "Error fixing conversations", e)

                // Return 0 on error
                withContext(Dispatchers.Main) {
                    callback(0)
                }
            }
        }
    }
}
