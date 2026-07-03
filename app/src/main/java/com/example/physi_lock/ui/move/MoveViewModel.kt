package com.example.physi_lock.ui.move

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.PhysiLockDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MoveViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val motionDao = db.motionInterventionLogDao()

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Compute timestamp for 30 days ago and call DAO helper
                val after = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                val count = motionDao.countSuccessfulInterventionDaysAfter(after)
                _streakDays.value = count
            } catch (e: Exception) {
                _streakDays.value = 3 // placeholder
            }
        }
    }
}
