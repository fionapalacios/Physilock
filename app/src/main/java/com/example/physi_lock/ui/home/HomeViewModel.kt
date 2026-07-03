package com.example.physi_lock.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.PhysiLockDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val appUsageDao = db.appUsageLogDao()
    private val userConfigDao = db.userConfigurationDao()

    private val _todayScreenTimeMinutes = MutableStateFlow(0)
    val todayScreenTimeMinutes: StateFlow<Int> = _todayScreenTimeMinutes.asStateFlow()

    private val _riskLevel = MutableStateFlow("Moderate")
    val riskLevel: StateFlow<String> = _riskLevel.asStateFlow()

    init {
        // If DAO provides total duration Flow (ms) convert to minutes
        try {
            val flow = appUsageDao.getTotalDurationByDate(java.time.LocalDate.now().toString())
            viewModelScope.launch {
                flow.map { totalMs ->
                    val minutes = (totalMs ?: 0L) / 60000L
                    minutes.toInt()
                }.collect { minutes -> _todayScreenTimeMinutes.value = minutes }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                val totalMs = try { appUsageDao.getTotalDurationByDateOnce(java.time.LocalDate.now().toString()) } catch (ex: Exception) { 0L }
                _todayScreenTimeMinutes.value = (totalMs ?: 0L / 60000L).toInt()
            }
        }

        viewModelScope.launch {
            val cfg = try { userConfigDao.getActiveConfigurationOnce() } catch (e: Exception) { null }
            _riskLevel.value = cfg?.riskSensitivity ?: "Moderate"
        }
    }
}
