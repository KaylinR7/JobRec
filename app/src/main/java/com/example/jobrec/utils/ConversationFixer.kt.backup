package com.example.jobrec.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Utility class to fix existing conversations by updating the companyId field
 * to use the Firebase Auth user ID instead of the custom company ID.
 * Also handles fixing conversations in the other direction if needed.
 */
class ConversationFixer {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "ConversationFixer"

    /**
     * This method is now disabled to prevent modifying conversations that don't belong to the current user.
     * It now only logs information about conversations without making any changes.
     */
    suspend fun fixAllConversations() {
        try {
            Log.d(TAG, "Conversation fixing is disabled to prevent data issues")

            // Just log information about conversations without making changes
            val conversations = db.collection("conversations")
                .get()
                .await()

            Log.d(TAG, "Found ${conversations.size()} conversations in the database")

            // Log information about each conversation for debugging
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
