package com.example.physi_lock.ui.admin

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.AppCategory
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.DefaultSettings
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.Role
import com.example.physi_lock.ui.settings.InstalledAppInfo
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AdminAnalytics(
    val totalAccounts: Int = 0,
    val activeAccounts: Int = 0,
    val categorizedAppCount: Int = 0,
    val totalUsageMinutesLast7Days: Int = 0,
    val topApps: List<AppUsageTotal> = emptyList()
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val appCategoryDao = db.appCategoryDao()
    private val defaultSettingsDao = db.defaultSettingsDao()
    private val appUsageLogDao = db.appUsageLogDao()

    // 1. Manage User Accounts (real-time from Firestore)
    val accounts: StateFlow<List<Account>> = accountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun accountsFlow(): Flow<List<Account>> = callbackFlow {
        val registration = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val accounts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Account::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(accounts)
            }
        awaitClose { registration.remove() }
    }

    fun setAccountActive(account: Account, active: Boolean) {
        viewModelScope.launch {
            firestore.collection("users").document(account.id)
                .update("isActive", active)
                .await()
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            firestore.collection("users").document(account.id)
                .delete()
                .await()
        }
    }

    // 2. Curate App Categories
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    val appCategories: StateFlow<Map<String, String>> = appCategoryDao.getAll()
        .map { list -> list.associate { it.packageName to it.category } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
            appCategoryDao.upsert(
                AppCategory(packageName = app.packageName, appName = app.appName, category = category)
            )
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

    fun setDefaultMotionLockSensitivity(level: String) =
        updateDefaults { it.copy(motionLockSensitivity = level) }

    private fun updateDefaults(transform: (DefaultSettings) -> DefaultSettings) {
        viewModelScope.launch {
            val next = transform(defaultSettings.value).copy(lastUpdatedTime = System.currentTimeMillis())
            defaultSettingsDao.upsert(next)
        }
    }

    // 4. View Usage Analytics + Export Research Data
    private val _analytics = MutableStateFlow(AdminAnalytics())
    val analytics: StateFlow<AdminAnalytics> = _analytics.asStateFlow()

    private fun loadAnalytics() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val startDate = today.minusDays(6)
            val last7Dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val totalMinutes = try {
                (last7Dates.sumOf { date -> appUsageLogDao.getTotalDurationByDateOnce(date.toString()) ?: 0L } / 60_000L).toInt()
            } catch (e: Exception) {
                0
            }
            val topApps = try {
                var latest = emptyList<AppUsageTotal>()
                appUsageLogDao.getAppTotalsByDateRange(startDate.toString(), today.toString())
                    .collect { latest = it.take(5) }
                latest
            } catch (e: Exception) {
                emptyList()
            }
            _analytics.value = AdminAnalytics(
                totalAccounts = accounts.value.size,
                activeAccounts = accounts.value.count { it.isActive },
                categorizedAppCount = appCategories.value.size,
                totalUsageMinutesLast7Days = totalMinutes,
                topApps = topApps
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