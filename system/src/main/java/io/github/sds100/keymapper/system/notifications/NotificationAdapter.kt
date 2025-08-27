package io.github.sds100.keymapper.system.notifications

import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import kotlinx.coroutines.flow.Flow

interface NotificationAdapter {
    /**
     * The string is the ID of the action.
     */
    val onNotificationActionClick: Flow<KMNotificationAction.IntentAction>

    /**
     * Emits text input from notification actions that support RemoteInput.
     */
    val onNotificationRemoteInput: Flow<NotificationRemoteInput>

    fun showNotification(notification: NotificationModel)
    fun dismissNotification(notificationId: Int)
    fun createChannel(channel: NotificationChannelModel)
    fun deleteChannel(channelId: String)
    fun openChannelSettings(channelId: String)
}
