package com.example.physi_lock.ui.move

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.MotionInterventionLog
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UsageStatsRepository
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.sensor.ChallengeType
import com.example.physi_lock.service.AppMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class LockedAppInfo(val packageName: String, val appName: String)

class MoveViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val motionDao = db.motionInterventionLogDao()
    private val appLockRuleDao = db.appLockRuleDao()
    private val usageStatsRepository = UsageStatsRepository(application)

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    val totalXp: StateFlow<Int> = motionDao.getTotalXp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lockedApps: StateFlow<List<LockedAppInfo>> = appLockRuleDao.getAllRules()
        .map { rules ->
            rules.filter { it.isLocked }.map {
                LockedAppInfo(it.packageName, usageStatsRepository.getAppLabel(it.packageName))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Move hub's per-card "Completed" badge — which challenge types were already
    // finished today, so the hub doesn't just show 3 always-open cards.
    val completedToday: StateFlow<Set<ChallengeType>> = motionDao.getCompletedChallengeTypesAfter(startOfTodayMillis())
        .map { names -> names.mapNotNull { name -> runCatching { ChallengeType.valueOf(name) }.getOrNull() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    var sensitivity: ChallengeSensitivity = ChallengeSensitivity.MEDIUM
        private set

    init {
        viewModelScope.launch {
            try {
                val after = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                val count = motionDao.countSuccessfulInterventionDaysAfter(after)
                _streakDays.value = count
            } catch (e: Exception) {
                _streakDays.value = 3 // placeholder
            }

            val cfg = try { db.userConfigurationDao().getActiveConfigurationOnce() } catch (e: Exception) { null }
            sensitivity = ChallengeSensitivity.fromLabel(cfg?.motionLockSensitivity)
        }
    }

    fun onChallengeCompleted(packageName: String, type: ChallengeType) {
        viewModelScope.launch {
            AppMonitorService.grantTemporaryUnlock(packageName, AppMonitorService.CHALLENGE_UNLOCK_DURATION_MS)
            try {
                motionDao.insert(
                    MotionInterventionLog(
                        packageName = packageName,
                        triggerType = "VOLUNTARY_CHALLENGE",
                        interventionTimestamp = System.currentTimeMillis(),
                        userResponse = "UNLOCKED",
                        challengeType = type.name,
                        xpEarned = type.xpReward
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun startOfTodayMillis(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}
