package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 30/01/21.
 */
interface INotificationManagerWrapper {
    fun showNotification(notification: AppNotification)
    fun dismissNotification(notification: AppNotification)
    fun createChannel(vararg channelId: String)
    fun deleteChannel(channelId: String)
}