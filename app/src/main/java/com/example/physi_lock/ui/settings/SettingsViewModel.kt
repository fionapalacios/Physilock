package com.example.physi_lock.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UserConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val userConfigDao = db.userConfigurationDao()
    private val motionDao = db.motionInterventionLogDao()

    val configuration: StateFlow<UserConfiguration> = userConfigDao.getActiveConfiguration()
        .map { it ?: UserConfiguration() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserConfiguration()
        )

    // Real value for SettingsScreen's ProfileCard streak line — same query/window
    // MoveViewModel uses, duplicated here rather than shared since the two ViewModels
    // have no other relationship and this is a single cheap COUNT query.
    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val after = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                _streakDays.value = motionDao.countSuccessfulInterventionDaysAfter(after)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Real per-mode default limit (2026-08-27): "Stricter for Student / Looser for Work"
    // is applied as a preset daily-limit re-applied on every explicit mode switch (a
    // proposed default, not manuscript-specified — bracketing the existing 480 min
    // global default), rather than a silently-never-changing one-time value.
    fun setUserMode(mode: String) = update {
        val presetMinutes = when (mode) {
            "STUDENT_MODE" -> 360
            "WORK_MODE" -> 600
            else -> null
        }
        val base = it.copy(userMode = mode)
        if (presetMinutes != null) base.copy(dailyScreenTimeThresholdMs = presetMinutes * 60_000L) else base
    }

    fun setDailyScreenTimeThresholdMinutes(minutes: Int) =
        update { it.copy(dailyScreenTimeThresholdMs = minutes * 60_000L) }

    fun setBreakReminderEnabled(enabled: Boolean) =
        update { it.copy(breakReminderEnabled = enabled) }

    fun setBreakReminderIntervalMinutes(minutes: Int) =
        update { it.copy(breakReminderIntervalMs = minutes * 60_000L) }

    fun setOveruseAlertsEnabled(enabled: Boolean) =
        update { it.copy(overuseAlertsEnabled = enabled) }

    fun setContextAlertsEnabled(enabled: Boolean) =
        update { it.copy(contextAlertsEnabled = enabled) }

    fun setContextAlertWifiSsid(ssid: String?) =
        update { it.copy(contextAlertWifiSsid = ssid?.takeIf { s -> s.isNotBlank() }) }

    /** Settings > Reset to Default Settings — overwrites every threshold/toggle back to
     * [UserConfiguration]'s built-in defaults, keeping only the account's chosen usage mode. */
    fun resetToDefaults() {
        viewModelScope.launch {
            val currentMode = configuration.value.userMode
            userConfigDao.upsert(
                UserConfiguration(userMode = currentMode, lastUpdatedTime = System.currentTimeMillis())
            )
        }
    }

    private fun update(transform: (UserConfiguration) -> UserConfiguration) {
        viewModelScope.launch {
            val current = configuration.value
            val next = transform(current).copy(lastUpdatedTime = System.currentTimeMillis())
            userConfigDao.upsert(next)
        }
    }
}
