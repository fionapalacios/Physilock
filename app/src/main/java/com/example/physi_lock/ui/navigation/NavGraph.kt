package com.example.physi_lock.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.physi_lock.data.Account
import com.example.physi_lock.ui.home.HomeScreen
import com.example.physi_lock.ui.reports.ReportsScreen
import com.example.physi_lock.ui.move.MoveScreen
import com.example.physi_lock.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Reports : Screen("reports")
    object Move : Screen("move")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    currentAccount: Account?,
    onAccountUpdated: (Account) -> Unit,
    onLogout: () -> Unit,
    startDestination: String = Screen.Home.route
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    displayName = currentAccount?.fullName ?: "Alex",
                    onManageAppLock = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Reports.route) { ReportsScreen() }
            composable(Screen.Move.route) { MoveScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    currentAccount = currentAccount,
                    onAccountUpdated = onAccountUpdated,
                    onLogout = onLogout
                )
            }
        }
    }
}
