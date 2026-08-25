package com.example.physi_lock.data

import kotlinx.coroutines.flow.first

/** Shared by [com.example.physi_lock.ui.reports.ReportsViewModel] (Category Breakdown) and
 *  [com.example.physi_lock.ui.goals.UsageGoalsViewModel] (per-category goal "current" values) —
 *  buckets a package→duration map by each app's Admin-curated category (see
 *  AdminCategoriesSection). Apps Admin never categorized fall into [AppCategoryType.OTHER],
 *  same as the entity's own default — not fabricated, just uncategorized. */
suspend fun categoryTotals(
    packageTotals: Map<String, Long>,
    appCategoryDao: AppCategoryDao
): Map<String, Long> {
    val categoryByPackage = try {
        appCategoryDao.getAll().first().associate { it.packageName to it.category }
    } catch (e: Exception) {
        emptyMap()
    }

    val totals = mutableMapOf<String, Long>()
    packageTotals.forEach { (packageName, ms) ->
        val category = categoryByPackage[packageName] ?: AppCategoryType.OTHER
        totals[category] = (totals[category] ?: 0L) + ms
    }
    return totals
}
