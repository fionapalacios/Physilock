package com.example.physi_lock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito

/** Direct shortcut to Student Mode's "Study App Allowlist" (AllowlistSection in
 *  ScheduleBlockSection.kt / ScheduleViewModel), reachable from Settings without going
 *  through Student Mode's schedule-block screen -- same data, just not buried. */
@Composable
fun WhitelistManagerScreen(
    onBackClick: () -> Unit,
    scheduleViewModel: ScheduleViewModel = viewModel()
) {
    val installedApps by scheduleViewModel.installedApps.collectAsState()
    val allowlistedApps by scheduleViewModel.allowlistedApps.collectAsState()
    val allowlistedPackages = allowlistedApps.map { it.packageName }.toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DeepOlive,
                modifier = Modifier.size(20.dp).clickable(onClick = onBackClick)
            )
            Text(
                text = "Whitelist Manager",
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
        }

        Column(modifier = Modifier.padding(15.dp)) {
            AllowlistSection(
                installedApps = installedApps,
                allowlistedPackages = allowlistedPackages,
                onToggle = { app, allowed -> scheduleViewModel.setAllowlisted(app, allowed) }
            )
        }
    }
}
