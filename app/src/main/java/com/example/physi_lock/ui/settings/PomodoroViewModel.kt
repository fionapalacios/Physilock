package com.example.physi_lock.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.entity.PomodoroSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Student Mode's real Pomodoro timer backend (2026-09-07) -- one [PomodoroSession] row
 *  spans a full run from Start to Reset, cycling WORK/BREAK phases automatically. Same
 *  survives-navigation/process-death principle as [com.example.physi_lock.ui.focus.FocusModeViewModel]/
 *  [com.example.physi_lock.ui.focus.DeepWorkViewModel] (remaining time is computed from a
 *  persisted wall-clock anchor, not a live-only counter), extended with a real pause: see
 *  [PomodoroSession] kdoc for how phaseStartTimeMillis/pausedAt work together.
 *
 *  [remainingSeconds] ticks once a second while running by re-collecting [activeSession]
 *  (a Room Flow) -- writing a phase advance to the DB causes that Flow to re-emit, which
 *  restarts the tick loop against the fresh phase/target automatically (`collectLatest`),
 *  rather than needing a separate phase-transition watcher.
 */
class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val pomodoroSessionDao = PhysiLockDatabase.getInstance(application).pomodoroSessionDao()

    val activeSession: StateFlow<PomodoroSession?> = pomodoroSessionDao.getActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    init {
        viewModelScope.launch {
            activeSession.collectLatest { session ->
                if (session == null) {
                    _remainingSeconds.value = 0L
                    return@collectLatest
                }
                if (session.pausedAt != null) {
                    _remainingSeconds.value = remainingSecsFor(session, atTime = session.pausedAt).coerceAtLeast(0)
                    return@collectLatest
                }
                while (true) {
                    val remaining = remainingSecsFor(session, atTime = System.currentTimeMillis())
                    if (remaining <= 0) {
                        advancePhase(session)
                        return@collectLatest
                    }
                    _remainingSeconds.value = remaining
                    delay(1000)
                }
            }
        }
    }

    private fun remainingSecsFor(session: PomodoroSession, atTime: Long): Long {
        val phaseTargetSecs = if (session.phase == "WORK") session.workMinutes * 60L else session.breakMinutes * 60L
        val elapsedSecs = (atTime - session.phaseStartTimeMillis) / 1000
        return phaseTargetSecs - elapsedSecs
    }

    private suspend fun advancePhase(session: PomodoroSession) {
        val now = System.currentTimeMillis()
        val (nextPhase, nextSessionNumber) = if (session.phase == "WORK") {
            "BREAK" to session.sessionNumber
        } else {
            "WORK" to session.sessionNumber + 1
        }
        pomodoroSessionDao.advancePhase(session.id, nextPhase, now, nextSessionNumber)
    }

    /** No-op if a session is already active. [workMinutes]/[breakMinutes] only apply to
     *  the session actually created here -- re-entering mid-session keeps the original
     *  values, same convention [com.example.physi_lock.ui.focus.DeepWorkViewModel] uses. */
    fun startSessionIfNeeded(workMinutes: Int, breakMinutes: Int) {
        viewModelScope.launch {
            if (pomodoroSessionDao.getActiveSessionOnce() != null) return@launch
            val now = System.currentTimeMillis()
            pomodoroSessionDao.insert(
                PomodoroSession(
                    startTimeMillis = now,
                    workMinutes = workMinutes,
                    breakMinutes = breakMinutes,
                    phase = "WORK",
                    phaseStartTimeMillis = now,
                    sessionNumber = 1
                )
            )
        }
    }

    fun pause() {
        viewModelScope.launch {
            val session = pomodoroSessionDao.getActiveSessionOnce() ?: return@launch
            if (session.pausedAt != null) return@launch
            pomodoroSessionDao.setPaused(session.id, System.currentTimeMillis())
        }
    }

    /** Shifts phaseStartTimeMillis forward by exactly how long the pause lasted, so the
     *  remaining time resumes from precisely where it was frozen. */
    fun resume() {
        viewModelScope.launch {
            val session = pomodoroSessionDao.getActiveSessionOnce() ?: return@launch
            val pausedAt = session.pausedAt ?: return@launch
            val pausedDurationMs = System.currentTimeMillis() - pausedAt
            pomodoroSessionDao.resume(session.id, session.phaseStartTimeMillis + pausedDurationMs)
        }
    }

    fun reset() {
        viewModelScope.launch {
            val session = pomodoroSessionDao.getActiveSessionOnce() ?: return@launch
            pomodoroSessionDao.endSession(session.id, System.currentTimeMillis())
        }
    }
}
