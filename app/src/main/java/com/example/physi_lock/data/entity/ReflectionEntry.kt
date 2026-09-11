package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Module 5 (Mental Health & Awareness): one real entry per calendar day (dateKey is the
// primary key), matching the "Daily Reflection" framing already used on Home — answering
// again the same day edits that day's entry rather than creating a duplicate.
// 2026-09-09: replaced the original single mood(1-5)+freetext shape with the 5 fixed
// prompts from the user's real Reflection mockup (mood impact, sense of control, pickup
// trigger, offline highlight, tomorrow's intention) -- see REFLECTION_PROMPTS in
// ReflectionViewModel.kt for the exact question text each field answers.
@Entity(tableName = "reflection_entries")
data class ReflectionEntry(
    @PrimaryKey val dateKey: String,
    val moodImpact: String, // "Worse", "Same", "Better"
    val controlLevel: String, // "Not at all", "Somewhat", "Yes, fully"
    val trigger: String, // "Boredom", "Anxiety", "Habit", "FOMO", "Work need"
    val highlight: String,
    val tomorrowPlan: String,
    val timestamp: Long
)
