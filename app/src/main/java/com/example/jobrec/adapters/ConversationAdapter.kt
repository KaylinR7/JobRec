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
    private val userNameCache = mutableMapOf<String, String>()
    private var isCompanyView = false
    init {
        android.util.Log.d("ConversationAdapter", "Initializing ConversationAdapter")
    }
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
            android.util.Log.d("ConversationAdapter", "Conversation ${conversation.id}: Using ${if (isCompanyView) "COMPANY" else "STUDENT"} view")
            if (isCompanyView) {
                setupCompanyView(conversation)
            } else {
                setupStudentView(conversation)
            }
            if (isCompanyView) {
            } else {
                if (participantNameText.text.toString() == conversation.jobTitle) {
                    jobTitleText.text = "Job Position"
                } else {
                    jobTitleText.text = conversation.jobTitle
                }
            }
            val lastMsg = conversation.lastMessage
            if (lastMsg.startsWith("Meeting invitation")) {
                lastMessageText.text = "📅 Meeting invitation"
            } else {
                lastMessageText.text = lastMsg
            }
            val messageDate = conversation.lastMessageTime.toDate()
            timeText.text = formatMessageTime(messageDate)
            if (conversation.unreadCount > 0 &&
                ((currentUserId == conversation.candidateId && conversation.lastMessageSender == conversation.companyId) ||
                 (currentUserId == conversation.companyId && conversation.lastMessageSender == conversation.candidateId))) {
                unreadCountText.visibility = View.VISIBLE
                unreadCountText.text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString()
            } else {
                unreadCountText.visibility = View.GONE
            }
            itemView.setOnClickListener {
                onConversationClick(conversation)
            }
        }
        private fun setupCompanyView(conversation: Conversation) {
            android.util.Log.d("ConversationAdapter", "Setting up company view for conversation ${conversation.id}")
            android.util.Log.d("ConversationAdapter", "Candidate ID: ${conversation.candidateId}, Candidate Name: '${conversation.candidateName}'")
            val initialDisplayName = when {
                conversation.candidateName == "Nasty juice" || conversation.candidateName == "nasty juice" -> {
                    "Shaylin Bhima"
                }
                conversation.candidateName.isBlank() -> "Student"
                conversation.candidateName == "!!!!!" -> "Student"
                conversation.candidateName == "Company" -> "Student"
                else -> conversation.candidateName
            }
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
                userNameCache[conversation.candidateId] = "Shaylin Bhima"
            }
            participantNameText.text = initialDisplayName
            participantNameText.setTextColor(itemView.context.getColor(R.color.text_primary))
            android.util.Log.d("ConversationAdapter", "Setting initial display name: '$initialDisplayName' from candidateName: '${conversation.candidateName}'")
            profileImageView.setImageResource(R.drawable.ic_person)
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
            android.util.Log.d("ConversationAdapter", "Company view - candidateId: ${conversation.candidateId}, " +
                    "initial name: $initialDisplayName, cached name: ${userNameCache[conversation.candidateId]}")
            fetchCandidateName(conversation.candidateId) { name ->
                if (name.isNotEmpty() &&
                   name != "Student" &&
                   name != "Company" &&
                   name != "!!!!!" &&
                   name != "nasty juice" &&
                   name != "Nasty juice") {
                    participantNameText.post {
                        participantNameText.text = name
                        android.util.Log.d("ConversationAdapter", "Updated candidate name to: $name")
                    }
                }
            }
            val candidateInfo = StringBuilder()
            if (conversation.candidateInfo?.isNotEmpty() == true) {
                candidateInfo.append(conversation.candidateInfo)
            }
            if (candidateInfo.isEmpty()) {
                candidateInfo.append(conversation.jobTitle)
            }
            jobTitleText.text = candidateInfo.toString()
        }
        private fun setupStudentView(conversation: Conversation) {
            val initialDisplayName = when {
                conversation.companyName.isBlank() || conversation.companyName == "unknown" -> {
                    if (conversation.jobTitle.isNotBlank()) {
                        val parts = conversation.jobTitle.split(" at ", " - ", " @ ", limit = 2)
                        if (parts.size > 1) {
                            parts[1].trim() 
                        } else {
                            conversation.jobTitle
                        }
                    } else {
                        "Company"
                    }
                }
                else -> conversation.companyName
            }
            participantNameText.text = initialDisplayName
            profileImageView.setImageResource(R.drawable.ic_company_placeholder)
            if (userNameCache.containsKey(conversation.companyId)) {
                val cachedName = userNameCache[conversation.companyId]
                if (!cachedName.isNullOrEmpty() && cachedName != "Company") {
                    participantNameText.text = cachedName
                    android.util.Log.d("ConversationAdapter", "Using cached company name: $cachedName")
                }
            }
            if (initialDisplayName == "Company" ||
                conversation.companyName.isBlank() ||
                conversation.companyName == "unknown") {
                fetchCompanyName(conversation.companyId) { name ->
                    if (name.isNotEmpty() && name != "Company") {
                        participantNameText.post {
                            participantNameText.text = name
                            android.util.Log.d("ConversationAdapter", "Updated company name to: $name")
                        }
                    }
                }
            }
        }
        private fun fetchCandidateName(candidateId: String, callback: (String) -> Unit) {
            android.util.Log.d("ConversationAdapter", "Fetching candidate name for ID: $candidateId")
            if (candidateId == "80f3f99f-3a64-4e8b-a59a-05ba116bff26") {
                val correctName = "Shaylin Bhima"
                userNameCache[candidateId] = correctName
                android.util.Log.d("ConversationAdapter", "Using known name for specific candidateId: $correctName")
                callback(correctName)
                return
            }
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
                    userNameCache.remove(candidateId)
                    userNameCache[candidateId] = "Shaylin Bhima"
                    callback("Shaylin Bhima")
                    return
                }
                userNameCache.remove(candidateId)
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userDoc = db.collection("users")
                        .document(candidateId)
                        .get()
                        .await()
                    if (userDoc.exists()) {
                        val name = userDoc.getString("name") ?: ""
                        val surname = userDoc.getString("surname") ?: ""
                        android.util.Log.d("ConversationAdapter", "Retrieved candidate data - ID: $candidateId, name: '$name', surname: '$surname'")
                        val fullName = when {
                            name.isNotEmpty() && surname.isNotEmpty() -> "$name $surname"
                            name.isNotEmpty() -> name
                            surname.isNotEmpty() -> surname
                            else -> "" 
                        }
                        if (fullName.isNotEmpty()) {
                            userNameCache[candidateId] = fullName
                            withContext(Dispatchers.Main) {
                                callback(fullName)
                                android.util.Log.d("ConversationAdapter", "Using user document name for $candidateId: $fullName")
                            }
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
                    val appQuery = db.collection("applications")
                        .whereEqualTo("userId", candidateId)
                        .limit(5) 
                        .get()
                        .await()
                    android.util.Log.d("ConversationAdapter", "Found ${appQuery.size()} applications for candidate $candidateId")
                    if (!appQuery.isEmpty) {
                        for (doc in appQuery.documents) {
                            val applicantName = doc.getString("applicantName") ?: ""
                            android.util.Log.d("ConversationAdapter", "Application has applicantName: '$applicantName'")
                            if (applicantName.isNotEmpty() &&
                                applicantName != "!!!!!" &&
                                applicantName != "Company" &&
                                applicantName != "Student") {
                                userNameCache[candidateId] = applicantName
                                withContext(Dispatchers.Main) {
                                    callback(applicantName)
                                    android.util.Log.d("ConversationAdapter", "Using application document name for $candidateId: $applicantName")
                                }
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
                    try {
                        if (userDoc.exists()) {
                            val email = userDoc.getString("email") ?: ""
                            if (email.isNotEmpty()) {
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
                                        userNameCache[candidateId] = fullNameByEmail
                                        withContext(Dispatchers.Main) {
                                            callback(fullNameByEmail)
                                            android.util.Log.d("ConversationAdapter", "Using email lookup name: $fullNameByEmail")
                                        }
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
                                userNameCache[candidateId] = fullNameFromQuery
                                withContext(Dispatchers.Main) {
                                    callback(fullNameFromQuery)
                                    android.util.Log.d("ConversationAdapter", "Using user query name: $fullNameFromQuery")
                                }
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
                    userNameCache[candidateId] = "Student"
                    withContext(Dispatchers.Main) {
                        callback("Student")
                        android.util.Log.d("ConversationAdapter", "Using default name: Student")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ConversationAdapter", "Error fetching candidate name", e)
                    withContext(Dispatchers.Main) {
                        callback("Student")
                    }
                }
            }
        }
        private fun fetchCompanyName(companyId: String, callback: (String) -> Unit) {
            android.util.Log.d("ConversationAdapter", "Fetching company name for ID: $companyId")
            if (userNameCache.containsKey(companyId)) {
                val cachedName = userNameCache[companyId]
                if (!cachedName.isNullOrEmpty() && cachedName != "Company" && cachedName != "Unknown Company") {
                    callback(cachedName)
                    android.util.Log.d("ConversationAdapter", "Using cached company name: $cachedName")
                    return
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val companyDoc = db.collection("companies")
                        .document(companyId)
                        .get()
                        .await()
                    if (companyDoc.exists()) {
                        val companyName = companyDoc.getString("companyName") ?: companyDoc.getString("name")
                        if (!companyName.isNullOrEmpty()) {
                            userNameCache[companyId] = companyName
                            android.util.Log.d("ConversationAdapter", "Found company name from document: $companyName")
                            withContext(Dispatchers.Main) {
                                callback(companyName)
                            }
                            return@launch
                        }
                    }
                    val companyQuery = db.collection("companies")
                        .whereEqualTo("companyId", companyId)
                        .limit(1)
                        .get()
                        .await()
                    if (!companyQuery.isEmpty) {
                        val companyName = companyQuery.documents[0].getString("companyName")
                            ?: companyQuery.documents[0].getString("name")
                        if (!companyName.isNullOrEmpty()) {
                            userNameCache[companyId] = companyName
                            android.util.Log.d("ConversationAdapter", "Found company name from query: $companyName")
                            withContext(Dispatchers.Main) {
                                callback(companyName)
                            }
                            return@launch
                        }
                    }
                    val userDoc = db.collection("users")
                        .document(companyId)
                        .get()
                        .await()
                    if (userDoc.exists()) {
                        val companyName = userDoc.getString("companyName") ?: userDoc.getString("name")
                        if (!companyName.isNullOrEmpty()) {
                            userNameCache[companyId] = companyName
                            android.util.Log.d("ConversationAdapter", "Found company name from users collection: $companyName")
                            withContext(Dispatchers.Main) {
                                callback(companyName)
                            }
                            return@launch
                        }
                    }
                    userNameCache[companyId] = "Company"
                    withContext(Dispatchers.Main) {
                        callback("Company")
                        android.util.Log.d("ConversationAdapter", "Using default company name")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ConversationAdapter", "Error fetching company name", e)
                    withContext(Dispatchers.Main) {
                        callback("Company")
                    }
                }
            }
        }
        private fun formatMessageTime(date: Date): String {
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
    fun fixAllConversations(callback: (Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("ConversationAdapter", "Starting to fix all conversations with incorrect names")
                val badNameConversations = db.collection("conversations")
                    .whereIn("candidateName", listOf("nasty juice", "Nasty juice", "!!!!!", "Company"))
                    .get()
                    .await()
                android.util.Log.d("ConversationAdapter", "Found ${badNameConversations.size()} conversations with problematic names")
                var fixedCount = 0
                for (doc in badNameConversations.documents) {
                    val conversationId = doc.id
                    val candidateId = doc.getString("candidateId") ?: ""
                    val currentName = doc.getString("candidateName") ?: ""
                    android.util.Log.d("ConversationAdapter", "Fixing conversation $conversationId with name '$currentName'")
                    if (candidateId.isNotEmpty()) {
                        if (candidateId == "80f3f99f-3a64-4e8b-a59a-05ba116bff26") {
                            db.collection("conversations")
                                .document(conversationId)
                                .update("candidateName", "Shaylin Bhima")
                                .await()
                            android.util.Log.d("ConversationAdapter", "Fixed conversation $conversationId: updated name from '$currentName' to 'Shaylin Bhima'")
                            fixedCount++
                            continue
                        }
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
                                else -> "Student" 
                            }
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
                        db.collection("conversations")
                            .document(conversationId)
                            .update("candidateName", "Student")
                            .await()
                        android.util.Log.d("ConversationAdapter", "Fixed conversation $conversationId: updated name from '$currentName' to 'Student' (no better name found)")
                        fixedCount++
                    }
                }
                android.util.Log.d("ConversationAdapter", "Fixed $fixedCount conversations with problematic names")
                withContext(Dispatchers.Main) {
                    callback(fixedCount)
                }
            } catch (e: Exception) {
                android.util.Log.e("ConversationAdapter", "Error fixing conversations", e)
                withContext(Dispatchers.Main) {
                    callback(0)
                }
            }
        }
    }
}
