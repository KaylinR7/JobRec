package com.example.jobrec
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class AdminEditUserDialog : DialogFragment() {
    private lateinit var user: User
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AdminEditUserDialog"
    var onUserUpdated: (() -> Unit)? = null
    var onUserDeleted: (() -> Unit)? = null
    private var userId: String = ""
    private var isNewUser: Boolean = false
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnShowListener {
            val width = resources.displayMetrics.widthPixels
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window?.setLayout((width * 0.9).toInt(), height)
        }
        return dialog
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_admin_edit_user, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = arguments?.getParcelable(ARG_USER) ?: User()
        userId = arguments?.getString(ARG_USER_ID) ?: ""
        isNewUser = userId.isEmpty()
        view.findViewById<android.widget.TextView>(R.id.dialogTitleTextView).text = 
            if (isNewUser) "Add New User" else "Edit User"
        setupUI(view)
        view.findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener {
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            saveChanges(view)
        }
        view.findViewById<MaterialButton>(R.id.deleteButton).setOnClickListener {
            confirmDelete(view)
        }
        if (isNewUser) {
            view.findViewById<MaterialButton>(R.id.deleteButton).visibility = View.GONE
        }
    }
    private fun setupUI(view: View) {
        view.findViewById<TextInputEditText>(R.id.nameEditText).setText(user.name)
        view.findViewById<TextInputEditText>(R.id.surnameEditText).setText(user.surname)
        view.findViewById<TextInputEditText>(R.id.emailEditText).setText(user.email)
        if (!isNewUser) {
            view.findViewById<TextInputEditText>(R.id.emailEditText).isEnabled = false
        }
    }
    private fun saveChanges(view: View) {
        val name = view.findViewById<TextInputEditText>(R.id.nameEditText).text.toString().trim()
        val surname = view.findViewById<TextInputEditText>(R.id.surnameEditText).text.toString().trim()
        val email = view.findViewById<TextInputEditText>(R.id.emailEditText).text.toString().trim()
        val password = view.findViewById<TextInputEditText>(R.id.passwordEditText).text.toString()
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty()) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        val updatedUser = user.copy(
            name = name,
            surname = surname,
            email = email
        )
        if (isNewUser) {
            createNewUser(updatedUser, password)
        } else {
            updateExistingUser(updatedUser, password)
        }
    }
    private fun createNewUser(user: User, password: String) {
        if (password.isEmpty()) {
            Toast.makeText(context, "Password is required for new users", Toast.LENGTH_SHORT).show()
            return
        }
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener
                db.collection("users").document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        Log.d(TAG, "User created successfully")
                        Toast.makeText(context, "User created successfully", Toast.LENGTH_SHORT).show()
                        onUserUpdated?.invoke()
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error creating user in Firestore", e)
                        Toast.makeText(context, "Error creating user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user in Firebase Auth", e)
                Toast.makeText(context, "Error creating user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateExistingUser(user: User, password: String) {
        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "User updated successfully")
                if (password.isNotEmpty()) {
                    updateUserPassword(user.email, password)
                } else {
                    Toast.makeText(context, "User updated successfully", Toast.LENGTH_SHORT).show()
                    onUserUpdated?.invoke()
                    dismiss()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating user", e)
                Toast.makeText(context, "Error updating user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateUserPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                auth.currentUser?.updatePassword(password)
                    ?.addOnSuccessListener {
                        Log.d(TAG, "Password updated successfully")
                        Toast.makeText(context, "User and password updated successfully", Toast.LENGTH_SHORT).show()
                        onUserUpdated?.invoke()
                        dismiss()
                    }
                    ?.addOnFailureListener { e ->
                        Log.e(TAG, "Error updating password", e)
                        Toast.makeText(context, "User updated but error updating password: ${e.message}", Toast.LENGTH_SHORT).show()
                        onUserUpdated?.invoke()
                        dismiss()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error signing in to update password", e)
                Toast.makeText(context, "User updated but couldn't update password: ${e.message}", Toast.LENGTH_SHORT).show()
                onUserUpdated?.invoke()
                dismiss()
            }
    }
    private fun confirmDelete(view: View) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete this user? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun deleteUser() {
        db.collection("users").document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "User deleted successfully from Firestore")
                Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                onUserDeleted?.invoke()
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user from Firestore", e)
                Toast.makeText(context, "Error deleting user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    companion object {
        private const val ARG_USER = "user"
        private const val ARG_USER_ID = "user_id"
        fun newInstance(user: User, userId: String = ""): AdminEditUserDialog {
            val fragment = AdminEditUserDialog()
            val args = Bundle()
            args.putParcelable(ARG_USER, user)
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }
}
