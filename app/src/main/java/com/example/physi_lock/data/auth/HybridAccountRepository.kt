package com.example.physi_lock.data.auth

import com.example.physi_lock.data.Account
import com.example.physi_lock.data.CachedAccount
import com.example.physi_lock.data.CachedAccountDao
import com.example.physi_lock.ui.auth.AuthFormState

/**
 * Login-with-offline-fallback wrapper around [FirebaseAccountRepository].
 *
 * Every successful online login/registration writes a hashed-password snapshot to
 * [cache] (Room). If a later login call throws (Firebase/Firestore unreachable), it
 * falls back to verifying the identifier/password against that local snapshot.
 *
 * Registration and profile updates always require Firebase — there's no server to
 * reserve a username/email or reconcile a conflicting edit against while offline.
 */
class HybridAccountRepository(
    private val remote: AccountRepository = FirebaseAccountRepository(),
    private val cache: CachedAccountDao
) : AccountRepository {

    override suspend fun register(form: AuthFormState): Account? {
        val account = remote.register(form) ?: return null
        cacheAccount(account, form.password)
        return account
    }

    override suspend fun login(identifier: String, password: String): Account? {
        return try {
            val account = remote.login(identifier, password)
            if (account != null) cacheAccount(account, password)
            account
        } catch (e: Exception) {
            loginOffline(identifier, password)
        }
    }

    override suspend fun updateAccount(account: Account): Account? =
        remote.updateAccount(account)

    // Purely local (clears the Firebase SDK's session token) — no offline handling needed.
    override fun logout() = remote.logout()

    override fun currentUserId(): String? = remote.currentUserId()

    /**
     * Auto-resume-session lookup (app relaunch with no re-login) — same offline-fallback
     * shape as [login]: try Firestore first, fall back to the local cache (keyed by uid,
     * since that's all a locally-persisted Firebase Auth session gives us with no network)
     * if that throws.
     */
    override suspend fun getCurrentAccount(): Account? {
        return try {
            remote.getCurrentAccount()
        } catch (e: Exception) {
            val uid = remote.currentUserId() ?: return null
            val cached = cache.findById(uid) ?: return null
            if (!cached.isActive) return null
            cached.toAccount()
        }
    }

    private suspend fun loginOffline(identifier: String, password: String): Account? {
        val cached = cache.findByIdentifier(identifier) ?: return null
        if (!cached.isActive) return null
        if (!PasswordHasher.matches(password, cached.passwordSalt, cached.passwordHash)) return null
        return cached.toAccount()
    }

    private suspend fun cacheAccount(account: Account, password: String) {
        val salt = PasswordHasher.newSalt()
        cache.upsert(
            CachedAccount(
                id = account.id,
                username = account.username,
                fullName = account.fullName,
                email = account.email,
                occupation = account.occupation,
                role = account.role,
                createdAt = account.createdAt,
                isActive = account.isActive,
                passwordHash = PasswordHasher.hash(password, salt),
                passwordSalt = salt,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }

    private fun CachedAccount.toAccount() = Account(
        id = id,
        username = username,
        fullName = fullName,
        email = email,
        occupation = occupation,
        role = role,
        createdAt = createdAt,
        isActive = isActive
    )
}
