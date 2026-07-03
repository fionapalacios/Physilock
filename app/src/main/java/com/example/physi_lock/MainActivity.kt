package com.example.physi_lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.physi_lock.ui.auth.AuthScreen
import com.example.physi_lock.ui.landing.LandingScreen
import com.example.physi_lock.ui.navigation.NavGraph
import com.example.physi_lock.ui.theme.PhysiLockTheme

// Prototype-testing flow: Landing -> Auth -> Home, skipping email verification /
// permission modal / mode picker for now (no backend yet — see project memory
// "project-auth-screens" for the full intended flow to wire in later).
private enum class AppStage { LANDING, AUTH, HOME }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var stage by remember { mutableStateOf(AppStage.LANDING) }

            PhysiLockTheme {
                when (stage) {
                    AppStage.LANDING -> LandingScreen(onGetStarted = { stage = AppStage.AUTH })
                    AppStage.AUTH -> AuthScreen(
                        onLogin = { _, _ -> stage = AppStage.HOME },
                        onRegister = { stage = AppStage.HOME },
                        onForgotPassword = { /* not wired yet */ },
                        onBack = { stage = AppStage.LANDING }
                    )
                    AppStage.HOME -> NavGraph()
                }
            }
        }
    }
}
