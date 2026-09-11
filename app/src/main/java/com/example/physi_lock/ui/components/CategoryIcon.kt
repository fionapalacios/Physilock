package com.example.physi_lock.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.physi_lock.data.entity.AppCategoryType

/** Real vector icon per app category, replacing the decorative emoji this project's design
 *  standards reject (2026-09-10 emoji sweep). Shared between Reports' Category Breakdown and
 *  Usage Goals' per-category goal cards so both screens use the same icon for the same
 *  category instead of two independently-drifted emoji sets. [categoryType] null (the "Total
 *  Daily Screen Time" pseudo-category on Usage Goals) falls to the same generic icon as an
 *  unrecognized/uncategorized type. */
fun categoryIcon(categoryType: String?): ImageVector = when (categoryType) {
    AppCategoryType.SOCIAL_MEDIA -> Icons.Filled.Groups
    AppCategoryType.ENTERTAINMENT -> Icons.Filled.Movie
    AppCategoryType.GAMES -> Icons.Filled.SportsEsports
    AppCategoryType.PRODUCTIVITY -> Icons.Filled.Work
    else -> Icons.Filled.Apps
}
