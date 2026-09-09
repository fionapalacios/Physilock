package com.example.physi_lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.model.Account
import com.example.physi_lock.data.model.Role
import com.example.physi_lock.ui.admin.AdminHomeScreen
import com.example.physi_lock.ui.auth.AuthFormState
import com.example.physi_lock.ui.auth.AuthViewModel
import com.example.physi_lock.ui.auth.CreateAccountScreen
import com.example.physi_lock.ui.auth.ForgotPasswordScreen
import com.example.physi_lock.ui.auth.LoginScreen
import com.example.physi_lock.ui.auth.VerifyEmailScreen
import com.example.physi_lock.ui.landing.LandingScreen
import com.example.physi_lock.ui.navigation.NavGraph
import com.example.physi_lock.ui.onboarding.OnboardingScreen
import com.example.physi_lock.ui.settings.SettingsViewModel
import com.example.physi_lock.ui.theme.PhysiLockTheme
import kotlinx.coroutines.launch

// Prototype-testing flow: Landing -> Auth (Login/Register, Register includes a real Student/Work
// Usage Mode picker) -> [Verify Email, if unverified] -> Onboarding -> Home/AdminHome. Verify
// Email always runs on fresh email/password registration, and is re-checked (via
// proceedPastAuth below) on every subsequent login, Google sign-in, and session-resume, so a
// user who backs out of it once can't reach Home permanently on the same unverified account.
// Google sign-in accounts are always pre-verified by Firebase, so they pass straight through.
private enum class AppStage {
    LANDING, AUTH_LOGIN, AUTH_REGISTER, FORGOT_PASSWORD, VERIFY_EMAIL, ONBOARDING, HOME, ADMIN_HOME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var stage by rememberSaveable { mutableStateOf(AppStage.LANDING) }
            var authError by remember { mutableStateOf<String?>(null) }
            var currentAccount by remember { mutableStateOf<Account?>(null) }
            var sessionChecked by rememberSaveable { mutableStateOf(false) }
            val authViewModel: AuthViewModel = viewModel()
            val settingsViewModel: SettingsViewModel = viewModel()
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            var pendingVerificationEmail by remember { mutableStateOf("") }
            var verifyEmailChecking by remember { mutableStateOf(false) }
            var verifyEmailError by remember { mutableStateOf<String?>(null) }
            var passwordResetSending by remember { mutableStateOf(false) }
            var passwordResetSent by remember { mutableStateOf(false) }
            var passwordResetError by remember { mutableStateOf<String?>(null) }

            // Single post-auth gate used by every path that lands on an Account (interactive
            // login, Google sign-in, and session-resume below) so unverified users can't reach
            // Home by any route -- closes the bug where VerifyEmailScreen's back button logged
            // the user out, but logging back in with the same unverified account skipped the
            // check entirely and reached Home permanently. Admin accounts are exempt (they're
            // provisioned directly in the console, not through this email/password flow, and
            // already skip Onboarding). Fails open (treats as verified) if the check itself
            // can't run -- e.g. offline session-resume via the cached account, where there's no
            // network to ask Firebase and no cached verified-flag to fall back on; enforcement
            // only matters while online, which is exactly when the check can succeed.
            suspend fun proceedPastAuth(account: Account, verifiedStage: AppStage) {
                currentAccount = account
                if (account.role == Role.ADMIN) {
                    stage = AppStage.ADMIN_HOME
                    return
                }
                val verified = try {
                    authViewModel.checkEmailVerified()
                } catch (e: Exception) {
                    true
                }
                stage = if (verified) {
                    verifiedStage
                } else {
                    pendingVerificationEmail = account.email
                    verifyEmailError = null
                    AppStage.VERIFY_EMAIL
                }
            }

            // Persistent login: Firebase Auth keeps its session on disk independently of this
            // screen's own (rememberSaveable) stage — closing/swiping away the app and
            // reopening it otherwise always restarted at Landing, forcing a re-login every
            // time. Runs once per cold start; only redirects if nothing else has already
            // navigated (stage still LANDING) so it can't hijack an in-progress flow.
            LaunchedEffect(Unit) {
                if (!sessionChecked && stage == AppStage.LANDING) {
                    val account = try {
                        authViewModel.getCurrentAccount()
                    } catch (e: Exception) {
                        null
                    }
                    if (account != null && stage == AppStage.LANDING) {
                        proceedPastAuth(account, AppStage.HOME)
                    }
                }
                sessionChecked = true
            }

