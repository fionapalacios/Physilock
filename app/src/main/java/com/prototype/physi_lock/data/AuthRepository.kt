package com.prototype.physi_lock.data

import android.util.Patterns

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val message: String) : AuthResult
}

/**
 * In-memory mock account store. Stands in for a real backend so the sign-in/sign-up
 * flows have something concrete to validate against until authentication is wired up.
 */
object AuthRepository {
    private val accounts = mutableMapOf("demo@physilock.com" to "password123")

    fun register(name: String, email: String, password: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            return AuthResult.Failure("Please fill in all fields.")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return AuthResult.Failure("Please enter a valid email address.")
        }
        if (password.length < 8) {
            return AuthResult.Failure("Password must be at least 8 characters.")
        }
        if (accounts.containsKey(normalizedEmail)) {
            return AuthResult.Failure("An account with this email already exists.")
        }
        accounts[normalizedEmail] = password
        return AuthResult.Success
    }

    fun signIn(email: String, password: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Failure("Please enter your email and password.")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return AuthResult.Failure("Please enter a valid email address.")
        }
        val storedPassword = accounts[normalizedEmail]
            ?: return AuthResult.Failure("Invalid email or password. Please try again.")
        if (storedPassword != password) {
            return AuthResult.Failure("Invalid email or password. Please try again.")
        }
        return AuthResult.Success
    }
}
