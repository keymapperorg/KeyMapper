package io.github.sds100.keymapper.system.notifications

import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NotificationAdapter {
    /**
     * The string is the ID of the action.
     */
    val onNotificationActionClick: Flow<KMNotificationAction.IntentAction>

    /**
     * Emits text input from notification actions that support RemoteInput.
     */
    val onNotificationRemoteInput: Flow<NotificationRemoteInput>

    /**
     * The notifications that are posted right now. This requires the notification listener
     * permission and is empty without it.
     */
    val activeNotifications: StateFlow<List<PostedNotification>>

    fun showNotification(notification: NotificationModel)
    fun dismissNotification(notificationId: Int)
    fun createChannel(channel: NotificationChannelModel)
    fun deleteChannel(channelId: String)
    fun openChannelSettings(channelId: String)

    /**
     * Dismiss every notification that Key Mapper is allowed to dismiss.
     */
    suspend fun dismissAllNotifications(): KMResult<*>

    /**
     * Dismiss the notification that was posted most recently. Does nothing if none are posted.
     */
    suspend fun dismissLastNotification(): KMResult<*>
}
