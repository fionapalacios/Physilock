package com.example.physi_lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.Role
import com.example.physi_lock.ui.admin.AdminHomeScreen
import com.example.physi_lock.ui.auth.AuthScreen
import com.example.physi_lock.ui.auth.AuthViewModel
import com.example.physi_lock.ui.landing.LandingScreen
import com.example.physi_lock.ui.navigation.NavGraph
import com.example.physi_lock.ui.onboarding.OnboardingScreen
import com.example.physi_lock.ui.theme.PhysiLockTheme
import kotlinx.coroutines.launch

// Prototype-testing flow: Landing -> Auth -> Onboarding (User only) -> Home/AdminHome,
// skipping email verification / mode picker for now (see project memory
// "project-auth-screens" for the full intended flow to wire in later).
private enum class AppStage { LANDING, AUTH, ONBOARDING, HOME, ADMIN_HOME }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var stage by rememberSaveable { mutableStateOf(AppStage.LANDING) }
            var authError by remember { mutableStateOf<String?>(null) }
            var currentAccount by remember { mutableStateOf<Account?>(null) }
            val authViewModel: AuthViewModel = viewModel()
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            PhysiLockTheme {
                when (stage) {
                    AppStage.LANDING -> LandingScreen(onGetStarted = { stage = AppStage.AUTH })
                    AppStage.AUTH -> AuthScreen(
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
                        onRegister = { form ->
                            coroutineScope.launch {
                                try {
                                    val account = authViewModel.register(form)
                                    if (account == null) {
                                        authError = "That username or email is already registered."
                                    } else {
                                        authError = null
                                        currentAccount = account
                                        stage = AppStage.ONBOARDING
                                    }
                                } catch (e: Exception) {
                                    authError = e.message ?: "Registration failed."
                                }
                            }
                        },
                        onForgotPassword = { /* not wired yet */ },
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
                        onBack = {
                            authError = null
                            stage = AppStage.LANDING
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
                                stage = AppStage.AUTH
                            }
                        }
                    )
                    AppStage.ADMIN_HOME -> AdminHomeScreen()
                }
            }
        }
    }
}