            PhysiLockTheme {
                if (!sessionChecked) {
                    SessionCheckSplash()
                    return@PhysiLockTheme
                }
                when (stage) {
                    AppStage.LANDING -> LandingScreen(
                        onGetStarted = {
                            authError = null
                            stage = AppStage.AUTH_REGISTER
                        },
                        onSignIn = {
                            authError = null
                            stage = AppStage.AUTH_LOGIN
                        }
                    )
                    AppStage.AUTH_LOGIN -> LoginScreen(
                        errorMessage = authError,
                        onLogin = { identifier, password ->
                            coroutineScope.launch {
                                try {
                                    val account = authViewModel.login(identifier, password)
                                    if (account == null) {
                                        authError = "Invalid username/email or password."
                                    } else {
                                        authError = null
                                        proceedPastAuth(account, AppStage.ONBOARDING)
                                    }
                                } catch (e: Exception) {
                                    authError = e.message ?: "Sign in failed."
                                }
                            }
                        },
                        onNavigateToForgotPassword = {
                            passwordResetSent = false
                            passwordResetError = null
                            stage = AppStage.FORGOT_PASSWORD
                        },
                        onGoogleSignIn = {
                            coroutineScope.launch {
                                try {
                                    val account = authViewModel.signInWithGoogle(context)
                                    if (account == null) {
                                        authError = "Google sign-in failed."
                                    } else {
                                        authError = null
                                        proceedPastAuth(account, AppStage.ONBOARDING)
                                    }
                                } catch (e: GetCredentialCancellationException) {
                                    // User dismissed the account picker — not an error, no banner.
                                } catch (e: GetCredentialException) {
                                    authError = e.message ?: "Google sign-in failed."
                                } catch (e: Exception) {
                                    authError = e.message ?: "Google sign-in failed."
                                }
                            }
                        },
                        onNavigateToRegister = {
                            authError = null
                            stage = AppStage.AUTH_REGISTER
                        },
                        onBackClick = {
                            authError = null
                            stage = AppStage.LANDING
                        }
                    )
                    AppStage.AUTH_REGISTER -> CreateAccountScreen(
                        errorMessage = authError,
                        onCreateAccount = { form: AuthFormState ->
                            coroutineScope.launch {
                                try {
                                    val account = authViewModel.register(form)
                                    if (account == null) {
                                        authError = "That username or email is already registered."
                                    } else {
                                        authError = null
                                        currentAccount = account
                                        settingsViewModel.setUserMode(form.usageMode)
                                        pendingVerificationEmail = account.email
                                        verifyEmailError = null
                                        stage = AppStage.VERIFY_EMAIL
                                    }
                                } catch (e: Exception) {
                                    authError = e.message ?: "Registration failed."
                                }
                            }
                        },
                        onGoogleSignIn = {
                            coroutineScope.launch {
                                try {
                                    val account = authViewModel.signInWithGoogle(context)
                                    if (account == null) {
                                        authError = "Google sign-in failed."
                                    } else {
                                        authError = null
                                        proceedPastAuth(account, AppStage.ONBOARDING)
                                    }
                                } catch (e: GetCredentialCancellationException) {
                                    // User dismissed the account picker — not an error, no banner.
                                } catch (e: GetCredentialException) {
                                    authError = e.message ?: "Google sign-in failed."
                                } catch (e: Exception) {
                                    authError = e.message ?: "Google sign-in failed."
                                }
                            }
                        },
                        onNavigateToLogin = {
                            authError = null
                            stage = AppStage.AUTH_LOGIN
                        },
                        onBackClick = {
                            authError = null
                            stage = AppStage.LANDING
                        }
                    )
                    AppStage.FORGOT_PASSWORD -> ForgotPasswordScreen(
                        sending = passwordResetSending,
                        sent = passwordResetSent,
                        errorMessage = passwordResetError,
                        onSendCode = { email ->
                            coroutineScope.launch {
                                passwordResetSending = true
                                passwordResetError = null
                                try {
                                    authViewModel.sendPasswordReset(email)
                                    passwordResetSent = true
                                } catch (e: Exception) {
                                    passwordResetError = e.message ?: "Couldn't send reset email."
                                } finally {
                                    passwordResetSending = false
                                }
                            }
                        },
                        onBackClick = {
                            passwordResetSent = false
                            passwordResetError = null
                            stage = AppStage.AUTH_LOGIN
                        }
                    )
                    AppStage.VERIFY_EMAIL -> VerifyEmailScreen(
                        email = pendingVerificationEmail,
                        checking = verifyEmailChecking,
                        verificationError = verifyEmailError,
                        onCheckVerified = {
                            coroutineScope.launch {
                                verifyEmailChecking = true
                                verifyEmailError = null
                                try {
                                    if (authViewModel.checkEmailVerified()) {
                                        stage = AppStage.ONBOARDING
                                    } else {
                                        verifyEmailError = "Still not verified — open the email and tap the link, then try again."
                                    }
                                } catch (e: Exception) {
                                    verifyEmailError = e.message ?: "Couldn't check verification status."
                                } finally {
                                    verifyEmailChecking = false
                                }
                            }
                        },
                        onResendCode = {
                            coroutineScope.launch {
                                try {
                                    authViewModel.resendVerificationEmail()
                                } catch (e: Exception) {
                                    verifyEmailError = e.message ?: "Couldn't resend the email."
                                }
                            }
                        },
                        onBackClick = {
                            coroutineScope.launch {
                                authViewModel.logout(context)
                                currentAccount = null
                                verifyEmailError = null
                                stage = AppStage.AUTH_LOGIN
                            }
                        }
                    )
                    AppStage.ONBOARDING -> OnboardingScreen(onFinished = { stage = AppStage.HOME })
                    AppStage.HOME -> NavGraph(
                        currentAccount = currentAccount,
                        onAccountUpdated = { updatedAccount -> currentAccount = updatedAccount },
                        onLogout = {
                            coroutineScope.launch {
                                authViewModel.logout(context)
                                currentAccount = null
                                authError = null
                                stage = AppStage.AUTH_LOGIN
                            }
                        }
                    )
                    AppStage.ADMIN_HOME -> AdminHomeScreen(
                        onLogout = {
                            coroutineScope.launch {
                                authViewModel.logout(context)
                                currentAccount = null
                                authError = null
                                stage = AppStage.AUTH_LOGIN
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Shown only while the persistent-login check (see the LaunchedEffect above) is in flight. */
@Composable
private fun SessionCheckSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.physi_lock.ui.theme.BackgroundLight),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Physi-Lock logo",
            modifier = Modifier.size(96.dp)
        )
    }
}
