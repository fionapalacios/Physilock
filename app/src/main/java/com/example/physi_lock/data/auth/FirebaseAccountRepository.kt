package com.example.physi_lock.data.auth

import com.example.physi_lock.data.model.Account
import com.example.physi_lock.data.model.Role
import com.example.physi_lock.ui.auth.AuthFormState
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Change Password (Settings > Edit Profile): distinguishes the specific failure so
 *  EditProfileScreen can show an accurate message instead of a generic "something went
 *  wrong" -- see [FirebaseAccountRepository.changePassword]. */
sealed class ChangePasswordResult {
    data object Success : ChangePasswordResult()
    data object WrongCurrentPassword : ChangePasswordResult()
    data object NoPasswordProvider : ChangePasswordResult()
    data class Error(val message: String) : ChangePasswordResult()
}

/** Delete Account (Settings): distinguishes the specific failure so SettingsScreen can show
 *  an accurate message -- see [FirebaseAccountRepository.deleteAccount]. Mirrors
 *  [ChangePasswordResult]'s shape. */
sealed class DeleteAccountResult {
    data object Success : DeleteAccountResult()
    data object WrongPassword : DeleteAccountResult()
    /** Google-linked account with no fresh ID token supplied yet -- caller should trigger
     *  a Google reauth prompt and retry with the resulting token. */
    data object NeedsGoogleReauth : DeleteAccountResult()
    data class Error(val message: String) : DeleteAccountResult()
}

class FirebaseAccountRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AccountRepository {

    private val usersCollection get() = firestore.collection("users")

    // Public username -> email lookup. Used to resolve a username to an email before the
    // client is authenticated (login by username), and also as the uniqueness check for
    // register/update/suggestUsername below — `/users` no longer allows `list` for
    // non-admins (see firestore.rules), so those checks can't query `users` by username
    // anymore and use this exact-key lookup instead. Deliberately minimal: exposes nothing
    // but email, keyed by exact username (no `list`/enumeration allowed by the rules).
    private val usernamesCollection get() = firestore.collection("usernames")

    override suspend fun register(form: AuthFormState): Account? {
        // Firebase Auth runs before any Firestore access — Firestore rules scope reads/writes
        // to signed-in users, and there's no account to be signed in as until this succeeds.
        val authResult = try {
            auth.createUserWithEmailAndPassword(form.identifier, form.password).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            return null // email already registered
        }
        val uid = authResult.user?.uid ?: return null

        // Best-effort username uniqueness check — Firestore has no unique-field constraint
        // and Firebase Auth only enforces email uniqueness, so there's a small race window
        // between this read and the write below. Acceptable at this project's scale.
        val usernameTaken = usernamesCollection.document(form.username).get().await()
        if (usernameTaken.exists()) {
            authResult.user?.delete()?.await() // roll back the auth account we just created
            return null
        }

        // Best-effort — a failed send shouldn't fail registration itself; VerifyEmailScreen's
        // "Resend email" gives the user another chance.
        try {
            authResult.user?.sendEmailVerification()?.await()
        } catch (e: Exception) {
            // Ignored — see kdoc above.
        }

        val account = Account(
            id = uid,
            username = form.username,
            fullName = form.fullName,
            email = form.identifier,
            occupation = null,
            role = Role.USER,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        usersCollection.document(uid).set(account).await()
        putUsernameLookup(form.username, form.identifier)
        return account
    }

    override suspend fun login(identifier: String, password: String): Account? {
        val email = resolveEmail(identifier) ?: return null

        val authResult = try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: FirebaseAuthException) {
            return null // no match / wrong password
        }
        val uid = authResult.user?.uid ?: return null

        val account = fetchAccount(uid)
        if (account == null || !account.isActive) {
            auth.signOut()
            return null
        }
        return account
    }

    override fun logout() = auth.signOut()

    override fun currentUserId(): String? = auth.currentUser?.uid

    override suspend fun getCurrentAccount(): Account? {
        val uid = currentUserId() ?: return null
        val account = fetchAccount(uid) ?: return null
        if (!account.isActive) {
            auth.signOut()
            return null
        }
        return account
    }

    /**
     * Firebase's native link-based reset flow — the user gets an email with a link to
     * Firebase's own hosted reset page, no in-app code entry needed. Throws on failure
     * (e.g. malformed email, network error); the caller decides what to show.
     */
    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    /** Sends Firebase's native link-based verification email to the just-registered user. */
    suspend fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    /** Refreshes [auth]'s cached user so [isCurrentUserEmailVerified] reflects a just-clicked link. */
    suspend fun reloadCurrentUser() {
        auth.currentUser?.reload()?.await()
    }

    fun isCurrentUserEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    /** True only for accounts that actually have a Firebase email/password credential --
     *  Google-only accounts have nothing for "Change Password" to change. */
    fun hasPasswordProvider(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } ?: false

