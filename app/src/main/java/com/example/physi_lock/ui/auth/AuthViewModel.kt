package com.example.physi_lock.ui.auth

import android.app.Application
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.AndroidViewModel
import com.example.physi_lock.R
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.auth.FirebaseAccountRepository
import com.example.physi_lock.data.auth.HybridAccountRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class AuthFormState(
    val username: String = "",
    val fullName: String = "",
    val identifier: String = "", // login: username or email · register: email
    val password: String = "",
    val confirmPassword: String = "",
    val usageMode: String = "STUDENT_MODE" // "STUDENT_MODE" or "WORK_MODE" — see UserConfiguration
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    // Google sign-in is inherently online-only (Credential Manager needs the network),
    // so it talks to Firebase directly rather than through the offline-fallback wrapper.
    private val firebaseRepository = FirebaseAccountRepository()
    private val repository = HybridAccountRepository(
        remote = firebaseRepository,
        cache = PhysiLockDatabase.getInstance(application).cachedAccountDao()
    )

    suspend fun login(identifier: String, password: String): Account? =
        repository.login(identifier, password)

    suspend fun register(form: AuthFormState): Account? =
        repository.register(form)

    /** Firebase's native link-based reset flow — throws on failure (network/invalid email). */
    suspend fun sendPasswordReset(email: String) = firebaseRepository.sendPasswordResetEmail(email)

    suspend fun resendVerificationEmail() = firebaseRepository.sendEmailVerification()

    /** Re-checks the current Firebase user's verified state against the server (post link-click). */
    suspend fun checkEmailVerified(): Boolean {
        firebaseRepository.reloadCurrentUser()
        return firebaseRepository.isCurrentUserEmailVerified()
    }

    /**
     * Ends the Firebase session. [context] is optional — pass it (an Activity context)
     * to also clear Credential Manager's cached Google account state, so the picker
     * doesn't silently auto-resume the same account on the next sign-in attempt.
     */
    suspend fun logout(context: Context? = null) {
        repository.logout()
        if (context != null) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                // Best-effort — the Firebase sign-out above is what actually ends the session.
            }
        }
    }

    /** [context] must be an Activity context — required by Credential Manager's UI. */
    suspend fun signInWithGoogle(context: Context): Account? {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            return firebaseRepository.signInWithGoogleIdToken(
                idToken = googleIdTokenCredential.idToken,
                displayName = googleIdTokenCredential.displayName,
                email = googleIdTokenCredential.id
            )
        }
        return null
    }
}
