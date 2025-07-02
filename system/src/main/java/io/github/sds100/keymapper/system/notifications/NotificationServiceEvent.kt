package io.github.sds100.keymapper.system.notifications

import kotlinx.serialization.Serializable

sealed class NotificationServiceEvent {

    @Serializable
    data object DismissLastNotification : NotificationServiceEvent()

    @Serializable
    data object DismissAllNotifications : NotificationServiceEvent()

}