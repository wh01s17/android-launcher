package com.wh01s17.launcher.core.system.notifications

import android.app.Notification
import android.content.Context
import android.os.UserManager
import android.service.notification.StatusBarNotification

/** Las notificaciones que enseña el launcher, de la más nueva a la más antigua. */
internal fun List<StatusBarNotification>.toItems(context: Context): List<NotificationItem> {
    val users = context.getSystemService(UserManager::class.java)
    return mapNotNull { it.toItem(context.packageName) { user -> users.getSerialNumberForUser(user) } }
        .sortedByDescending { it.postTime }
}

private fun StatusBarNotification.toItem(
    ownPackage: String,
    serialOf: (android.os.UserHandle) -> Long,
): NotificationItem? {
    val extras = notification.extras
    val title = extras?.getCharSequence(Notification.EXTRA_TITLE)
    val text = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras?.getCharSequence(Notification.EXTRA_TEXT)
    val media = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true || notification.category == Notification.CATEGORY_TRANSPORT
    if (!NotificationFilter.accepts(packageName, ownPackage, notification.flags, title, text, media)) return null
    return NotificationItem(
        key = key,
        packageName = packageName,
        userSerial = serialOf(user),
        title = title?.toString(),
        text = text?.toString(),
        postTime = postTime,
        clearable = isClearable,
        autoCancel = notification.flags and Notification.FLAG_AUTO_CANCEL != 0,
        open = notification.contentIntent,
    )
}
