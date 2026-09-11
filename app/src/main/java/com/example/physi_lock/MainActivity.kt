package com.example.physi_lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Olive
import com.example.physi_lock.ui.theme.PhysiLockTheme
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
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

/** Shown only while the persistent-login check (see the LaunchedEffect above) is in flight --
 *  real design pasted by the user (2026-09-10), ported from a React/motion mockup to Compose.
 *  Unlike the mockup's own fixed ~1.6s progress fill (which drove its own onDone callback),
 *  this app's actual "done" signal is the real async session check above (sessionChecked),
 *  not a fixed timer -- so the progress bar loops indefinitely as a generic in-progress
 *  indicator instead of a one-shot fill tied to a duration unrelated to real completion. */
@Composable
private fun SessionCheckSplash() {
    val logoScale = remember { Animatable(0.72f) }
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(400))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splashLoading")
    val progressPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "splashProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SecondarySage, Color(0xFFE8E4D0), CardCream))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Physi-Lock logo",
                modifier = Modifier
                    .size(96.dp)
                    .scale(logoScale.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha.value)
            ) {
                Text(
                    text = "Physi-Lock",
                    fontFamily = Nunito,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepOlive
                )
                Text(
                    text = "MOVE · FOCUS · RECHARGE",
                    fontFamily = DmMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .background(DeepOlive.copy(alpha = 0.14f), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPhase)
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(Olive, SageAccent)), RoundedCornerShape(50))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1100, delayMillis = index * 180, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "splashDot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .alpha(dotAlpha)
                            .background(SageAccent, CircleShape)
                    )
                }
            }
        }

        Text(
            text = "\"Focus on being productive instead of busy.\" — Tim Ferriss",
            fontFamily = DmMono,
            fontSize = 11.sp,
            color = MutedText.copy(alpha = 0.55f),
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp)
                .padding(bottom = 28.dp)
        )
    }
}
