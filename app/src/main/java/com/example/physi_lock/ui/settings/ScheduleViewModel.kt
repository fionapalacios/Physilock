package com.example.physi_lock.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.entity.AllowlistedApp
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.entity.AppCategoryType
import com.example.physi_lock.data.entity.DeepWorkSchedule
import com.example.physi_lock.data.entity.PomodoroBlockedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs Student/Work Mode's real settings-list UI (StudentModeScreen/WorkModeScreen) --
 * Deep Work Blocks and the Work Mode blocked-apps display span Work Mode, the study
 * allowlist and Pomodoro blocked-apps list are Student-only. One shared instance covers
 * both screens. Reuses InstalledAppInfo/the queryIntentActivities loader already defined
 * in AppLockViewModel.kt (same package). Live session/timer state for the Pomodoro
 * timer itself lives in the separate PomodoroViewModel, not here. */
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val allowlistedAppDao = db.allowlistedAppDao()
    private val deepWorkScheduleDao = db.deepWorkScheduleDao()
    private val pomodoroBlockedAppDao = db.pomodoroBlockedAppDao()

    val allowlistedApps: StateFlow<List<AllowlistedApp>> = allowlistedAppDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Work Mode redesign (2026-09-07): "Deep Work Blocks" -- named windows that auto-
    // start/end the real Deep Work Mode session, see AppMonitorService.checkDeepWorkSchedules.
    val deepWorkSchedules: StateFlow<List<DeepWorkSchedule>> = deepWorkScheduleDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Read-only display list for Work Mode's new screen -- same Admin-curated
    // Social Media/Entertainment set AppMonitorService.focusBlockedPackages actually
    // enforces (see DeepWorkViewModel.blockedAppNames for the identical pattern). Work
    // Mode's blocking stays admin-governed, not user-toggleable, unlike Focus Mode's
    // own FocusBlockedApp set -- so this is shown, not editable, deliberately deviating
    // from the mockup's per-app toggle affordance to preserve that existing boundary.
    val workModeBlockedAppNames: StateFlow<List<String>> = db.appCategoryDao().getAll()
        .map { categories ->
            categories
                .filter { it.category == AppCategoryType.SOCIAL_MEDIA || it.category == AppCategoryType.ENTERTAINMENT }
                .map { it.appName }
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Student Mode Pomodoro (2026-09-07): a User-owned blocklist for the timer's WORK
    // phase, replacing the old Class-Schedule-era allowlist-inversion model. See
    // PomodoroBlockedApp kdoc for why this is a blocklist, not an allowlist.
    val pomodoroBlockedApps: StateFlow<List<PomodoroBlockedApp>> = pomodoroBlockedAppDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

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
    }

    fun addDeepWorkSchedule(label: String, startMinute: Int, endMinute: Int) {
        viewModelScope.launch {
            deepWorkScheduleDao.insert(DeepWorkSchedule(label = label, startMinute = startMinute, endMinute = endMinute))
        }
    }

    fun setDeepWorkScheduleActive(id: Long, active: Boolean) {
        viewModelScope.launch { deepWorkScheduleDao.setActive(id, active) }
    }

    fun deleteDeepWorkSchedule(id: Long) {
        viewModelScope.launch { deepWorkScheduleDao.delete(id) }
    }

    fun setAllowlisted(app: InstalledAppInfo, allowed: Boolean) {
        viewModelScope.launch {
            if (allowed) {
                allowlistedAppDao.upsert(AllowlistedApp(packageName = app.packageName, appName = app.appName))
            } else {
                allowlistedAppDao.delete(app.packageName)
            }
        }
    }

    fun setPomodoroBlocked(app: InstalledAppInfo, blocked: Boolean) {
        viewModelScope.launch {
            if (blocked) {
                pomodoroBlockedAppDao.upsert(PomodoroBlockedApp(packageName = app.packageName, appName = app.appName))
            } else {
                pomodoroBlockedAppDao.delete(app.packageName)
            }
        }
    }
}
