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
     * Fix all conversations in the database by updating the companyId field
     * to use the Firebase Auth user ID.
     */
    suspend fun fixAllConversations() {
        try {
            // Get all conversations
            val conversations = db.collection("conversations")
                .get()
                .await()

            Log.d(TAG, "Found ${conversations.size()} conversations to check")

            // Process each conversation
            for (conversationDoc in conversations) {
                val conversationId = conversationDoc.id
                val companyId = conversationDoc.getString("companyId") ?: continue

                Log.d(TAG, "Checking conversation $conversationId with companyId=$companyId")

                // Check if the companyId is a Firebase Auth user ID (typically longer than 10 chars)
                if (companyId.length > 10) {
                    // This might be a Firebase Auth ID, let's verify it exists in companies collection
                    val companyByUserIdDoc = db.collection("companies")
                        .whereEqualTo("userId", companyId)
                        .get()
                        .await()

                    if (!companyByUserIdDoc.isEmpty) {
                        Log.d(TAG, "Company ID is a valid Firebase Auth ID, no need to fix")
                        continue
                    } else {
                        Log.d(TAG, "Company ID looks like a Firebase Auth ID but no company found, might need fixing")
                        // Continue to try fixing it below
                    }
                }

                // Look up the company's Firebase Auth user ID
                val companyDoc = db.collection("companies")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()

                // If we found the company, update the conversation
                if (!companyDoc.isEmpty) {
                    val companyUserId = companyDoc.documents[0].getString("userId")
                    if (companyUserId != null) {
                        Log.d(TAG, "Updating conversation $conversationId: changing companyId from $companyId to $companyUserId")

                        // Update the conversation
                        db.collection("conversations")
                            .document(conversationId)
                            .update("companyId", companyUserId)
                            .await()

                        Log.d(TAG, "Successfully updated conversation $conversationId")
                    } else {
                        Log.d(TAG, "Company found but userId is null, skipping")
                    }
                } else {
                    // Try the reverse lookup - maybe we have a Firebase Auth ID but need the custom company ID
                    val companyByUserIdDoc = db.collection("companies")
                        .whereEqualTo("userId", companyId)
                        .get()
                        .await()

                    if (!companyByUserIdDoc.isEmpty) {
                        val customCompanyId = companyByUserIdDoc.documents[0].getString("companyId")
                        if (customCompanyId != null) {
                            Log.d(TAG, "Found company with userId=$companyId, has custom companyId=$customCompanyId")

                            // We'll keep the Firebase Auth ID as it's more reliable
                            Log.d(TAG, "Keeping Firebase Auth ID for conversation $conversationId")
                        }
                    } else {
                        // Special case: Check if this is a numeric company ID (like "090878")
                        // These might be legacy IDs that need special handling
                        if (companyId.matches(Regex("\\d+"))) {
                            Log.d(TAG, "Found numeric company ID: $companyId, checking for company")

                            // Try to find any company with this ID
                            val anyCompanyDoc = db.collection("companies")
                                .get()
                                .await()

                            // Log all companies for debugging
                            Log.d(TAG, "Found ${anyCompanyDoc.size()} companies in database")
                            anyCompanyDoc.forEach { doc ->
                                val cId = doc.getString("companyId") ?: "null"
                                val userId = doc.getString("userId") ?: "null"
                                val name = doc.getString("companyName") ?: "null"
                                Log.d(TAG, "Company: name=$name, companyId=$cId, userId=$userId")
                            }

                            // Try to find the first company and use its userId
                            if (!anyCompanyDoc.isEmpty) {
                                val firstCompany = anyCompanyDoc.documents[0]
                                val userId = firstCompany.getString("userId")
                                if (userId != null) {
                                    Log.d(TAG, "Using first company's userId=$userId for conversation $conversationId")

                                    // Update the conversation with this userId
                                    db.collection("conversations")
                                        .document(conversationId)
                                        .update("companyId", userId)
                                        .await()

                                    Log.d(TAG, "Successfully updated conversation $conversationId with company userId")
                                }
                            }
                        } else {
                            Log.d(TAG, "Company not found for ID $companyId, skipping")
                        }
                    }
                }
            }

            Log.d(TAG, "Finished fixing conversations")
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing conversations", e)
            throw e
        }
    }
}
