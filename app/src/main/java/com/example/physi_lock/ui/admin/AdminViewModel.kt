package com.example.physi_lock.ui.admin

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.AppCategory
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.ui.settings.InstalledAppInfo
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.DailyAppUsage
import com.example.physi_lock.data.DailyCount
import com.example.physi_lock.data.DefaultSettings
import com.example.physi_lock.data.NetworkConnectivityObserver
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.Role
import com.example.physi_lock.data.toAccount
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One line of the "Top Apps (min/day, last 7 days)" chart -- minutesByDay is aligned to
 *  AdminAnalytics.dailyLabels (index 0 = 7 days ago .. index 6 = today). */
data class TopAppSeries(val appName: String, val minutesByDay: List<Int>)

data class AdminAnalytics(
    val totalAccounts: Int = 0,
    val activeAccounts: Int = 0,
    val categorizedAppCount: Int = 0,
    val totalUsageMinutesLast7Days: Int = 0,
    val topApps: List<AppUsageTotal> = emptyList(),
    /** Mon/Tue/.../Sun labels for the last 7 days, oldest first. */
    val dailyLabels: List<String> = emptyList(),
    val topAppsDaily: List<TopAppSeries> = emptyList(),
    val dailyActiveUsers: List<Int> = emptyList(),
    val dailyChallengesCompleted: List<Int> = emptyList()
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val cachedAccountDao = db.cachedAccountDao()
    private val appCategoryDao = db.appCategoryDao()
    private val defaultSettingsDao = db.defaultSettingsDao()
    private val appUsageLogDao = db.appUsageLogDao()
    private val motionInterventionLogDao = db.motionInterventionLogDao()
    private val loginEventDao = db.loginEventDao()

    // Admin governance (known gap tracked since 2026-08-09): a shared "the last mutating
    // action failed" signal every toggle/delete/category-save/settings-save action below
    // now sets on failure instead of silently swallowing it (fire-and-forget with no
    // try/catch, previously). UI shows this as a dismissible banner, not a retry -- retrying
    // a specific failed mutation would need re-capturing its original arguments, which isn't
    // worth the complexity here; the user just retaps the action.
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()
    fun dismissActionError() {
        _actionError.value = null
    }

    // Admin governance: a real connectivity signal backing an honest "you're offline"
    // banner, replacing the previous complete absence of any offline affordance.
    private val connectivityObserver = NetworkConnectivityObserver(application)
    val isOnline: StateFlow<Boolean> = connectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // 1. Manage User Accounts
    // DEMO MODE: reads/writes the local Room cache (the same one DemoAccountRepository uses
    // for login) instead of Firestore, since there's no real backend to query -- see
    // PhysiLockApplication's placeholder Firebase init. accountsError/retryAccounts are kept
    // for API compatibility with AdminAccountsSection; a local Room Flow doesn't fail the way
    // a Firestore listener can, so there's nothing to surface or retry here. Swap back to a
    // Firestore-backed accounts flow once a real backend exists.
    private val _accountsError = MutableStateFlow<String?>(null)
    val accountsError: StateFlow<String?> = _accountsError.asStateFlow()

    fun retryAccounts() {}

    val accounts: StateFlow<List<Account>> = cachedAccountDao.getAll()
        .map { cached -> cached.map { it.toAccount() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAccountActive(account: Account, active: Boolean) {
        viewModelScope.launch {
            try {
                val cached = cachedAccountDao.findById(account.id)
                    ?: throw IllegalStateException("Account not found")
                cachedAccountDao.upsert(cached.copy(isActive = active))
            } catch (e: Exception) {
                _actionError.value = "Couldn't update ${account.fullName}'s status: ${e.localizedMessage ?: "unknown error"}"
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                cachedAccountDao.deleteById(account.id)
            } catch (e: Exception) {
                _actionError.value = "Couldn't delete ${account.fullName}: ${e.localizedMessage ?: "unknown error"}"
            }
        }
    }

    // 2. Curate App Categories
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    // Raw rows (with appName) so a manually-added app -- see addManualApp -- can still render
    // even though it's not in installedApps (it isn't a real launchable package).
    val appCategoryEntries: StateFlow<List<AppCategory>> = appCategoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appCategories: StateFlow<Map<String, String>> = appCategoryEntries
        .map { list -> list.associate { it.packageName to it.category } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** For an app the auto-detected installed list doesn't have (not launchable / not queried
     *  by PackageManager) -- stored under a synthetic package id since there's no real one. */
    fun addManualApp(appName: String, category: String) {
        viewModelScope.launch {
            try {
                appCategoryDao.upsert(
                    AppCategory(
                        packageName = "manual:${UUID.randomUUID()}",
                        appName = appName,
                        category = category
                    )
                )
            } catch (e: Exception) {
                _actionError.value = "Couldn't add $appName: ${e.localizedMessage ?: "unknown error"}"
            }
        }
    }

    fun removeAppCategory(app: InstalledAppInfo) {
        viewModelScope.launch {
            try {
                appCategoryDao.delete(app.packageName)
            } catch (e: Exception) {
                _actionError.value = "Couldn't remove ${app.appName}: ${e.localizedMessage ?: "unknown error"}"
            }
        }
    }

    // 4. View Usage Analytics + Export Research Data
    private val _analytics = MutableStateFlow(AdminAnalytics())
    val analytics: StateFlow<AdminAnalytics> = _analytics.asStateFlow()

    // Admin governance (known gap tracked since 2026-08-09): the two queries below always
    // caught their own failures and silently fell back to 0/emptyList with nothing shown to
    // the UI -- this keeps that same safe-fallback display behavior but now also surfaces
    // that a failure actually happened, so AdminAnalyticsSection can offer a real retry.
    private val _analyticsError = MutableStateFlow<String?>(null)
    val analyticsError: StateFlow<String?> = _analyticsError.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = application.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val ownPackage = application.packageName
            val apps = pm.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                .distinctBy { it.first }
                .filter { it.first != ownPackage }
                .sortedBy { it.second.lowercase() }
                .map { InstalledAppInfo(packageName = it.first, appName = it.second) }
            _installedApps.value = apps
        }
        loadAnalytics()
    }

    fun setCategory(app: InstalledAppInfo, category: String) {
        viewModelScope.launch {
            try {
                appCategoryDao.upsert(
                    AppCategory(packageName = app.packageName, appName = app.appName, category = category)
                )
            } catch (e: Exception) {
                _actionError.value = "Couldn't save ${app.appName}'s category: ${e.localizedMessage ?: "unknown error"}"
            }
        }
    }

    // 3. Configure Default Settings
    val defaultSettings: StateFlow<DefaultSettings> = defaultSettingsDao.getDefaults()
        .map { it ?: DefaultSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultSettings())

    fun setDefaultUserMode(mode: String) = updateDefaults { it.copy(userMode = mode) }

    fun setDefaultDailyScreenTimeThresholdMinutes(minutes: Int) =
        updateDefaults { it.copy(dailyScreenTimeThresholdMs = minutes * 60_000L) }

    fun setDefaultDoomscrollingDetectionEnabled(enabled: Boolean) =
        updateDefaults { it.copy(doomscrollingDetectionEnabled = enabled) }

    fun setDefaultDoomscrollingSensitivity(level: String) =
        updateDefaults { it.copy(doomscrollingSensitivity = level) }

    fun setDefaultMotionLockSensitivity(level: String) =
        updateDefaults { it.copy(motionLockSensitivity = level) }

    private fun updateDefaults(transform: (DefaultSettings) -> DefaultSettings) {
        viewModelScope.launch {
            try {
                val next = transform(defaultSettings.value).copy(lastUpdatedTime = System.currentTimeMillis())
                defaultSettingsDao.upsert(next)
            } catch (e: Exception) {
                _actionError.value = "Couldn't save default settings: ${e.localizedMessage ?: "unknown error"}"
            }
        }
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _analyticsError.value = null
            val today = LocalDate.now()
            val startDate = today.minusDays(6)
            val last7Dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val dailyLabels = last7Dates.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
            val sinceMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val totalMinutes = try {
                (last7Dates.sumOf { date -> appUsageLogDao.getTotalDurationByDateOnce(date.toString()) ?: 0L } / 60_000L).toInt()
            } catch (e: Exception) {
                _analyticsError.value = "Couldn't load usage totals: ${e.localizedMessage ?: "unknown error"}"
                0
            }

            // NOTE: this used to .collect{} a live Flow here, which never completes on its own
            // (Room keeps it open for future invalidations) -- that silently stalled the rest
            // of this function forever after the first emission, so _analytics.value was never
            // actually set. .first() takes just that first emission and lets loading finish.
            val topApps = try {
                appUsageLogDao.getAppTotalsByDateRange(startDate.toString(), today.toString()).first().take(5)
            } catch (e: Exception) {
                _analyticsError.value = "Couldn't load top apps: ${e.localizedMessage ?: "unknown error"}"
                emptyList()
            }

            val topAppsDaily = try {
                val dailyRows = appUsageLogDao.getDailyAppTotals(startDate.toString(), today.toString())
                topApps.take(3).map { app ->
                    val byDate = dailyRows.filter { it.packageName == app.packageName }.associateBy { it.dateKey }
                    TopAppSeries(
                        appName = app.appName,
                        minutesByDay = last7Dates.map { date ->
                            ((byDate[date.toString()]?.totalDurationMs ?: 0L) / 60_000L).toInt()
                        }
                    )
                }
            } catch (e: Exception) {
                _analyticsError.value = "Couldn't load top app trends: ${e.localizedMessage ?: "unknown error"}"
                emptyList()
            }

            val dailyActiveUsers = try {
                val byDate = loginEventDao.getDailyActiveUserCounts(startDate.toString(), today.toString())
                    .associateBy { it.dateKey }
                last7Dates.map { date -> byDate[date.toString()]?.count ?: 0 }
            } catch (e: Exception) {
                _analyticsError.value = "Couldn't load daily active users: ${e.localizedMessage ?: "unknown error"}"
                last7Dates.map { 0 }
            }

            val dailyChallengesCompleted = try {
                val byDate = motionInterventionLogDao.getDailyCompletedCounts(sinceMillis).associateBy { it.dateKey }
                last7Dates.map { date -> byDate[date.toString()]?.count ?: 0 }
            } catch (e: Exception) {
                _analyticsError.value = "Couldn't load challenge completions: ${e.localizedMessage ?: "unknown error"}"
                last7Dates.map { 0 }
            }

            _analytics.value = AdminAnalytics(
                totalAccounts = accounts.value.size,
                activeAccounts = accounts.value.count { it.isActive },
                categorizedAppCount = appCategories.value.size,
                totalUsageMinutesLast7Days = totalMinutes,
                topApps = topApps,
                dailyLabels = dailyLabels,
                topAppsDaily = topAppsDaily,
                dailyActiveUsers = dailyActiveUsers,
                dailyChallengesCompleted = dailyChallengesCompleted
            )
        }
    }

    fun refreshAnalytics() = loadAnalytics()

    fun buildResearchExport(): String {
        val a = analytics.value
        val accs = accounts.value
        val cats = appCategories.value
        val sb = StringBuilder()
        sb.appendLine("Physi-Lock Research Data Export")
        sb.appendLine("Generated: ${LocalDate.now()}")
        sb.appendLine()
        sb.appendLine("== Account Summary (anonymized) ==")
        sb.appendLine("Total accounts: ${accs.size}")
        sb.appendLine("Active accounts: ${accs.count { it.isActive }}")
        sb.appendLine("Admin accounts: ${accs.count { it.role == Role.ADMIN }}")
        sb.appendLine()
        sb.appendLine("== Usage Summary (last 7 days, aggregate) ==")
        sb.appendLine("Total usage minutes: ${a.totalUsageMinutesLast7Days}")
        sb.appendLine("Top apps by usage:")
        a.topApps.forEach { sb.appendLine("  - ${it.appName}: ${it.totalDurationMs / 60_000L} min") }
        sb.appendLine()
        sb.appendLine("== App Category Master List ==")
        cats.entries.groupBy({ it.value }, { it.key }).forEach { (category, packages) ->
            sb.appendLine("${AppCategoryType.label(category)}: ${packages.joinToString(", ")}")
        }
        return sb.toString()
    }
}