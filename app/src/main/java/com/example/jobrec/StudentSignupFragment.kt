package com.example.jobrec
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.jobrec.utils.PasswordValidator
class StudentSignupFragment : Fragment() {
    private lateinit var nameInput: TextInputEditText
    private lateinit var surnameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var registerButton: MaterialButton
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_student_signup, container, false)
        nameInput = view.findViewById(R.id.etName)
        surnameInput = view.findViewById(R.id.etSurname)
        emailInput = view.findViewById(R.id.etEmail)
        passwordInput = view.findViewById(R.id.etPassword)
        registerButton = view.findViewById(R.id.btnRegister)
        registerButton.setOnClickListener {
            registerStudent()
        }
        return view
    }
    private fun registerStudent() {
        val name = nameInput.text.toString().trim()
        val surname = surnameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate password strength
        val passwordValidation = PasswordValidator.validatePassword(password)
        if (!passwordValidation.isValid) {
            Toast.makeText(requireContext(), "Password must contain:\n${passwordValidation.errors.joinToString("\n")}", Toast.LENGTH_LONG).show()
            return
        }
        val user = User(
            name = name,
            surname = surname,
            email = email.lowercase(),
            phoneNumber = "",
            province = "",
            city = "",
            summary = "",
            linkedin = "",
            github = "",
            portfolio = "",
            skills = listOf(),
            yearsOfExperience = "",
            certificate = "",
            expectedSalary = "",
            field = "",
            subField = ""
        )
        FirebaseHelper.getInstance().registerUserWithVerification(user, password) { success, error, verificationCode ->
            // Check if fragment is still attached to avoid crashes
            if (isAdded && !isDetached && activity != null) {
                requireActivity().runOnUiThread {
                    if (success) {
                        Toast.makeText(requireContext(), "Registration successful! Please check your email for verification code, then login to continue.", Toast.LENGTH_LONG).show()
                    } else {
                        // Show error but still redirect to login
                        if (error?.contains("email address is already in use", ignoreCase = true) == true) {
                            Toast.makeText(requireContext(), "Email already registered. Please login instead.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Registration failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // ALWAYS redirect to login page regardless of success or failure
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.putExtra("registration_completed", true)
                    intent.putExtra("registered_email", email)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }
    }
}
