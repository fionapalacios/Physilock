package com.example.physi_lock.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.entity.AllowlistedApp
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.entity.AppCategoryType
import com.example.physi_lock.data.entity.DeepWorkSchedule
import com.example.physi_lock.data.entity.ScheduleBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs Student/Work Mode's real schedule-block enforcement UI (StudentModeScreen /
 * WorkModeScreen, see AppMonitorService.activeScheduleBlock for the runtime enforcement
 * side). One shared instance covers both screens -- schedule blocks span both modes
 * (filtered by mode in the UI) and the study allowlist is Student-only. Reuses
 * InstalledAppInfo/the queryIntentActivities loader already defined in
 * AppLockViewModel.kt (same package). */
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val scheduleBlockDao = db.scheduleBlockDao()
    private val allowlistedAppDao = db.allowlistedAppDao()
    private val deepWorkScheduleDao = db.deepWorkScheduleDao()

    val scheduleBlocks: StateFlow<List<ScheduleBlock>> = scheduleBlockDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun addScheduleBlock(mode: String, dayOfWeek: Int, startMinute: Int, endMinute: Int, label: String) {
        viewModelScope.launch {
            scheduleBlockDao.insert(
                ScheduleBlock(
                    mode = mode,
                    dayOfWeek = dayOfWeek,
                    startMinute = startMinute,
                    endMinute = endMinute,
                    label = label
                )
            )
        }
    }

    fun deleteScheduleBlock(id: Long) {
        viewModelScope.launch { scheduleBlockDao.delete(id) }
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
}
