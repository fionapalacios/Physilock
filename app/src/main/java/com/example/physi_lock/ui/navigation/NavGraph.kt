package com.example.physi_lock.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.model.Account
import com.example.physi_lock.ui.focus.DeepWorkRoute
import com.example.physi_lock.ui.focus.FocusModeRoute
import com.example.physi_lock.ui.theme.BackgroundLight
import kotlinx.coroutines.flow.first
import com.example.physi_lock.ui.goals.UsageGoalsScreen
import com.example.physi_lock.ui.home.HomeScreen
import com.example.physi_lock.ui.reflection.ReflectionScreen
import com.example.physi_lock.ui.reports.ReportsScreen
import com.example.physi_lock.ui.move.MoveScreen
import com.example.physi_lock.ui.settings.AppLockRulesScreen
import com.example.physi_lock.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Reports : Screen("reports")
    object Move : Screen("move")
    object Settings : Screen("settings")
    object Focus : Screen("focus")
    object DeepWork : Screen("deep_work")
    object Goals : Screen("goals")
    object AppLockRules : Screen("app_lock_rules")
    object Reflection : Screen("reflection")
}

@Composable
fun NavGraph(
    currentAccount: Account?,
    onAccountUpdated: (Account) -> Unit,
    onLogout: () -> Unit,
    startDestination: String = Screen.Home.route
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()

    // Ground-truth lock, resolved once before the graph ever renders: if a FocusSession is
    // genuinely active, this NavGraph starts directly on Focus instead of Home. Covers the
    // Focus-session notification landing on Home when the process was recreated in the
    // background (Compose Navigation's own back-stack restoration isn't reliable enough to
    // trust here). Resolving the *start destination* itself (rather than rendering Home then
    // correcting via navigate()) avoids a visible Home flash before landing on Focus.
    val focusSessionDao = remember { PhysiLockDatabase.getInstance(context).focusSessionDao() }
    var resolvedStartDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        resolvedStartDestination =
            if (focusSessionDao.getActiveSession().first() != null) Screen.Focus.route else startDestination
    }
    val actualStartDestination = resolvedStartDestination
    if (actualStartDestination == null) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundLight))
        return
    }

    Scaffold(
        // Focus Mode is meant to be a locked screen while a session is active -- the only
        // way out is "End Focus Session" (see FocusModeRoute's BackHandler). Showing the
        // bottom nav here let the user just tap "Home" and leave with no confirmation at all.
        bottomBar = {
            if (currentRoute?.destination?.route != Screen.Focus.route &&
                currentRoute?.destination?.route != Screen.DeepWork.route
            ) {
                BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = actualStartDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    displayName = currentAccount?.fullName ?: "Alex",
                    onManageAppLock = {
                        navController.navigate(Screen.AppLockRules.route)
                    },
                    onNavigateToFocus = { navController.navigate(Screen.Focus.route) },
                    onNavigateToDeepWork = { navController.navigate(Screen.DeepWork.route) },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                    onNavigateToMove = { navController.navigate(Screen.Move.route) },
                    onNavigateToReflection = { navController.navigate(Screen.Reflection.route) }
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
            composable(Screen.Focus.route) {
                FocusModeRoute(
                    // Explicit navigate-to-Home rather than popBackStack(): Focus can be either
                    // the actual start destination (nothing below it in the stack -- the exact
                    // shape that made popBackStack() silently no-op and strand the user on this
                    // screen after ending a session) or pushed on top of Home normally. This
                    // works correctly either way.
                    onEndFocusClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Focus.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.DeepWork.route) {
                DeepWorkRoute(
                    onEndClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.DeepWork.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Goals.route) {
                UsageGoalsScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { navController.popBackStack() }
                )
            }
            composable(Screen.AppLockRules.route) {
                AppLockRulesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Reflection.route) {
                ReflectionScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}
