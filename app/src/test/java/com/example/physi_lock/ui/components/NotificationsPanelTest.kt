package com.example.physi_lock.ui.components

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * formatRelativeTime always re-reads System.currentTimeMillis() internally, slightly later
 * than each test computes `timestamp` -- that only ever *grows* elapsed time between the two
 * reads, never shrinks it, so truncation (toMinutes/toHours/toDays) can only round the same
 * way or up, never down across a boundary. That's what keeps these assertions deterministic
 * rather than flaky.
 */
class NotificationsPanelTest {

    @Test
    fun `less than a minute ago reads Just now`() {
        val timestamp = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30)
        assertEquals("Just now", formatRelativeTime(timestamp))
    }

    @Test
    fun `minutes ago is formatted in whole minutes`() {
        val timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)
        assertEquals("5m ago", formatRelativeTime(timestamp))
    }

    @Test
    fun `just under an hour still reads in minutes`() {
        val timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(59)
        assertEquals("59m ago", formatRelativeTime(timestamp))
    }

    @Test
    fun `hours ago is formatted in whole hours`() {
        val timestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)
        assertEquals("3h ago", formatRelativeTime(timestamp))
    }

    @Test
    fun `a day or more ago is formatted in whole days`() {
        val timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
        assertEquals("2d ago", formatRelativeTime(timestamp))
    }

    @Test
    fun `a future timestamp never goes negative`() {
        val timestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)
        assertEquals("Just now", formatRelativeTime(timestamp))
    }
}
