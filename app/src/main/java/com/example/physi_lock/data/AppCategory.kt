package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Admin-curated master list of which apps count as "social media"/distracting,
// used for tracking & risk scoring (see project memory "Admin scope").
object AppCategoryType {
    const val SOCIAL_MEDIA = "SOCIAL_MEDIA"
    const val ENTERTAINMENT = "ENTERTAINMENT"
    const val GAMES = "GAMES"
    const val PRODUCTIVITY = "PRODUCTIVITY"
    const val OTHER = "OTHER"

    val all = listOf(SOCIAL_MEDIA, ENTERTAINMENT, GAMES, PRODUCTIVITY, OTHER)

    fun label(value: String): String = when (value) {
        SOCIAL_MEDIA -> "Social Media"
        ENTERTAINMENT -> "Entertainment"
        GAMES -> "Games"
        PRODUCTIVITY -> "Productivity"
        else -> "Other"
    }
}

@Entity(tableName = "app_categories")
data class AppCategory(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String = AppCategoryType.OTHER
)