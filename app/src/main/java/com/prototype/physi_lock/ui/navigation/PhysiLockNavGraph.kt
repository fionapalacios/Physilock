package com.prototype.physi_lock.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prototype.physi_lock.ui.screens.auth.CreateAccountScreen
import com.prototype.physi_lock.ui.screens.auth.ForgotPasswordScreen
import com.prototype.physi_lock.ui.screens.auth.LoginScreen
import com.prototype.physi_lock.ui.screens.dashboard.DashboardScreen
import com.prototype.physi_lock.ui.screens.onboarding.GetStartedScreen

object PhysiLockDestinations {
    const val GET_STARTED = "get_started"
    const val CREATE_ACCOUNT = "create_account"
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"
    const val DASHBOARD = "dashboard"
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
                onAccountCreated = {
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
