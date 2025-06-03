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
class CompanySignupFragment : Fragment() {
    private lateinit var companyNameInput: TextInputEditText
    private lateinit var registrationNumberInput: TextInputEditText
    private lateinit var industryInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var registerButton: MaterialButton
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_company_signup, container, false)
        companyNameInput = view.findViewById(R.id.companyNameInput)
        registrationNumberInput = view.findViewById(R.id.registrationNumberInput)
        industryInput = view.findViewById(R.id.industryInput)
        emailInput = view.findViewById(R.id.emailInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        registerButton = view.findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            registerCompany()
        }
        return view
    }
    private fun registerCompany() {
        val companyName = companyNameInput.text.toString().trim()
        val registrationNumber = registrationNumberInput.text.toString().trim()
        val industry = industryInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (companyName.isEmpty() || registrationNumber.isEmpty() || industry.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate password strength
        val passwordValidation = PasswordValidator.validatePassword(password)
        if (!passwordValidation.isValid) {
            Toast.makeText(requireContext(), "Password must contain:\n${passwordValidation.errors.joinToString("\n")}", Toast.LENGTH_LONG).show()
            return
        }
        val company = Company(
            id = registrationNumber,
            companyName = companyName,
            registrationNumber = registrationNumber,
            industry = industry,
            companySize = "",
            location = "",
            website = "",
            description = "",
            contactPersonName = "",
            contactPersonEmail = "",
            contactPersonPhone = "",
            email = email.lowercase()
        )
        FirebaseHelper.getInstance().registerCompanyWithVerification(company, password) { success, error, verificationCode ->
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
