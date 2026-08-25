package com.example.physi_lock.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Trivial fake -- categoryTotals only ever calls getAll(), so upsert/delete just assert
 *  they're unused rather than pretending to implement real persistence. */
private class FakeAppCategoryDao(private val categories: List<AppCategory>) : AppCategoryDao {
    override suspend fun upsert(category: AppCategory) =
        throw UnsupportedOperationException("not exercised by categoryTotals")

    override fun getAll(): Flow<List<AppCategory>> = flowOf(categories)

    override suspend fun delete(packageName: String) =
        throw UnsupportedOperationException("not exercised by categoryTotals")
}

class CategoryUsageAggregatorTest {

    @Test
    fun `buckets known packages by their curated category`() = runBlocking {
        val dao = FakeAppCategoryDao(
            listOf(
                AppCategory("com.instagram.android", "Instagram", AppCategoryType.SOCIAL_MEDIA),
                AppCategory("com.netflix.mediaclient", "Netflix", AppCategoryType.ENTERTAINMENT)
            )
        )
        val packageTotals = mapOf(
            "com.instagram.android" to 60_000L,
            "com.netflix.mediaclient" to 30_000L
        )

        val totals = categoryTotals(packageTotals, dao)

        assertEquals(60_000L, totals[AppCategoryType.SOCIAL_MEDIA])
        assertEquals(30_000L, totals[AppCategoryType.ENTERTAINMENT])
    }

    @Test
    fun `uncategorized packages fall into OTHER instead of being dropped`() = runBlocking {
        val dao = FakeAppCategoryDao(emptyList())
        val packageTotals = mapOf("com.unknown.app" to 45_000L)

        val totals = categoryTotals(packageTotals, dao)

        assertEquals(45_000L, totals[AppCategoryType.OTHER])
    }

    @Test
    fun `sums multiple packages that share the same category`() = runBlocking {
        val dao = FakeAppCategoryDao(
            listOf(
                AppCategory("com.instagram.android", "Instagram", AppCategoryType.SOCIAL_MEDIA),
                AppCategory("com.twitter.android", "Twitter", AppCategoryType.SOCIAL_MEDIA)
            )
        )
        val packageTotals = mapOf(
            "com.instagram.android" to 60_000L,
            "com.twitter.android" to 40_000L
        )

        val totals = categoryTotals(packageTotals, dao)

        assertEquals(100_000L, totals[AppCategoryType.SOCIAL_MEDIA])
    }

    @Test
    fun `empty package totals yields empty result`() = runBlocking {
        val dao = FakeAppCategoryDao(emptyList())
        val totals = categoryTotals(emptyMap(), dao)
        assertEquals(true, totals.isEmpty())
        assertNull(totals[AppCategoryType.OTHER])
    }
}
