package com.example.jobrec.utils

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.jobrec.R

object PasswordValidator {
    
    enum class PasswordStrength(val value: Int, val label: String, val color: Int) {
        WEAK(1, "Weak", android.R.color.holo_red_dark),
        MEDIUM(2, "Medium", android.R.color.holo_orange_dark),
        STRONG(3, "Strong", android.R.color.holo_green_dark)
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val strength: PasswordStrength,
        val errors: List<String>,
        val score: Int
    )
    
    /**
     * Validates password according to South African standards
     */
    fun validatePassword(password: String): ValidationResult {
        val errors = mutableListOf<String>()
        var score = 0
        
        // Check minimum length
        if (password.length < 8) {
            errors.add("At least 8 characters required")
        } else {
            score += 1
        }
        
        // Check for uppercase letter
        if (!password.any { it.isUpperCase() }) {
            errors.add("At least one uppercase letter required")
        } else {
            score += 1
        }
        
        // Check for lowercase letter
        if (!password.any { it.isLowerCase() }) {
            errors.add("At least one lowercase letter required")
        } else {
            score += 1
        }
        
        // Check for digit
        if (!password.any { it.isDigit() }) {
            errors.add("At least one number required")
        } else {
            score += 1
        }
        
        // Check for special character
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (!password.any { it in specialChars }) {
            errors.add("At least one special character required (!@#$%^&*)")
        } else {
            score += 1
        }
        
        // Additional scoring for length
        if (password.length >= 12) score += 1
        if (password.length >= 16) score += 1
        
        // Determine strength
        val strength = when {
            score >= 6 -> PasswordStrength.STRONG
            score >= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            strength = strength,
            errors = errors,
            score = score
        )
    }
    
    /**
     * Sets up real-time password validation with strength indicator
     */
    fun setupPasswordValidation(
        context: Context,
        passwordInput: TextInputEditText,
        passwordLayout: TextInputLayout,
        strengthIndicator: TextView? = null
    ) {
        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString() ?: ""
                val result = validatePassword(password)
                
                // Update error message
                if (password.isNotEmpty() && !result.isValid) {
                    passwordLayout.error = result.errors.firstOrNull()
                } else {
                    passwordLayout.error = null
                }
                
                // Update strength indicator
                strengthIndicator?.let { indicator ->
                    if (password.isNotEmpty()) {
                        indicator.visibility = View.VISIBLE
                        indicator.text = "Password strength: ${result.strength.label}"
                        indicator.setTextColor(ContextCompat.getColor(context, result.strength.color))
                    } else {
                        indicator.visibility = View.GONE
                    }
                }
            }
        })
    }
    
    /**
     * Validates password and shows all errors
     */
    fun validateAndShowErrors(
        passwordInput: TextInputEditText,
        passwordLayout: TextInputLayout
    ): Boolean {
        val password = passwordInput.text?.toString() ?: ""
        val result = validatePassword(password)
        
        if (!result.isValid) {
            passwordLayout.error = result.errors.joinToString("\n")
            return false
        }
        
        passwordLayout.error = null
        return true
    }
}
