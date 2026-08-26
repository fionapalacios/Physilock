package com.example.physi_lock.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SecondarySage

/** Visual design ported from the teammate's sprint-2-ui-navigation branch (BottomNavBar.kt) — a
 * pill-highlighted branded bar replacing the stock Material3 NavigationBar. Keeps this repo's
 * NavController-driven routing (their version takes a plain enum + callback instead). Icon/pill/
 * label sizing reverted 2026-08-26 back to the teammate's original values (17dp icon, 12sp
 * label) after a prior session's "restore to Material-standard 24dp" pass was judged too big. */
private data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

private val bottomNavItems = listOf(
    BottomNavItem("home", Icons.Filled.Home, "Home"),
    BottomNavItem("reports", Icons.Filled.PieChart, "Reports"),
    BottomNavItem("move", Icons.AutoMirrored.Filled.DirectionsWalk, "Move"),
    BottomNavItem("settings", Icons.Filled.Settings, "Settings")
)

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.79.dp)
                .background(DeepOlive.copy(alpha = 0.08f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundLight)
                .padding(horizontal = 7.5.dp, vertical = 7.5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = item.route == currentRoute
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 32.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.background(DeepOlive, RoundedCornerShape(19.dp))
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) SecondarySage else DeepOlive,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.75.dp))
                    Text(
                        text = item.label,
                        fontFamily = Nunito,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) DeepOlive else DeepOlive.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
