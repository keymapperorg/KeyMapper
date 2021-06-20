package io.github.sds100.keymapper.system.notifications

import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.ServiceLocator

/**
 * Created by sds100 on 14/11/20.
 */
class NotificationReceiver : NotificationListenerService() {
    private val mediaSessionManager: MediaSessionManager by lazy { getSystemService()!! }

    private val notificationListenerComponent by lazy { ComponentName(this, NotificationReceiver::class.java) }

    private val activeSessionsChangeListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            ServiceLocator.mediaAdapter(this).onActiveMediaSessionChange(controllers ?: emptyList())
        }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        mediaSessionManager.addOnActiveSessionsChangedListener(
            activeSessionsChangeListener,
            notificationListenerComponent
        )
    }

    override fun onListenerDisconnected() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangeListener)

        ServiceLocator.mediaAdapter(this).onActiveMediaSessionChange(emptyList())

        super.onListenerDisconnected()
    }
}