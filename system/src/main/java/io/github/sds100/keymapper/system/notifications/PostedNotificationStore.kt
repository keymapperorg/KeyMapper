package io.github.sds100.keymapper.system.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the notifications that [NotificationReceiver] has seen and that are still posted.
 *
 * This is deliberately free of any Android dependency so that its behaviour can be tested, and it
 * lives outside the service because the system destroys and rebinds a notification listener on
 * permission changes and memory pressure, which would wipe state kept on the service itself.
 *
 * Everything here is in memory only and is never written to disk.
 */
class PostedNotificationStore {
    private val _active = MutableStateFlow<List<PostedNotification>>(emptyList())

    /**
     * The notifications that are posted right now, oldest first. Anything that dismisses the most
     * recent notification relies on this order, so new notifications always go on the end.
     */
    val active: StateFlow<List<PostedNotification>> = _active.asStateFlow()

    /**
     * Replace everything that is posted, for when the listener connects and notifications may have
     * been posted while it was not running.
     */
    fun seed(notifications: List<PostedNotification>) {
        _active.update { notifications }
    }

    fun onPosted(notification: PostedNotification) {
        _active.update { active ->
            // Updating a notification in place moves it to the end, because the app just posted it.
            active.filterNot { it.key == notification.key } + notification
        }
    }

    fun onRemoved(key: String) {
        _active.update { active -> active.filterNot { it.key == key } }
    }

    /**
     * Losing the listener may mean the permission was revoked, so no notification content should be
     * left in memory.
     */
    fun clear() {
        _active.update { emptyList() }
    }
}
