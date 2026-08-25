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
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.Role
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
// Usage Mode picker) -> [Verify Email, register-only] -> Onboarding -> Home/AdminHome. Verify
// Email only runs on a fresh email/password registration (see project memory
// "project-auth-screens"); login and Google sign-in skip straight from Onboarding to Home.
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
                        currentAccount = account
                        stage = if (account.role == Role.ADMIN) AppStage.ADMIN_HOME else AppStage.HOME
                    }
                }
                sessionChecked = true
            }

            var pendingVerificationEmail by remember { mutableStateOf("") }
            var verifyEmailChecking by remember { mutableStateOf(false) }
            var verifyEmailError by remember { mutableStateOf<String?>(null) }
            var passwordResetSending by remember { mutableStateOf(false) }
            var passwordResetSent by remember { mutableStateOf(false) }
            var passwordResetError by remember { mutableStateOf<String?>(null) }

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
                                        currentAccount = account
                                        stage = if (account.role == Role.ADMIN) AppStage.ADMIN_HOME else AppStage.ONBOARDING
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
                                        currentAccount = account
                                        stage = if (account.role == Role.ADMIN) AppStage.ADMIN_HOME else AppStage.ONBOARDING
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
                                        currentAccount = account
                                        stage = if (account.role == Role.ADMIN) AppStage.ADMIN_HOME else AppStage.ONBOARDING
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
                    AppStage.ADMIN_HOME -> AdminHomeScreen()
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
