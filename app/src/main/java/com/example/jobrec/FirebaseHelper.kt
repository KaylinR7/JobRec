package com.example.jobrec
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import android.util.Log
import kotlin.random.Random
class FirebaseHelper private constructor() {
    private val db: FirebaseFirestore
    private val auth: FirebaseAuth
    private val functions: FirebaseFunctions
    private val usersCollection: com.google.firebase.firestore.CollectionReference
    private val companiesCollection: com.google.firebase.firestore.CollectionReference
    companion object {
        @Volatile
        private var INSTANCE: FirebaseHelper? = null
        private const val TAG = "FirebaseHelper"
        fun getInstance(): FirebaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseHelper().also { INSTANCE = it }
            }
        }
    }
    init {
        db = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }
        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()
        usersCollection = db.collection("users")
        companiesCollection = db.collection("companies")
    }
    fun addUser(user: User, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    val userWithId = user.copy(id = userId)
                    usersCollection.document(userId)
                        .set(userWithId)
                        .addOnSuccessListener {
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding user to Firestore", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after auth")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user in Firebase Auth", e)
                callback(false, e.message)
            }
    }
    fun addCompany(company: Company, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(company.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    val companyWithUserId = company.copy(userId = userId, id = company.registrationNumber)
                    companiesCollection.document(company.registrationNumber)
                        .set(companyWithUserId)
                        .addOnSuccessListener {
                            Log.d(TAG, "Company added successfully with ID: ${company.registrationNumber}")
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding company to Firestore", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after auth")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating company in Firebase Auth", e)
                callback(false, e.message)
            }
    }
    fun checkUser(email: String, password: String, callback: (Boolean, String?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userEmail = email.lowercase()
                Log.d(TAG, "Looking for user with email (lowercase): $userEmail")
                usersCollection.get()
                    .addOnSuccessListener { userDocuments ->
                        val userDoc = userDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }
                        if (userDoc != null) {
                            val emailVerified = userDoc.getBoolean("emailVerified") ?: false
                            // Allow login regardless of verification status
                            // The app will handle verification after login
                            callback(true, "user", if (!emailVerified) "verification_required" else null)
                            return@addOnSuccessListener
                        }
                        companiesCollection.get()
                            .addOnSuccessListener { companyDocuments ->
                                val companyDoc = companyDocuments.find { doc ->
                                    doc.getString("email")?.lowercase() == userEmail
                                }
                                if (companyDoc != null) {
                                    val emailVerified = companyDoc.getBoolean("emailVerified") ?: false
                                    // Allow login regardless of verification status
                                    // The app will handle verification after login
                                    callback(true, "company", if (!emailVerified) "verification_required" else null)
                                } else {
                                    callback(true, "unknown", null)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error checking companies collection", e)
                                callback(false, null, e.message)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error checking users collection", e)
                        callback(false, null, e.message)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error signing in", e)
                callback(false, null, e.message)
            }
    }
    fun isEmailExists(email: String, callback: (Boolean) -> Unit) {
        val userEmail = email.lowercase()
        Log.d(TAG, "Checking if email exists (lowercase): $userEmail")
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }
                if (userDoc != null) {
                    callback(true)
                    return@addOnSuccessListener
                }
                companiesCollection.get()
                    .addOnSuccessListener { companyDocuments ->
                        val companyDoc = companyDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }
                        callback(companyDoc != null)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error checking companies collection", e)
                        callback(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking users collection", e)
                callback(false)
            }
    }
    fun isIdNumberExists(idNumber: String, callback: (Boolean) -> Unit) {
        usersCollection.document(idNumber)
            .get()
            .addOnSuccessListener { document ->
                callback(document.exists())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking ID number", e)
                callback(false)
            }
    }
    fun recoverOrCreateUser(user: User, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    usersCollection.document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                callback(true, null)
                            } else {
                                val userWithId = user.copy(id = userId)
                                usersCollection.document(userId)
                                    .set(userWithId)
                                    .addOnSuccessListener {
                                        callback(true, null)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to create Firestore record for existing Auth user", e)
                                        callback(false, e.message)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking for existing Firestore record", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after sign in")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sign in to existing account", e)
                addUser(user, password, callback)
            }
    }
    fun recoverOrCreateCompany(company: Company, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(company.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    val companyEmail = company.email.lowercase()
                    Log.d(TAG, "Checking if company exists with email (lowercase): $companyEmail")
                    companiesCollection.get()
                        .addOnSuccessListener { documents ->
                            val companyDoc = documents.find { doc ->
                                doc.getString("email")?.lowercase() == companyEmail
                            }
                            if (companyDoc != null) {
                                callback(true, null)
                            } else {
                                val companyWithUserId = company.copy(userId = userId, id = company.registrationNumber)
                                companiesCollection.document(company.registrationNumber)
                                    .set(companyWithUserId)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Company recovered successfully with ID: ${company.registrationNumber}")
                                        callback(true, null)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to create Firestore record for existing Auth company", e)
                                        callback(false, e.message)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking for existing Firestore record", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after sign in")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sign in to existing account", e)
                addCompany(company, password, callback)
            }
    }

    // Email Verification Methods
    fun generateVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    fun sendVerificationEmail(email: String, verificationCode: String, userType: String, name: String, callback: (Boolean, String?) -> Unit) {
        val emailSubject = "DUTCareerHub - Email Verification"
        val emailBody = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2196F3;">Welcome to DUTCareerHub!</h2>
                    <p>Hello $name,</p>
                    <p>Thank you for registering as a ${if (userType == "student") "student" else "company"} on DUTCareerHub.</p>
                    <p>To complete your registration, please use the following verification code:</p>
                    <div style="background-color: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0; border-radius: 5px;">
                        <h1 style="color: #2196F3; font-size: 32px; margin: 0; letter-spacing: 5px;">$verificationCode</h1>
                    </div>
                    <p>This code will expire in 15 minutes for security purposes.</p>
                    <p>If you didn't create an account with DUTCareerHub, please ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        This is an automated message from DUTCareerHub. Please do not reply to this email.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()

        val mailData = hashMapOf(
            "to" to email,
            "message" to hashMapOf(
                "subject" to emailSubject,
                "html" to emailBody
            )
        )

        Log.d(TAG, "Sending verification email to: $email with code: $verificationCode")

        // Write to mail collection - the Firebase extension will automatically send the email
        db.collection("mail")
            .add(mailData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Verification email queued successfully with ID: ${documentReference.id}")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error queuing verification email", e)
                callback(false, "Failed to send verification email: ${e.message}")
            }
    }

    fun registerUserWithVerification(user: User, password: String, callback: (Boolean, String?, String?) -> Unit) {
        // Generate verification code
        val verificationCode = generateVerificationCode()
        val userWithCode = user.copy(verificationCode = verificationCode, emailVerified = false)

        // Create Firebase Auth user but don't sign them in
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    // Store user data with verification code
                    val userWithId = userWithCode.copy(id = userId)
                    usersCollection.document(userId)
                        .set(userWithId)
                        .addOnSuccessListener {
                            // Sign out the user immediately to ensure they must login after verification
                            auth.signOut()
                            Log.d(TAG, "User registered and signed out. User must login after verification.")

                            // Send verification email
                            sendVerificationEmail(
                                user.email,
                                verificationCode,
                                "student",
                                user.name
                            ) { emailSent, emailError ->
                                if (emailSent) {
                                    callback(true, null, verificationCode)
                                } else {
                                    callback(false, "User created but failed to send verification email: $emailError", null)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding user to Firestore", e)
                            callback(false, e.message, null)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after auth")
                    callback(false, "Failed to get user ID", null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user in Firebase Auth", e)
                callback(false, e.message, null)
            }
    }

    fun registerCompanyWithVerification(company: Company, password: String, callback: (Boolean, String?, String?) -> Unit) {
        // Generate verification code
        val verificationCode = generateVerificationCode()
        val companyWithCode = company.copy(verificationCode = verificationCode, emailVerified = false)

        // Create Firebase Auth user but don't sign them in
        auth.createUserWithEmailAndPassword(company.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    // Store company data with verification code
                    val companyWithUserId = companyWithCode.copy(userId = userId, id = company.registrationNumber)
                    companiesCollection.document(company.registrationNumber)
                        .set(companyWithUserId)
                        .addOnSuccessListener {
                            // Sign out the user immediately to ensure they must login after verification
                            auth.signOut()
                            Log.d(TAG, "Company registered and signed out. User must login after verification.")

                            // Send verification email
                            sendVerificationEmail(
                                company.email,
                                verificationCode,
                                "company",
                                company.companyName
                            ) { emailSent, emailError ->
                                if (emailSent) {
                                    callback(true, null, verificationCode)
                                } else {
                                    callback(false, "Company created but failed to send verification email: $emailError", null)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding company to Firestore", e)
                            callback(false, e.message, null)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after auth")
                    callback(false, "Failed to get user ID", null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating company in Firebase Auth", e)
                callback(false, e.message, null)
            }
    }

    fun verifyEmailCode(email: String, enteredCode: String, callback: (Boolean, String?, String?) -> Unit) {
        val userEmail = email.lowercase()

        // Check users collection first
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                if (userDoc != null) {
                    val storedCode = userDoc.getString("verificationCode")
                    if (storedCode == enteredCode) {
                        // Update user as verified
                        usersCollection.document(userDoc.id)
                            .update(mapOf(
                                "emailVerified" to true,
                                "verificationCode" to ""
                            ))
                            .addOnSuccessListener {
                                callback(true, "user", null)
                            }
                            .addOnFailureListener { e ->
                                callback(false, null, e.message)
                            }
                    } else {
                        callback(false, null, "Invalid verification code")
                    }
                    return@addOnSuccessListener
                }

                // Check companies collection
                companiesCollection.get()
                    .addOnSuccessListener { companyDocuments ->
                        val companyDoc = companyDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }

                        if (companyDoc != null) {
                            val storedCode = companyDoc.getString("verificationCode")
                            if (storedCode == enteredCode) {
                                // Update company as verified
                                companiesCollection.document(companyDoc.id)
                                    .update(mapOf(
                                        "emailVerified" to true,
                                        "verificationCode" to ""
                                    ))
                                    .addOnSuccessListener {
                                        callback(true, "company", null)
                                    }
                                    .addOnFailureListener { e ->
                                        callback(false, null, e.message)
                                    }
                            } else {
                                callback(false, null, "Invalid verification code")
                            }
                        } else {
                            callback(false, null, "Email not found")
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, null, e.message)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, null, e.message)
            }
    }

    // Password Reset Methods
    fun sendPasswordResetCode(email: String, callback: (Boolean, String?) -> Unit) {
        val userEmail = email.lowercase()
        val resetCode = generateVerificationCode()
        val expiryTime = System.currentTimeMillis() + (15 * 60 * 1000) // 15 minutes

        // Check users collection first
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                if (userDoc != null) {
                    val userData = userDoc.data
                    val userName = "${userData?.get("name") ?: ""} ${userData?.get("surname") ?: ""}".trim()

                    // Update user with reset code
                    usersCollection.document(userDoc.id)
                        .update(mapOf(
                            "passwordResetCode" to resetCode,
                            "passwordResetExpiry" to expiryTime
                        ))
                        .addOnSuccessListener {
                            // Send reset email
                            sendPasswordResetEmail(email, resetCode, "student", userName) { emailSent, emailError ->
                                callback(emailSent, emailError)
                            }
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message)
                        }
                    return@addOnSuccessListener
                }

                // Check companies collection
                companiesCollection.get()
                    .addOnSuccessListener { companyDocuments ->
                        val companyDoc = companyDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }

                        if (companyDoc != null) {
                            val companyData = companyDoc.data
                            val companyName = companyData?.get("companyName") as? String ?: ""

                            // Update company with reset code
                            companiesCollection.document(companyDoc.id)
                                .update(mapOf(
                                    "passwordResetCode" to resetCode,
                                    "passwordResetExpiry" to expiryTime
                                ))
                                .addOnSuccessListener {
                                    // Send reset email
                                    sendPasswordResetEmail(email, resetCode, "company", companyName) { emailSent, emailError ->
                                        callback(emailSent, emailError)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    callback(false, e.message)
                                }
                        } else {
                            callback(false, "Email not found")
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    private fun sendPasswordResetEmail(email: String, resetCode: String, userType: String, name: String, callback: (Boolean, String?) -> Unit) {
        val emailSubject = "DUTCareerHub - Password Reset"
        val emailBody = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2196F3;">Password Reset Request</h2>
                    <p>Hello $name,</p>
                    <p>We received a request to reset your password for your DUTCareerHub ${if (userType == "student") "student" else "company"} account.</p>
                    <p>Please use the following code to reset your password:</p>
                    <div style="background-color: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0; border-radius: 5px;">
                        <h1 style="color: #2196F3; font-size: 32px; margin: 0; letter-spacing: 5px;">$resetCode</h1>
                    </div>
                    <p><strong>This code will expire in 15 minutes</strong> for security purposes.</p>
                    <p>If you didn't request a password reset, please ignore this email. Your password will remain unchanged.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        This is an automated message from DUTCareerHub. Please do not reply to this email.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()

        val mailData = hashMapOf(
            "to" to email,
            "message" to hashMapOf(
                "subject" to emailSubject,
                "html" to emailBody
            )
        )

        Log.d(TAG, "Sending password reset email to: $email with code: $resetCode")

        // Write to mail collection - the Firebase extension will automatically send the email
        db.collection("mail")
            .add(mailData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Password reset email queued successfully with ID: ${documentReference.id}")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error queuing password reset email", e)
                callback(false, "Failed to send password reset email: ${e.message}")
            }
    }

    fun verifyPasswordResetCode(email: String, enteredCode: String, callback: (Boolean, String?) -> Unit) {
        val userEmail = email.lowercase()
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "Verifying password reset code for email: $userEmail, entered code: $enteredCode")

        // Check users collection first
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                if (userDoc != null) {
                    val storedCode = userDoc.getString("passwordResetCode")
                    val expiryTime = userDoc.getLong("passwordResetExpiry") ?: 0

                    Log.d(TAG, "Found user doc. Stored code: $storedCode, entered code: $enteredCode, expiry: $expiryTime, current time: $currentTime")

                    when {
                        storedCode != enteredCode -> {
                            Log.d(TAG, "Code verification failed: Invalid reset code")
                            callback(false, "Invalid reset code")
                        }
                        currentTime > expiryTime -> {
                            Log.d(TAG, "Code verification failed: Reset code has expired")
                            callback(false, "Reset code has expired")
                        }
                        else -> {
                            Log.d(TAG, "Code verification successful")
                            callback(true, null)
                        }
                    }
                    return@addOnSuccessListener
                }

                // Check companies collection
                Log.d(TAG, "User not found in users collection, checking companies collection")
                companiesCollection.get()
                    .addOnSuccessListener { companyDocuments ->
                        val companyDoc = companyDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }

                        if (companyDoc != null) {
                            val storedCode = companyDoc.getString("passwordResetCode")
                            val expiryTime = companyDoc.getLong("passwordResetExpiry") ?: 0

                            Log.d(TAG, "Found company doc. Stored code: $storedCode, entered code: $enteredCode, expiry: $expiryTime, current time: $currentTime")

                            when {
                                storedCode != enteredCode -> {
                                    Log.d(TAG, "Company code verification failed: Invalid reset code")
                                    callback(false, "Invalid reset code")
                                }
                                currentTime > expiryTime -> {
                                    Log.d(TAG, "Company code verification failed: Reset code has expired")
                                    callback(false, "Reset code has expired")
                                }
                                else -> {
                                    Log.d(TAG, "Company code verification successful")
                                    callback(true, null)
                                }
                            }
                        } else {
                            Log.d(TAG, "Email not found in either users or companies collection")
                            callback(false, "Email not found")
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    // Method to check if current user needs verification
    fun getCurrentUserVerificationStatus(email: String, callback: (Boolean, String?, String?) -> Unit) {
        val userEmail = email.lowercase()
        Log.d(TAG, "Checking verification status for email: $userEmail")

        // Check users collection first
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                if (userDoc != null) {
                    val emailVerified = userDoc.getBoolean("emailVerified") ?: false
                    val verificationCode = userDoc.getString("verificationCode") ?: ""
                    Log.d(TAG, "Found user doc. emailVerified: $emailVerified, verificationCode: $verificationCode")
                    callback(!emailVerified, "user", if (!emailVerified && verificationCode.isNotEmpty()) verificationCode else null)
                    return@addOnSuccessListener
                }

                // Check companies collection
                Log.d(TAG, "User not found in users collection, checking companies collection")
                companiesCollection.get()
                    .addOnSuccessListener { companyDocuments ->
                        val companyDoc = companyDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }

                        if (companyDoc != null) {
                            val emailVerified = companyDoc.getBoolean("emailVerified") ?: false
                            val verificationCode = companyDoc.getString("verificationCode") ?: ""
                            Log.d(TAG, "Found company doc. emailVerified: $emailVerified, verificationCode: $verificationCode")
                            callback(!emailVerified, "company", if (!emailVerified && verificationCode.isNotEmpty()) verificationCode else null)
                        } else {
                            Log.d(TAG, "User not found in either users or companies collection")
                            callback(false, null, "User not found")
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, null, e.message)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, null, e.message)
            }
    }

    // Method to resend verification code for logged-in users
    fun resendVerificationCode(email: String, callback: (Boolean, String?, String?) -> Unit) {
        val userEmail = email.lowercase()
        val newVerificationCode = generateVerificationCode()

        // Check users collection first
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                if (userDoc != null) {
                    val userData = userDoc.data
                    val userName = "${userData?.get("name") ?: ""} ${userData?.get("surname") ?: ""}".trim()

                    // Update user with new verification code
                    usersCollection.document(userDoc.id)
                        .update("verificationCode", newVerificationCode)
                        .addOnSuccessListener {
                            // Send verification email
                            sendVerificationEmail(email, newVerificationCode, "student", userName) { emailSent, emailError ->
                                if (emailSent) {
                                    callback(true, "user", newVerificationCode)
                                } else {
                                    callback(false, emailError, null)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message, null)
                        }
                    return@addOnSuccessListener
                }

                // Check companies collection
                companiesCollection.get()
                    .addOnSuccessListener { companyDocuments ->
                        val companyDoc = companyDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }

                        if (companyDoc != null) {
                            val companyData = companyDoc.data
                            val companyName = companyData?.get("companyName") as? String ?: ""

                            // Update company with new verification code
                            companiesCollection.document(companyDoc.id)
                                .update("verificationCode", newVerificationCode)
                                .addOnSuccessListener {
                                    // Send verification email
                                    sendVerificationEmail(email, newVerificationCode, "company", companyName) { emailSent, emailError ->
                                        if (emailSent) {
                                            callback(true, "company", newVerificationCode)
                                        } else {
                                            callback(false, emailError, null)
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    callback(false, e.message, null)
                                }
                        } else {
                            callback(false, "User not found", null)
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message, null)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, e.message, null)
            }
    }

    fun resetPasswordWithCode(email: String, resetCode: String, newPassword: String, callback: (Boolean, String?) -> Unit) {
        // First verify the reset code
        verifyPasswordResetCode(email, resetCode) { codeValid, codeError ->
            if (codeValid) {
                // Code is valid, now we need to change the password
                // Since Firebase doesn't allow direct password changes without authentication,
                // we'll use a Cloud Function to handle this securely
                val data = hashMapOf(
                    "email" to email.lowercase(),
                    "newPassword" to newPassword,
                    "resetCode" to resetCode
                )

                Log.d(TAG, "Calling Cloud Function resetPasswordWithCode for email: $email")
                functions.getHttpsCallable("resetPasswordWithCode")
                    .call(data)
                    .addOnSuccessListener { result ->
                        Log.d(TAG, "Cloud Function resetPasswordWithCode succeeded")
                        // Clear our reset codes
                        clearPasswordResetCodes(email)
                        callback(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Cloud Function resetPasswordWithCode failed", e)
                        callback(false, "Failed to reset password: ${e.message}")
                    }
            } else {
                callback(false, codeError)
            }
        }
    }

    // Simplified reset password method - now redirects to code-based reset
    fun resetPassword(email: String, newPassword: String, callback: (Boolean, String?) -> Unit) {
        // Use our custom code-based reset instead of Firebase's link-based reset
        // This method is called from ResetPasswordActivity which already has the code
        // So we should not be calling this method anymore - it's kept for compatibility
        callback(false, "This method should not be called. Use resetPasswordWithCode instead.")
    }

    // Alternative method for when user is already signed in
    fun updatePasswordForSignedInUser(newPassword: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.updatePassword(newPassword)
                .addOnSuccessListener {
                    callback(true, null)
                }
                .addOnFailureListener { e ->
                    callback(false, e.message)
                }
        } else {
            callback(false, "User not signed in")
        }
    }

    private fun clearPasswordResetCodes(email: String) {
        val userEmail = email.lowercase()

        // Clear from users collection
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                userDoc?.let {
                    usersCollection.document(it.id)
                        .update(mapOf(
                            "passwordResetCode" to "",
                            "passwordResetExpiry" to 0
                        ))
                }
            }

        // Clear from companies collection
        companiesCollection.get()
            .addOnSuccessListener { companyDocuments ->
                val companyDoc = companyDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                companyDoc?.let {
                    companiesCollection.document(it.id)
                        .update(mapOf(
                            "passwordResetCode" to "",
                            "passwordResetExpiry" to 0
                        ))
                }
            }
    }

    // Admin function to completely delete a user from both Firebase Auth and Firestore
    fun adminDeleteUser(userId: String, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "Admin deleting user with ID: $userId")

        // First get the user's email from Firestore
        usersCollection.document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userEmail = document.getString("email")
                    if (userEmail != null) {
                        // Delete from Firestore first
                        usersCollection.document(userId)
                            .delete()
                            .addOnSuccessListener {
                                Log.d(TAG, "User deleted from Firestore successfully")

                                // Now delete from Firebase Auth
                                deleteUserFromAuth(userEmail) { authDeleted, authError ->
                                    if (authDeleted) {
                                        Log.d(TAG, "User completely deleted from both Firestore and Auth")
                                        callback(true, null)
                                    } else {
                                        Log.w(TAG, "User deleted from Firestore but Auth deletion failed: $authError")
                                        callback(true, "User deleted from database, but Auth cleanup failed: $authError")
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to delete user from Firestore", e)
                                callback(false, "Failed to delete user from database: ${e.message}")
                            }
                    } else {
                        callback(false, "User email not found")
                    }
                } else {
                    callback(false, "User not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user document", e)
                callback(false, "Error accessing user data: ${e.message}")
            }
    }

    // Admin function to completely delete a company from both Firebase Auth and Firestore
    fun adminDeleteCompany(companyId: String, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "Admin deleting company with ID: $companyId")

        // First get the company's email from Firestore
        companiesCollection.document(companyId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val companyEmail = document.getString("email")
                    if (companyEmail != null) {
                        // Delete from Firestore first
                        companiesCollection.document(companyId)
                            .delete()
                            .addOnSuccessListener {
                                Log.d(TAG, "Company deleted from Firestore successfully")

                                // Now delete from Firebase Auth
                                deleteUserFromAuth(companyEmail) { authDeleted, authError ->
                                    if (authDeleted) {
                                        Log.d(TAG, "Company completely deleted from both Firestore and Auth")
                                        callback(true, null)
                                    } else {
                                        Log.w(TAG, "Company deleted from Firestore but Auth deletion failed: $authError")
                                        callback(true, "Company deleted from database, but Auth cleanup failed: $authError")
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to delete company from Firestore", e)
                                callback(false, "Failed to delete company from database: ${e.message}")
                            }
                    } else {
                        callback(false, "Company email not found")
                    }
                } else {
                    callback(false, "Company not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting company document", e)
                callback(false, "Error accessing company data: ${e.message}")
            }
    }

    // Helper function to delete user from Firebase Auth using Cloud Function
    private fun deleteUserFromAuth(email: String, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "Attempting to delete user from Firebase Auth: $email")

        val data = hashMapOf(
            "email" to email.lowercase()
        )

        functions.getHttpsCallable("deleteUserByEmail")
            .call(data)
            .addOnSuccessListener { result ->
                Log.d(TAG, "User deleted from Firebase Auth successfully")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete user from Firebase Auth", e)
                // Don't treat this as a complete failure since Firestore deletion succeeded
                callback(false, e.message)
            }
    }
}