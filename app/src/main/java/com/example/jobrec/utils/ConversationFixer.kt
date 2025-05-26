package com.example.jobrec.utils
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
class ConversationFixer {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "ConversationFixer"
    suspend fun fixAllConversations() {
        try {
            Log.d(TAG, "Conversation fixing is disabled to prevent data issues")
            val conversations = db.collection("conversations")
                .get()
                .await()
            Log.d(TAG, "Found ${conversations.size()} conversations in the database")
            for (conversationDoc in conversations) {
                val conversationId = conversationDoc.id
                val companyId = conversationDoc.getString("companyId") ?: "null"
                val candidateId = conversationDoc.getString("candidateId") ?: "null"
                val companyName = conversationDoc.getString("companyName") ?: "null"
                val candidateName = conversationDoc.getString("candidateName") ?: "null"
                Log.d(TAG, "Conversation $conversationId: companyId=$companyId, candidateId=$candidateId, " +
                        "companyName=$companyName, candidateName=$candidateName")
            }
            Log.d(TAG, "Finished logging conversation information")
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing conversations", e)
            throw e
        }
    }
}
