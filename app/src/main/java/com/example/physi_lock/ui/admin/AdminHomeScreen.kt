package com.example.physi_lock.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.SignOutBackground

/** One outlined rectangle in an 18x18dp glyph, in dp units matching the source mockup's CSS. */
data class GlyphRect(val x: Float, val y: Float, val w: Float, val h: Float)

/**
 * Bottom-nav icon glyphs, reproduced from the teammate's Figma "copy as code" export
 * (2026-08-28) — each icon there is just a handful of absolutely-positioned outlined divs
 * (Figma's CSS export flattens vector icon layers to their stroke segments' bounding boxes),
 * so we replicate those exact rects via Canvas instead of substituting a Material icon.
 * DEFAULTS has no shapes in the source at all — its div was empty — so it renders blank here
 * too; swap in a real icon once one exists for that tab.
 */
private val AccountsGlyph = listOf(
    GlyphRect(1.5f, 11.25f, 10.5f, 4.5f),
    GlyphRect(3.75f, 2.25f, 6f, 6f),
    GlyphRect(14.25f, 11.35f, 2.25f, 4.4f),
    GlyphRect(12f, 2.35f, 2.26f, 5.81f)
)
private val AppsGlyph = listOf(
    GlyphRect(2.25f, 2.25f, 5.25f, 5.25f),
    GlyphRect(10.5f, 2.25f, 5.25f, 5.25f),
    GlyphRect(10.5f, 10.5f, 5.25f, 5.25f),
    GlyphRect(2.25f, 10.5f, 5.25f, 5.25f)
)
private val AnalyticsGlyph = listOf(
    GlyphRect(9f, 1.54f, 7.47f, 7.47f),
    GlyphRect(1.5f, 2.12f, 14.41f, 14.37f)
)

enum class AdminTab(
    val label: String,
    val glyph: List<GlyphRect>,
    val strokeWidth: Dp,
    val heading: String,
    val subheading: String? = null
) {
    ACCOUNTS("Accounts", AccountsGlyph, 1.65.dp, "Manage User Accounts"),
    CATEGORIES(
        "Apps", AppsGlyph, 1.35.dp, "App Categories",
        "Classify apps to apply appropriate screen-time rules per category."
    ),
    DEFAULTS(
        "Defaults", emptyList(), 1.35.dp, "Configure Default Settings",
        "Applied to new accounts on registration. Motion Lock Sensitivity and Doomscrolling " +
            "Detection/Sensitivity are Admin-controlled — users can view these in their own Settings " +
            "but cannot change them."
    ),
    ANALYTICS(
        "Analytics", AnalyticsGlyph, 1.35.dp, "Usage Analytics",
        "Aggregate, anonymized usage across all accounts — no individual user is identifiable here."
    )
}

@Composable
private fun NavGlyph(glyph: List<GlyphRect>, strokeWidth: Dp, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val stroke = Stroke(width = strokeWidth.toPx())
        glyph.forEach { rect ->
            drawRect(
                color = tint,
                topLeft = Offset(rect.x.dp.toPx(), rect.y.dp.toPx()),
                size = Size(rect.w.dp.toPx(), rect.h.dp.toPx()),
                style = stroke
            )
        }
    }
}

@Composable
fun AdminHomeScreen(viewModel: AdminViewModel = viewModel(), onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(AdminTab.ACCOUNTS) }

    val isOnline by viewModel.isOnline.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Admin Console",
                fontFamily = Nunito,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = DeepOlive
            )
            Box(
                modifier = Modifier
                    .background(SignOutBackground, RoundedCornerShape(10.dp))
                    .border(0.8.dp, ErrorRed.copy(alpha = 0.13f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Sign Out",
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ErrorRed
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            if (!isOnline) {
                AdminOfflineBanner()
                Spacer(modifier = Modifier.height(8.dp))
            }
            actionError?.let { message ->
                AdminErrorBanner(message = message, onDismiss = viewModel::dismissActionError)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = tab.heading,
                fontFamily = Nunito,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = DeepOlive,
                modifier = Modifier.padding(top = 12.dp)
            )
            tab.subheading?.let { subheading ->
                Text(
                    text = subheading,
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    color = MutedText,
                    modifier = Modifier.padding(top = 2.dp)
                )
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
                    val categoryEntries by viewModel.appCategoryEntries.collectAsState()
                    AdminCategoriesSection(
                        apps = apps,
                        categoryEntries = categoryEntries,
                        onSetCategory = viewModel::setCategory,
                        onRemoveCategory = viewModel::removeAppCategory,
                        onAddManualApp = viewModel::addManualApp
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

        AdminBottomNav(selected = tab, onSelect = { tab = it })
    }
}

@Composable
private fun AdminBottomNav(selected: AdminTab, onSelect: (AdminTab) -> Unit) {
    Column {
        HorizontalDivider(color = DeepOlive.copy(alpha = 0.08f), thickness = 0.8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundLight)
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AdminTab.entries.forEach { t ->
                val isSelected = t == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(t) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 32.dp)
                            .background(
                                if (isSelected) DeepOlive else Color.Transparent,
                                RoundedCornerShape(19.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        NavGlyph(
                            glyph = t.glyph,
                            strokeWidth = t.strokeWidth,
                            tint = if (isSelected) SecondarySage else MutedText
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t.label,
                        fontFamily = Nunito,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isSelected) DeepOlive else MutedText
                    )
                }
            }
        }
    }
}
