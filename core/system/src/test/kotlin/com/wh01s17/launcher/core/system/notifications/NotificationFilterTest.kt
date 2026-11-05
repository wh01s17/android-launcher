package com.wh01s17.launcher.core.system.notifications

import android.app.Notification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFilterTest {
    private fun accepts(
        packageName: String = "com.chat",
        flags: Int = 0,
        title: String? = "Ana",
        text: String? = "hola",
        media: Boolean = false,
    ) = NotificationFilter.accepts(packageName, "com.wh01s17.launcher", flags, title, text, media)

    @Test
    fun `an ordinary message is shown`() = assertTrue(accepts())

    @Test
    fun `the launcher's own notifications are left out`() = assertFalse(accepts(packageName = "com.wh01s17.launcher"))

    @Test
    fun `group summaries are left out because each message is already listed`() =
        assertFalse(accepts(flags = Notification.FLAG_GROUP_SUMMARY))

    @Test
    fun `ongoing notifications are left out`() = assertFalse(accepts(flags = Notification.FLAG_ONGOING_EVENT))

    @Test
    fun `a notification with only a title or only a text is shown`() {
        assertTrue(accepts(text = null))
        assertTrue(accepts(title = null))
    }

    @Test
    fun `a notification without any text is left out`() {
        assertFalse(accepts(title = null, text = null))
        assertFalse(accepts(title = " ", text = ""))
    }

    @Test
    fun `other flags do not hide it`() = assertTrue(accepts(flags = Notification.FLAG_AUTO_CANCEL))

    @Test
    fun `music is shown while it plays and hidden while paused`() {
        assertTrue(accepts(flags = Notification.FLAG_ONGOING_EVENT, media = true))
        assertFalse(accepts(media = true))
    }
}
