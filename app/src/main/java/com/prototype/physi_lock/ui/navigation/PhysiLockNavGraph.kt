package com.prototype.physi_lock.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prototype.physi_lock.ui.screens.auth.CreateAccountScreen
import com.prototype.physi_lock.ui.screens.auth.ForgotPasswordScreen
import com.prototype.physi_lock.ui.screens.auth.LoginScreen
import com.prototype.physi_lock.ui.screens.auth.VerifyEmailScreen
import com.prototype.physi_lock.ui.screens.dashboard.DashboardScreen
import com.prototype.physi_lock.ui.screens.onboarding.GetStartedScreen
import com.prototype.physi_lock.ui.screens.onboarding.GrantPermissionsScreen

object PhysiLockDestinations {
    const val GET_STARTED = "get_started"
    const val CREATE_ACCOUNT = "create_account"
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"
    const val VERIFY_EMAIL = "verify_email"
    const val GRANT_PERMISSIONS = "grant_permissions"
    const val DASHBOARD = "dashboard"

    const val VERIFY_EMAIL_EMAIL_ARG = "email"
    const val VERIFY_EMAIL_ROUTE = "$VERIFY_EMAIL/{$VERIFY_EMAIL_EMAIL_ARG}"

    fun verifyEmailRoute(email: String) = "$VERIFY_EMAIL/${Uri.encode(email)}"
}

@Composable
fun PhysiLockNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = PhysiLockDestinations.GET_STARTED,
        modifier = modifier
    ) {
        composable(PhysiLockDestinations.GET_STARTED) {
            GetStartedScreen(
                onGetStartedClick = {
                    navController.navigate(PhysiLockDestinations.CREATE_ACCOUNT)
                },
                onLoginClick = {
                    navController.navigate(PhysiLockDestinations.LOGIN)
                }
            )
        }
        composable(PhysiLockDestinations.CREATE_ACCOUNT) {
            CreateAccountScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(PhysiLockDestinations.LOGIN) {
                        launchSingleTop = true
                    }
                },
                onAccountCreated = { email ->
                    navController.navigate(PhysiLockDestinations.verifyEmailRoute(email))
                }
            )
        }
        composable(
            route = PhysiLockDestinations.VERIFY_EMAIL_ROUTE,
            arguments = listOf(navArgument(PhysiLockDestinations.VERIFY_EMAIL_EMAIL_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString(PhysiLockDestinations.VERIFY_EMAIL_EMAIL_ARG).orEmpty()
            VerifyEmailScreen(
                email = email,
                onBackClick = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(PhysiLockDestinations.GRANT_PERMISSIONS) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(PhysiLockDestinations.GRANT_PERMISSIONS) {
            GrantPermissionsScreen(
                onContinueClick = {
                    navController.navigate(PhysiLockDestinations.DASHBOARD) {
                        popUpTo(PhysiLockDestinations.GET_STARTED) { inclusive = true }
                    }
                }
            )
        }
        composable(PhysiLockDestinations.LOGIN) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToRegister = {
                    navController.navigate(PhysiLockDestinations.CREATE_ACCOUNT) {
                        launchSingleTop = true
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(PhysiLockDestinations.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    navController.navigate(PhysiLockDestinations.DASHBOARD) {
                        popUpTo(PhysiLockDestinations.GET_STARTED) { inclusive = true }
                    }
                }
            )
        }
        composable(PhysiLockDestinations.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onResetLinkSent = { }
            )
        }
        composable(PhysiLockDestinations.DASHBOARD) {
            DashboardScreen()
        }
    }
}
