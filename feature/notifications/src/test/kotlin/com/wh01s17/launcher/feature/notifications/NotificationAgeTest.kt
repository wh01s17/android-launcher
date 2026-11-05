package com.wh01s17.launcher.feature.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationAgeTest {
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test
    fun `it picks the biggest unit that fits`() {
        assertEquals(NotificationAge.Now, NotificationAge.of(100 * day, 100 * day - 59_000))
        assertEquals(NotificationAge.Minutes(1), NotificationAge.of(100 * day, 100 * day - minute))
        assertEquals(NotificationAge.Minutes(59), NotificationAge.of(100 * day, 100 * day - 59 * minute - 30_000))
        assertEquals(NotificationAge.Hours(1), NotificationAge.of(100 * day, 100 * day - hour))
        assertEquals(NotificationAge.Hours(23), NotificationAge.of(100 * day, 100 * day - 23 * hour - 59 * minute))
        assertEquals(NotificationAge.Days(2), NotificationAge.of(100 * day, 98 * day))
    }

    @Test
    fun `a notification from the future counts as now`() {
        assertEquals(NotificationAge.Now, NotificationAge.of(1_000, 5_000))
    }
}
