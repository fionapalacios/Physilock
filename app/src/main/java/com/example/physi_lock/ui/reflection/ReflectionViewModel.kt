package com.example.physi_lock.ui.reflection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.entity.MotionInterventionLog
import com.example.physi_lock.data.entity.ReflectionEntry
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PromptType { SCALE, CHOICE, TEXT }

data class ReflectionPrompt(
    val id: String,
    val question: String,
    val type: PromptType,
    val options: List<String> = emptyList(),
    val placeholder: String = ""
)

// Module 5 (Mental Health & Awareness): the 5 fixed prompts from the user's real Reflection
// mockup -- written content, not a stand-in for real data, same category as the pre-existing
// focusQuotes/deepWorkMessages lists elsewhere in the app.
val REFLECTION_PROMPTS = listOf(
    ReflectionPrompt(
        id = "mood",
        question = "How did screen time affect your mood today?",
        type = PromptType.SCALE,
        options = listOf("😔 Worse", "😐 Same", "😊 Better")
    ),
    ReflectionPrompt(
        id = "control",
        question = "Did you feel in control of your phone use today?",
        type = PromptType.SCALE,
        options = listOf("Not at all", "Somewhat", "Yes, fully")
    ),
    ReflectionPrompt(
        id = "trigger",
        question = "What usually triggers you to pick up your phone?",
        type = PromptType.CHOICE,
        options = listOf("Boredom", "Anxiety", "Habit", "FOMO", "Work need")
    ),
    ReflectionPrompt(
        id = "highlight",
        question = "What was the most meaningful thing you did offline today?",
        type = PromptType.TEXT,
        placeholder = "A walk, a conversation, a meal..."
    ),
    ReflectionPrompt(
        id = "tomorrow",
        question = "One thing you'll do differently with your phone tomorrow:",
        type = PromptType.TEXT,
        placeholder = "e.g. No phone during meals, put it away at 9 PM..."
    )
)

private const val REFLECTION_XP = 10

/**
 * One real [ReflectionEntry] per calendar day -- [todayDateKey] is that row's primary key,
 * so saving again the same day edits it rather than creating a duplicate. [currentStreak] is
 * computed from real stored dateKeys (consecutive calendar days ending today or yesterday),
 * not a fabricated counter. Saving also logs a real [MotionInterventionLog] row so the
 * "+10 XP" shown on submit is genuinely added to the same XP total Move hub reads
 * (`MotionInterventionLogDao.getTotalXp`), not a local-only display number.
 */
class ReflectionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val dao = db.reflectionEntryDao()
    private val motionDao = db.motionInterventionLogDao()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val todayDateKey: String = dateFormatter.format(Date())

    val todayEntry: StateFlow<ReflectionEntry?> = dao.getForDate(todayDateKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentEntries: StateFlow<List<ReflectionEntry>> = dao.getRecent(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentStreak: StateFlow<Int> = dao.getAllDateKeysDesc()
        .map { computeStreak(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private fun computeStreak(dateKeysDesc: List<String>): Int {
        if (dateKeysDesc.isEmpty()) return 0
        val days = dateKeysDesc.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        var cursor = LocalDate.now()
        // Today may not have an entry yet mid-wizard -- a streak still "counts" through
        // yesterday in that case, same convention as most habit-streak counters.
        if (cursor !in days) cursor = cursor.minusDays(1)
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun saveReflection(answers: Map<String, String>) {
        val moodImpact = answers["mood"] ?: return
        val controlLevel = answers["control"] ?: return
        val trigger = answers["trigger"] ?: return
        val highlight = answers["highlight"].orEmpty()
        val tomorrowPlan = answers["tomorrow"].orEmpty()

        // Captured before the write so a same-session re-save (todayEntry already
        // non-null) can't race the async Flow update and award XP twice for one day.
        val alreadyAnsweredToday = todayEntry.value != null

        viewModelScope.launch {
            dao.upsert(
                ReflectionEntry(
                    dateKey = todayDateKey,
                    moodImpact = moodImpact,
                    controlLevel = controlLevel,
                    trigger = trigger,
                    highlight = highlight,
                    tomorrowPlan = tomorrowPlan,
                    timestamp = System.currentTimeMillis()
                )
            )
            // Only award XP the first time today's entry is saved -- re-saving the same
            // day edits the row above but shouldn't mint XP twice for one day's reflection.
            if (!alreadyAnsweredToday) {
                motionDao.insert(
                    MotionInterventionLog(
                        packageName = getApplication<Application>().packageName,
                        triggerType = "REFLECTION",
                        interventionTimestamp = System.currentTimeMillis(),
                        userResponse = "UNLOCKED",
                        xpEarned = REFLECTION_XP
                    )
                )
            }
        }
    }
}

private val dateDisplayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

fun formatReflectionDateKey(dateKey: String): String = try {
    LocalDate.parse(dateKey).format(dateDisplayFormatter)
} catch (e: Exception) {
    dateKey
}
