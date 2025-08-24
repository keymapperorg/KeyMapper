package io.github.sds100.keymapper.base.system.notifications

import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationChannelModel
import io.github.sds100.keymapper.system.notifications.NotificationModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageNotificationsUseCaseImpl @Inject constructor(
    private val notificationAdapter: NotificationAdapter,
    private val permissionAdapter: PermissionAdapter,
) : ManageNotificationsUseCase {

    override val onActionClick: Flow<String> = notificationAdapter.onNotificationActionClick

    override fun show(notification: NotificationModel) {
        notificationAdapter.showNotification(notification)
    }

    override fun dismiss(notificationId: Int) {
        notificationAdapter.dismissNotification(notificationId)
    }

    override fun createChannel(channel: NotificationChannelModel) {
        notificationAdapter.createChannel(channel)
    }

    override fun deleteChannel(channelId: String) {
        notificationAdapter.deleteChannel(channelId)
    }

    override fun isPermissionGranted(): Boolean {
        return permissionAdapter.isGranted(Permission.POST_NOTIFICATIONS)
    }
}

interface ManageNotificationsUseCase {
    /**
     * The string is the ID of the action.
     */
    val onActionClick: Flow<String>

    fun isPermissionGranted(): Boolean
    fun show(notification: NotificationModel)
    fun dismiss(notificationId: Int)
    fun createChannel(channel: NotificationChannelModel)
    fun deleteChannel(channelId: String)
}
