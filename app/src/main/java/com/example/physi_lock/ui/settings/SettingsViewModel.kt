package com.example.physi_lock.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UserConfiguration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val userConfigDao = db.userConfigurationDao()

    val configuration: StateFlow<UserConfiguration> = userConfigDao.getActiveConfiguration()
        .map { it ?: UserConfiguration() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserConfiguration()
        )

    fun setUserMode(mode: String) = update { it.copy(userMode = mode) }

    fun setDailyScreenTimeThresholdMinutes(minutes: Int) =
        update { it.copy(dailyScreenTimeThresholdMs = minutes * 60_000L) }

    fun setDoomscrollingDetectionEnabled(enabled: Boolean) =
        update { it.copy(doomscrollingDetectionEnabled = enabled) }

    fun setMotionLockSensitivity(level: String) =
        update { it.copy(motionLockSensitivity = level) }

    private fun update(transform: (UserConfiguration) -> UserConfiguration) {
        viewModelScope.launch {
            val current = configuration.value
            val next = transform(current).copy(lastUpdatedTime = System.currentTimeMillis())
            userConfigDao.upsert(next)
        }
    }
}
