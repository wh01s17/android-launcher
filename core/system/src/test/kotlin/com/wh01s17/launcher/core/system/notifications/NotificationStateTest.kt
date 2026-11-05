package com.wh01s17.launcher.core.system.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationStateTest {
    private fun item(
        key: String,
        packageName: String,
        userSerial: Long = 0,
    ) = NotificationItem(key, packageName, userSerial, "t", "x", 0, clearable = true, autoCancel = true, open = null)

    @Test
    fun `badges count the notifications of each app`() {
        val state = NotificationState(true, listOf(item("1", "chat"), item("2", "chat"), item("3", "mail")))
        assertEquals(mapOf(BadgeKey("chat", 0) to 2, BadgeKey("mail", 0) to 1), state.badges)
    }

    @Test
    fun `the same app in the work profile has its own badge`() {
        val state = NotificationState(true, listOf(item("1", "chat", 0), item("2", "chat", 10), item("3", "chat", 10)))
        assertEquals(1, state.badges[BadgeKey("chat", 0)])
        assertEquals(2, state.badges[BadgeKey("chat", 10)])
    }

    @Test
    fun `the summary has the access and the total`() {
        val state = NotificationState(true, listOf(item("1", "chat"), item("2", "mail")))
        assertEquals(NotificationSummary(granted = true, count = 2), state.summary)
    }

    @Test
    fun `without access there is nothing to count`() {
        val state = NotificationState()
        assertFalse(state.access)
        assertEquals(NotificationSummary(), state.summary)
        assertEquals(emptyMap<BadgeKey, Int>(), state.badges)
    }
}
