package com.example.physi_lock.ui.auth

import android.app.Application
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.model.Account
import com.example.physi_lock.data.entity.LoginEvent
import com.example.physi_lock.data.dao.LoginEventDao
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.auth.DemoAccountRepository
import java.time.LocalDate
import kotlinx.coroutines.launch

data class AuthFormState(
    val username: String = "",
    val fullName: String = "",
    val identifier: String = "", // login: username or email · register: email
    val password: String = "",
    val confirmPassword: String = "",
    val usageMode: String = "STUDENT_MODE" // "STUDENT_MODE" or "WORK_MODE" — see UserConfiguration
)

/**
 * DEMO MODE: no real `google-services.json` is configured yet (see PhysiLockApplication's
 * placeholder Firebase init), so auth runs fully offline against the local Room cache via
 * [DemoAccountRepository] instead of hitting the (fake) Firebase project. Email verification,
 * password reset, and Google sign-in all require a real backend and are stubbed out below.
 *
 * Once a real backend is wired up, swap [repository] for
 * `HybridAccountRepository(FirebaseAccountRepository())` and restore the real implementations
 * of [sendPasswordReset], [resendVerificationEmail], [checkEmailVerified], and [signInWithGoogle]
 * (see FirebaseAccountRepository for the Firebase-backed versions).
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val loginEventDao: LoginEventDao = PhysiLockDatabase.getInstance(application).loginEventDao()
    private val repository = DemoAccountRepository(
        cache = PhysiLockDatabase.getInstance(application).cachedAccountDao(),
        prefs = application.getSharedPreferences("demo_auth", Context.MODE_PRIVATE)
    )

    init {
        // Lets Admin Console be reached in demo mode without a real backend to promote an
        // account to Role.ADMIN — sign in with DemoAccountRepository.DEMO_ADMIN_USERNAME /
        // DEMO_ADMIN_PASSWORD.
        viewModelScope.launch { repository.seedDemoAdminIfNeeded() }
    }

    // Backs Admin Analytics' "Daily Active Users" chart -- one row per account per calendar
    // day it was seen signed in. Insert is IGNORE-on-conflict (see LoginEventDao), so calling
    // this from every entry point below (fresh login, registration, or an already-signed-in
    // session just resuming on app launch) is safe without checking for an existing row first.
    private suspend fun recordDailyActive(account: Account?) {
        if (account == null) return
        loginEventDao.insert(
            LoginEvent(accountId = account.id, dateKey = LocalDate.now().toString(), timestamp = System.currentTimeMillis())
        )
    }

    suspend fun login(identifier: String, password: String): Account? =
        repository.login(identifier, password).also { recordDailyActive(it) }

    /** Resolves an already-signed-in session on app launch, so returning users skip Login entirely. */
    suspend fun getCurrentAccount(): Account? =
        repository.getCurrentAccount().also { recordDailyActive(it) }

    suspend fun register(form: AuthFormState): Account? =
        repository.register(form).also { recordDailyActive(it) }

    suspend fun sendPasswordReset(email: String): Unit =
        throw IllegalStateException("Password reset isn't available in demo mode.")

    /** No-op in demo mode — demo accounts are treated as already verified, see [checkEmailVerified]. */
    suspend fun resendVerificationEmail() {}

    /** Demo accounts skip real email verification entirely. */
    suspend fun checkEmailVerified(): Boolean = true

    /**
     * Ends the demo session. [context] is optional — pass it (an Activity context) to also
     * clear Credential Manager's cached Google account state left over from a real backend.
     */
    suspend fun logout(context: Context? = null) {
        repository.logout()
        if (context != null) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                // Best-effort — the demo sign-out above is what actually ends the session.
            }
        }
    }

    suspend fun signInWithGoogle(context: Context): Account? =
        throw IllegalStateException("Google sign-in isn't available in demo mode.")
}
