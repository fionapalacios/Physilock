package com.example.physi_lock.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.data.FocusSession
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UserConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Module 6 (Personalization & User Control): Focus Mode's real session backend. Replaces
// the old FocusModeRoute's locally-ticking counter that reset on navigating away --
// elapsedSeconds here is derived from a persisted FocusSession.startTimeMillis, so it
// survives navigation and even process death, and AppMonitorService enforces the actual
// app-blocking off the same table (see its focus-mode block check).
class FocusModeViewModel(application: Application) : AndroidViewModel(application) {
    private val focusSessionDao = PhysiLockDatabase.getInstance(application).focusSessionDao()
    private val appCategoryDao = PhysiLockDatabase.getInstance(application).appCategoryDao()
    private val userConfigDao = PhysiLockDatabase.getInstance(application).userConfigurationDao()

    val activeSession: StateFlow<FocusSession?> = focusSessionDao.getActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    // Blocked-during-focus chips reflect real Admin-curated categories (Social Media /
    // Entertainment) instead of the ported UI's hardcoded Instagram/TikTok/etc list --
    // apps the Admin hasn't categorized yet simply won't appear here or be blocked.
    val blockedApps: StateFlow<List<Pair<String, String>>> = appCategoryDao.getAll()
        .map { categories ->
            categories
                .filter { it.category == AppCategoryType.SOCIAL_MEDIA || it.category == AppCategoryType.ENTERTAINMENT }
                .sortedBy { it.appName }
                .map { category ->
                    val emoji = if (category.category == AppCategoryType.SOCIAL_MEDIA) "📱" else "🎬"
                    emoji to category.appName
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // collectLatest both keeps activeSession's stateIn hot (so startSessionIfNeeded/
        // endSession above see fresh values) and cancels/restarts the ticking loop
        // whenever the active session changes (starts, ends, or a new one begins).
        viewModelScope.launch {
            activeSession.collectLatest { session ->
                if (session == null) {
                    _elapsedSeconds.value = 0L
                    return@collectLatest
                }
                while (true) {
                    _elapsedSeconds.value = ((System.currentTimeMillis() - session.startTimeMillis) / 1000).coerceAtLeast(0)
                    delay(1000)
                }
            }
        }
    }

    /**
     * No-op if a session is already active (e.g. re-entering the screen mid-session).
     * Queries the DB directly rather than reading [activeSession]'s cached value, which
     * may not have started collecting yet on first composition.
     */
    fun startSessionIfNeeded() {
        viewModelScope.launch {
            if (focusSessionDao.getActiveSession().first() != null) return@launch
            focusSessionDao.insert(FocusSession(startTimeMillis = System.currentTimeMillis()))
        }
    }

    /**
     * Ends the session and credits [UserConfiguration.focusCreditBalanceMinutes] by the
     * amount earned -- previously this number was only ever stored on the FocusSession row
     * and shown once in EndFocusSessionSheet, with no way to actually spend it. Redeeming
     * happens from the Move hub (see MoveViewModel.redeemFocusCredit), the same real
     * per-app timed-unlock mechanism Activity Challenges already use.
     */
    fun endSession() {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val creditMinutes = ((endTime - session.startTimeMillis) / 300_000L).toInt()
            focusSessionDao.endSession(session.id, endTime, creditMinutes)
            if (creditMinutes > 0) {
                try {
                    val current = userConfigDao.getActiveConfigurationOnce() ?: UserConfiguration()
                    userConfigDao.upsert(
                        current.copy(
                            focusCreditBalanceMinutes = current.focusCreditBalanceMinutes + creditMinutes,
                            lastUpdatedTime = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
