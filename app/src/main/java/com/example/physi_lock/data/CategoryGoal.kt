package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Module 6 (Personalization & User Control): a persisted per-category daily usage goal
// (Usage Goals screen), keyed by AppCategoryType (SOCIAL_MEDIA, ENTERTAINMENT, GAMES).
// Absence of a row for a category just means the user hasn't customized that goal yet —
// UsageGoalsViewModel falls back to the ported UI's original preset default in that case.
@Entity(tableName = "category_goals")
data class CategoryGoal(
    @PrimaryKey val categoryType: String,
    val targetMs: Long
)
