package com.prototype.physi_lock.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.physi_lock.data.PhysiLockDatabase
import com.prototype.physi_lock.data.UserConfiguration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MS_PER_HOUR = 3_600_000L

/**
 * Only the "Total Daily Screen Time" goal maps onto a real column
 * (UserConfiguration.dailyScreenTimeThresholdMs). The per-category goals
 * (Social Media, Entertainment, Gaming) have no corresponding field in the
 * capstone schema yet, so those stay UI-local until a category-aware
 * data model exists.
 */
class UsageGoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val userConfigDao = PhysiLockDatabase.getInstance(application).userConfigurationDao()

    val dailyLimitHours: StateFlow<Float> = userConfigDao.getActiveConfiguration()
        .map { (it?.dailyScreenTimeThresholdMs ?: (7 * MS_PER_HOUR)) / MS_PER_HOUR.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 7f)

    fun setDailyLimitHours(hours: Float) {
        viewModelScope.launch {
            val current = userConfigDao.getActiveConfigurationOnce() ?: UserConfiguration()
            userConfigDao.upsert(
                current.copy(
                    dailyScreenTimeThresholdMs = (hours * MS_PER_HOUR).toLong(),
                    lastUpdatedTime = System.currentTimeMillis()
                )
            )
        }
    }
}
