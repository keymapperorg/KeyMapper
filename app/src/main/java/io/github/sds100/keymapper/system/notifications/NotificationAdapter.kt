package io.github.sds100.keymapper.system.notifications

import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 16/04/2021.
 */
interface NotificationAdapter {
    /**
     * The string is the ID of the action.
     */
    val onNotificationActionClick: Flow<String>

    fun showNotification(notification: NotificationModel)
    fun dismissNotification(notificationId: Int)
    fun createChannel(channel: NotificationChannelModel)
    fun deleteChannel(channelId: String)
}
