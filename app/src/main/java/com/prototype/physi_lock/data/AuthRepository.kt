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
    private val usernames = mutableSetOf<String>()

    fun register(username: String, name: String, email: String, password: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        val normalizedUsername = username.trim().lowercase()
        if (username.isBlank() || name.isBlank() || email.isBlank() || password.isBlank()) {
            return AuthResult.Failure("Please fill in all fields.")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return AuthResult.Failure("Please enter a valid email address.")
        }
        if (password.length < 8 || password.none { it.isUpperCase() } || password.none { it.isDigit() }) {
            return AuthResult.Failure("Password must be at least 8 characters and include an uppercase letter and a number.")
        }
        if (usernames.contains(normalizedUsername)) {
            return AuthResult.Failure("This username is already taken.")
        }
        if (accounts.containsKey(normalizedEmail)) {
            return AuthResult.Failure("An account with this email already exists.")
        }
        usernames.add(normalizedUsername)
        accounts[normalizedEmail] = password
        return AuthResult.Success
    }

    fun verifyEmailCode(code: String): AuthResult {
        if (code.length != 6 || code.any { !it.isDigit() }) {
            return AuthResult.Failure("Enter the 6-digit code we sent to your email.")
        }
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
