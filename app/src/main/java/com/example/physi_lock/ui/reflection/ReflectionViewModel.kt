package com.example.physi_lock.ui.reflection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.ReflectionEntry
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Module 5 (Mental Health & Awareness): static prompt copy, same category as the existing
// focusQuotes list in FocusModeScreen.kt -- written content, not a stand-in for real data.
// Picked deterministically by day-of-year so the same prompt shows all day (re-opening the
// screen doesn't reshuffle it), and rotates to a new one tomorrow.
val REFLECTION_PROMPTS = listOf(
    "What's one thing you accomplished today, away from your screen?",
    "How did your device usage make you feel today?",
    "What would you like to do with the time you saved today?",
    "Did you notice any moments where you reached for your phone out of habit rather than need?",
    "What's one small win you had today, on or off your phone?",
    "How present did you feel in your conversations today?",
    "What's something that brought you joy today that had nothing to do with a screen?",
    "If today had one theme, what would it be?",
    "What's one boundary you want to set with your phone tomorrow?",
    "How rested do you feel right now?"
)

/**
 * One real [ReflectionEntry] per calendar day -- [todayDateKey] is that row's primary key,
 * so saving again the same day edits it rather than creating a duplicate. [todayEntry] lets
 * the screen pre-fill an existing answer instead of always starting blank.
 */
class ReflectionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = PhysiLockDatabase.getInstance(application).reflectionEntryDao()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val todayDateKey: String = dateFormatter.format(Date())
    val todayPrompt: String = REFLECTION_PROMPTS[LocalDate.now().dayOfYear % REFLECTION_PROMPTS.size]

    val todayEntry: StateFlow<ReflectionEntry?> = dao.getForDate(todayDateKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentEntries: StateFlow<List<ReflectionEntry>> = dao.getRecent(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveReflection(moodRating: Int, answerText: String) {
        viewModelScope.launch {
            dao.upsert(
                ReflectionEntry(
                    dateKey = todayDateKey,
                    promptText = todayPrompt,
                    moodRating = moodRating,
                    answerText = answerText,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
