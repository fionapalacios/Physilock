package com.example.physi_lock.data.auth

import android.content.SharedPreferences
import com.example.physi_lock.data.model.Account
import com.example.physi_lock.data.entity.CachedAccount
import com.example.physi_lock.data.dao.CachedAccountDao
import com.example.physi_lock.data.model.Role
import com.example.physi_lock.data.entity.toAccount
import com.example.physi_lock.ui.auth.AuthFormState
import java.util.UUID

/**
 * Fully offline stand-in for [FirebaseAccountRepository], used while no real
 * `google-services.json` backend is configured (see PhysiLockApplication's placeholder
 * Firebase init). Stores accounts in the same local Room cache Hybrid...Repository normally
 * falls back to when Firebase is unreachable, plus a SharedPreferences-backed "session" so
 * login persists across app relaunches.
 *
 * Delete this class and point AuthViewModel back at
 * `HybridAccountRepository(FirebaseAccountRepository())` once a real backend is wired up.
 */
class DemoAccountRepository(
    private val cache: CachedAccountDao,
    private val prefs: SharedPreferences
) : AccountRepository {

    /**
     * Idempotently provisions a fixed demo admin account (username [DEMO_ADMIN_USERNAME],
     * password [DEMO_ADMIN_PASSWORD]) so the admin screens can be reached without a real
     * backend to promote an account. Safe to call on every AuthViewModel construction —
     * `REPLACE`-upserts the same row, so it never duplicates or drifts.
     */
    suspend fun seedDemoAdminIfNeeded() {
        val salt = PasswordHasher.newSalt()
        val account = Account(
            id = DEMO_ADMIN_ID,
            username = DEMO_ADMIN_USERNAME,
            fullName = "Demo Admin",
            email = DEMO_ADMIN_EMAIL,
            occupation = null,
            role = Role.ADMIN,
            createdAt = 0L,
            isActive = true
        )
        cache.upsert(account.toCached(PasswordHasher.hash(DEMO_ADMIN_PASSWORD, salt), salt))
    }

    override suspend fun register(form: AuthFormState): Account? {
        if (cache.findByIdentifier(form.username) != null) return null
        if (cache.findByIdentifier(form.identifier) != null) return null

        val salt = PasswordHasher.newSalt()
        val account = Account(
            id = UUID.randomUUID().toString(),
            username = form.username,
            fullName = form.fullName,
            email = form.identifier,
            occupation = null,
            role = Role.USER,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        cache.upsert(account.toCached(PasswordHasher.hash(form.password, salt), salt))
        setSession(account.id)
        return account
    }

    override suspend fun login(identifier: String, password: String): Account? {
        val cached = cache.findByIdentifier(identifier) ?: return null
        if (!cached.isActive) return null
        if (!PasswordHasher.matches(password, cached.passwordSalt, cached.passwordHash)) return null
        setSession(cached.id)
        return cached.toAccount()
    }

    override suspend fun updateAccount(account: Account): Account? {
        val existing = cache.findById(account.id) ?: return null
        val conflict = cache.findByIdentifier(account.username)
        if (conflict != null && conflict.id != account.id) return null
        cache.upsert(account.toCached(existing.passwordHash, existing.passwordSalt))
        return account
    }

    override fun logout() {
        prefs.edit().remove(KEY_SESSION_UID).apply()
    }

    override fun currentUserId(): String? = prefs.getString(KEY_SESSION_UID, null)

    override suspend fun getCurrentAccount(): Account? {
        val uid = currentUserId() ?: return null
        val cached = cache.findById(uid) ?: return null
        if (!cached.isActive) return null
        return cached.toAccount()
    }

    private fun setSession(uid: String) {
        prefs.edit().putString(KEY_SESSION_UID, uid).apply()
    }

    private fun Account.toCached(passwordHash: String, passwordSalt: String) = CachedAccount(
        id = id,
        username = username,
        fullName = fullName,
        email = email,
        occupation = occupation,
        role = role,
        createdAt = createdAt,
        isActive = isActive,
        passwordHash = passwordHash,
        passwordSalt = passwordSalt,
        lastSyncedAt = System.currentTimeMillis()
    )

    companion object {
        private const val KEY_SESSION_UID = "demo_session_uid"
        private const val DEMO_ADMIN_ID = "demo-admin"
        const val DEMO_ADMIN_USERNAME = "admin"
        const val DEMO_ADMIN_EMAIL = "admin@demo.physilock"
        const val DEMO_ADMIN_PASSWORD = "admin123"
    }
}
