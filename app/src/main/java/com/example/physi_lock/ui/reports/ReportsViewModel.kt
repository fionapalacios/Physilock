package com.example.physi_lock.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.PhysiLockDatabase
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DayUsage(val dayLabel: String, val minutes: Int, val isToday: Boolean)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val appUsageDao = db.appUsageLogDao()

    private val _weeklyUsage = MutableStateFlow<List<DayUsage>>(emptyList())
    val weeklyUsage: StateFlow<List<DayUsage>> = _weeklyUsage.asStateFlow()

    init {
        viewModelScope.launch {
            val today = java.time.LocalDate.now()
            // Oldest to newest, ending with today
            val last7Dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
            _weeklyUsage.value = last7Dates.map { date ->
                val totalMs = try { appUsageDao.getTotalDurationByDateOnce(date.toString()) } catch (e: Exception) { 0L }
                DayUsage(
                    dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                    minutes = ((totalMs ?: 0L) / 60_000L).toInt(),
                    isToday = date == today
                )
            }
        }
    }
}