    /**
     * Real Firebase reauth flow (Settings > Edit Profile > Change Password): Firebase
     * requires a recent sign-in before a sensitive operation like updatePassword(), so this
     * reauthenticates with the current password first, then updates. Distinguishes a wrong
     * current password from other failures so the UI can say which.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): ChangePasswordResult {
        val user = auth.currentUser ?: return ChangePasswordResult.Error("Not signed in")
        val email = user.email
        if (email == null || !hasPasswordProvider()) return ChangePasswordResult.NoPasswordProvider

        return try {
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            user.updatePassword(newPassword).await()
            ChangePasswordResult.Success
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            ChangePasswordResult.WrongCurrentPassword
        } catch (e: Exception) {
            ChangePasswordResult.Error(e.localizedMessage ?: "Couldn't change password")
        }
    }

    /**
     * Real Firebase account deletion (Settings > Delete Account). Firebase requires a recent
     * sign-in before this sensitive operation, same as [changePassword] -- password-based
     * accounts reauthenticate with [currentPassword]; Google-linked accounts (no password
     * provider) need a fresh [googleIdToken] instead, since there's no password to re-enter
     * (returns [DeleteAccountResult.NeedsGoogleReauth] if the caller hasn't supplied one yet,
     * so the UI can trigger the Google sign-in prompt and retry with the resulting token).
     * Cleans up the Firestore `users/{uid}` profile and the `usernames/{username}` lookup
     * doc as best-effort before deleting the Auth user itself -- a failed Firestore cleanup
     * doesn't block the deletion, since there's no recoverable account left either way once
     * the Auth user is gone.
     */
    suspend fun deleteAccount(currentPassword: String? = null, googleIdToken: String? = null): DeleteAccountResult {
        val user = auth.currentUser ?: return DeleteAccountResult.Error("Not signed in")

        return try {
            if (hasPasswordProvider()) {
                val email = user.email ?: return DeleteAccountResult.Error("No email on file")
                if (currentPassword == null) return DeleteAccountResult.Error("Password required")
                user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            } else {
                if (googleIdToken == null) return DeleteAccountResult.NeedsGoogleReauth
                user.reauthenticate(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
            }

            val uid = user.uid
            try {
                val account = fetchAccount(uid)
                usersCollection.document(uid).delete().await()
                account?.let { deleteUsernameLookup(it.username) }
            } catch (e: Exception) {
                // Best-effort -- see kdoc above.
            }

            user.delete().await()
            DeleteAccountResult.Success
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            DeleteAccountResult.WrongPassword
        } catch (e: Exception) {
            DeleteAccountResult.Error(e.localizedMessage ?: "Couldn't delete account")
        }
    }

    override suspend fun updateAccount(account: Account): Account? {
        val existingMapping = usernamesCollection.document(account.username).get().await()
        val conflict = existingMapping.exists() && existingMapping.getString("email") != account.email
        if (conflict) return null

        val previous = fetchAccount(account.id)
        usersCollection.document(account.id).set(account).await()
        if (previous != null && previous.username != account.username) {
            deleteUsernameLookup(previous.username)
            putUsernameLookup(account.username, account.email)
        }
        return account
    }

    /** Best-effort: a failed lookup write only affects username-based login, not the account itself. */
    private suspend fun putUsernameLookup(username: String, email: String) {
        try {
            usernamesCollection.document(username).set(mapOf("email" to email)).await()
        } catch (e: Exception) {
            // Ignored — see kdoc above.
        }
    }

    private suspend fun deleteUsernameLookup(username: String) {
        try {
            usernamesCollection.document(username).delete().await()
        } catch (e: Exception) {
            // Ignored — see kdoc above.
        }
    }

    /** Called by AuthViewModel after Credential Manager returns a Google ID token. */
    suspend fun signInWithGoogleIdToken(
        idToken: String,
        displayName: String?,
        email: String?
    ): Account? {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(firebaseCredential).await()
        val user = authResult.user ?: return null

        val existing = fetchAccount(user.uid)
        if (existing != null) {
            if (!existing.isActive) {
                auth.signOut()
                return null
            }
            return existing
        }

        // First Google sign-in for this UID: provision the Firestore profile.
        val resolvedEmail = email ?: user.email ?: ""
        val account = Account(
            id = user.uid,
            username = suggestUsername(resolvedEmail, user.uid),
            fullName = displayName ?: user.displayName.orEmpty(),
            email = resolvedEmail,
            occupation = null,
            role = Role.USER,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        usersCollection.document(user.uid).set(account).await()
        putUsernameLookup(account.username, account.email)
        return account
    }

    private suspend fun resolveEmail(identifier: String): String? {
        if (identifier.contains("@")) return identifier
        // Public get-by-exact-ID, not a query — works while unauthenticated (required,
        // since we don't have a session yet at this point in login) and can't be used
        // to enumerate usernames. See firestore.rules.
        val doc = usernamesCollection.document(identifier).get().await()
        return doc.getString("email")
    }

    private suspend fun fetchAccount(uid: String): Account? {
        val doc = usersCollection.document(uid).get().await()
        if (!doc.exists()) return null
        return doc.toObject(Account::class.java)?.copy(id = doc.id)
    }

    private suspend fun suggestUsername(email: String, uid: String): String {
        val base = email.substringBefore("@")
            .filter { it.isLetterOrDigit() }
            .ifBlank { "user" }
        val taken = usernamesCollection.document(base).get().await()
        return if (!taken.exists()) base else "$base${uid.takeLast(4)}"
    }
}
