package com.example.physi_lock.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.entity.NotificationLog
import com.example.physi_lock.data.db.PhysiLockDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = PhysiLockDatabase.getInstance(application).notificationLogDao()

    val notifications: StateFlow<List<NotificationLog>> = dao.getRecent()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun markAllRead() {
        viewModelScope.launch { dao.markAllRead() }
    }
}
