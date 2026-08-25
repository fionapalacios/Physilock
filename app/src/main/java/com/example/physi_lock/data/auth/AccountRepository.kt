package com.example.physi_lock.data.auth

import com.example.physi_lock.data.Account
import com.example.physi_lock.ui.auth.AuthFormState

/**
 * Abstraction over account storage, implemented by [FirebaseAccountRepository] against
 * Firebase Auth (credentials) + Firestore `users/{uid}` (profile: username, fullName,
 * email, occupation, role, createdAt, isActive).
 */
interface AccountRepository {
    /** Returns the created account, or null if the username/email is already taken. */
    suspend fun register(form: AuthFormState): Account?

    /** Returns the matching account, or null if the identifier/password don't match. */
    suspend fun login(identifier: String, password: String): Account?

    /** Persists profile edits for the given account, or null if the update conflicts. */
    suspend fun updateAccount(account: Account): Account?

    /** Ends the current Firebase Auth session. Purely local — works offline. */
    fun logout()

    /** Firebase Auth's locally-persisted current session uid, or null if none — purely local, no network. */
    fun currentUserId(): String?

    /** Resolves the account for the current persisted session (app relaunch, no re-login), or null if none/inactive. */
    suspend fun getCurrentAccount(): Account?
}
