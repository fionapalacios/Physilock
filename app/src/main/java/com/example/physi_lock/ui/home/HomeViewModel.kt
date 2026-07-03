package com.example.physi_lock.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.PhysiLockDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val appUsageDao = db.appUsageLogDao()
    private val userConfigDao = db.userConfigurationDao()
    private val appLockRuleDao = db.appLockRuleDao()
    private val today = java.time.LocalDate.now().toString()

    private val _todayScreenTimeMinutes = MutableStateFlow(0)
    val todayScreenTimeMinutes: StateFlow<Int> = _todayScreenTimeMinutes.asStateFlow()

    // Reflects the configured shake-to-unlock difficulty (Settings > Motion Lock
    // Sensitivity), not a computed risk score — real ML risk scoring is Sprint 4/5.
    private val _lockSensitivity = MutableStateFlow("Moderate")
    val lockSensitivity: StateFlow<String> = _lockSensitivity.asStateFlow()

    val lockedAppsToday: StateFlow<List<AppUsageTotal>> = combine(
        appLockRuleDao.getAllRules(),
        appUsageDao.getAppTotalsByDate(today)
    ) { rules, totals ->
        val locked = rules.filter { it.isLocked }.map { it.packageName }.toSet()
        totals.filter { it.packageName in locked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // If DAO provides total duration Flow (ms) convert to minutes
        try {
            val flow = appUsageDao.getTotalDurationByDate(today)
            viewModelScope.launch {
                flow.map { totalMs ->
                    val minutes = (totalMs ?: 0L) / 60000L
                    minutes.toInt()
                }.collect { minutes -> _todayScreenTimeMinutes.value = minutes }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                val totalMs = try { appUsageDao.getTotalDurationByDateOnce(today) } catch (ex: Exception) { 0L }
                _todayScreenTimeMinutes.value = (totalMs ?: 0L / 60000L).toInt()
            }
        }

        viewModelScope.launch {
            val cfg = try { userConfigDao.getActiveConfigurationOnce() } catch (e: Exception) { null }
            _lockSensitivity.value = cfg?.riskSensitivity ?: "Moderate"
        }
    }
}
