package com.example.physi_lock.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.entity.AppCategoryType
import com.example.physi_lock.data.entity.DeepWorkSession
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.entity.UserConfiguration
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

// Deep Work Mode (2026-09-04): a stricter tier than Focus Mode -- see DeepWorkSession kdoc
// for the shape/enforcement differences. Shares the same real-elapsed-time-from-persisted-
// startTimeMillis pattern FocusModeViewModel established (survives navigation/process
// death), and the same focusCreditBalanceMinutes currency/rate (1 credit min per 5 min),
// per instruction that ending early still earns credit -- no forfeiture penalty.
class DeepWorkViewModel(application: Application) : AndroidViewModel(application) {
    private val deepWorkSessionDao = PhysiLockDatabase.getInstance(application).deepWorkSessionDao()
    private val userConfigDao = PhysiLockDatabase.getInstance(application).userConfigurationDao()
    private val appCategoryDao = PhysiLockDatabase.getInstance(application).appCategoryDao()

    val activeSession: StateFlow<DeepWorkSession?> = deepWorkSessionDao.getActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Same Admin-category-derived set AppMonitorService.deepWorkBlockedPackages actually
    // enforces against -- shown here just for the "what's blocked" chip cloud.
    val blockedAppNames: StateFlow<List<String>> = appCategoryDao.getAll()
        .map { categories ->
            categories
                .filter { it.category == AppCategoryType.SOCIAL_MEDIA || it.category == AppCategoryType.ENTERTAINMENT }
                .map { it.appName }
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    init {
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

    /** No-op if a session is already active. [durationSecs] only applies to the session
     *  actually created here -- re-entering mid-session keeps the original duration. */
    fun startSessionIfNeeded(durationSecs: Int) {
        viewModelScope.launch {
            if (deepWorkSessionDao.getActiveSession().first() != null) return@launch
            deepWorkSessionDao.insert(DeepWorkSession(startTimeMillis = System.currentTimeMillis(), durationSecs = durationSecs))
        }
    }

    /** [endedEarly] is recorded for honesty/analytics only -- credit is earned at the same
     *  rate either way, per instruction (no early-exit forfeiture). */
    fun endSession(endedEarly: Boolean) {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val creditMinutes = ((endTime - session.startTimeMillis) / 300_000L).toInt()
            deepWorkSessionDao.endSession(session.id, endTime, endedEarly, creditMinutes)
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
