package io.github.sds100.keymapper.system.notifications

import kotlinx.serialization.Serializable

sealed class NotificationServiceEvent {

    /**
     * @param key the [PostedNotification.key] of the notification to dismiss.
     */
    @Serializable
    data class DismissNotification(val key: String) : NotificationServiceEvent()

    @Serializable
    data object DismissAllNotifications : NotificationServiceEvent()
}
