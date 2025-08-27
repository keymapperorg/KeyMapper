package io.github.sds100.keymapper.system.notifications

import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.common.notifications.KMNotificationAction


data class NotificationModel(
    val id: Int,
    val channel: String,
    val title: String,
    val text: String,
    @DrawableRes val icon: Int,
    /**
     * Null if nothing should happen when the notification is tapped.
     */
    val onClickAction: KMNotificationAction? = null,
    val showOnLockscreen: Boolean,
    val onGoing: Boolean,
    /**
     * On Android Oreo and newer this does nothing because the channel priority is used.
     */
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT,

    /**
     * Maps the action intent to the label string.
     */
    val actions: List<Pair<KMNotificationAction, String>> = emptyList(),

    /**
     * Clicking on the notification will automatically dismiss it.
     */
    val autoCancel: Boolean = false,
    val bigTextStyle: Boolean = false,
    val silent: Boolean = false
)

