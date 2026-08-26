package com.example.physi_lock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito

enum class AdminTab(val label: String) {
    ACCOUNTS("Accounts"),
    CATEGORIES("Categories"),
    DEFAULTS("Defaults"),
    ANALYTICS("Analytics")
}

@Composable
fun AdminHomeScreen(viewModel: AdminViewModel = viewModel()) {
    var tab by remember { mutableStateOf(AdminTab.ACCOUNTS) }

    val isOnline by viewModel.isOnline.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEFEFE))
            .padding(16.dp)
    ) {
        Text(
            text = "Admin Dashboard",
            fontFamily = Nunito,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = DeepOlive
        )
        Text(
            text = "Governance tools for accounts, app categories, and defaults",
            fontFamily = Nunito,
            fontSize = 13.sp,
            color = MutedText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!isOnline) {
            AdminOfflineBanner()
            Spacer(modifier = Modifier.height(8.dp))
        }
        actionError?.let { message ->
            AdminErrorBanner(message = message, onDismiss = viewModel::dismissActionError)
            Spacer(modifier = Modifier.height(8.dp))
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AdminTab.entries.forEachIndexed { index, t ->
                SegmentedButton(
                    selected = tab == t,
                    onClick = { tab = t },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = AdminTab.entries.size)
                ) {
                    Text(t.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (tab) {
            AdminTab.ACCOUNTS -> {
                val accounts by viewModel.accounts.collectAsState()
                val accountsError by viewModel.accountsError.collectAsState()
                AdminAccountsSection(
                    accounts = accounts,
                    errorMessage = accountsError,
                    onRetry = viewModel::retryAccounts,
                    onSetActive = viewModel::setAccountActive,
                    onDelete = viewModel::deleteAccount
                )
            }
            AdminTab.CATEGORIES -> {
                val apps by viewModel.installedApps.collectAsState()
                val categories by viewModel.appCategories.collectAsState()
                AdminCategoriesSection(
                    apps = apps,
                    categories = categories,
                    onSetCategory = viewModel::setCategory
                )
            }
            AdminTab.DEFAULTS -> {
                val defaults by viewModel.defaultSettings.collectAsState()
                AdminDefaultsSection(
                    defaults = defaults,
                    onSetUserMode = viewModel::setDefaultUserMode,
                    onSetDailyLimitMinutes = viewModel::setDefaultDailyScreenTimeThresholdMinutes,
                    onSetDoomscrolling = viewModel::setDefaultDoomscrollingDetectionEnabled,
                    onSetDoomscrollingSensitivity = viewModel::setDefaultDoomscrollingSensitivity,
                    onSetMotionSensitivity = viewModel::setDefaultMotionLockSensitivity
                )
            }
            AdminTab.ANALYTICS -> {
                val analytics by viewModel.analytics.collectAsState()
                val analyticsError by viewModel.analyticsError.collectAsState()
                AdminAnalyticsSection(
                    analytics = analytics,
                    errorMessage = analyticsError,
                    onRefresh = viewModel::refreshAnalytics,
                    buildExportText = viewModel::buildResearchExport
                )
            }
        }
    }
}