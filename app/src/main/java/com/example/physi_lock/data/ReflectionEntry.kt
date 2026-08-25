package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Module 5 (Mental Health & Awareness): one real entry per calendar day (dateKey is the
// primary key), matching the "Daily Reflection" framing already used on Home — answering
// again the same day edits that day's entry rather than creating a duplicate. promptText
// is stored per-entry (not looked up later) so history stays accurate even if the rotating
// prompt list changes in a future update.
@Entity(tableName = "reflection_entries")
data class ReflectionEntry(
    @PrimaryKey val dateKey: String,
    val promptText: String,
    val moodRating: Int, // 1 (struggling) .. 5 (great)
    val answerText: String,
    val timestamp: Long
)
