package io.github.sds100.keymapper.system.notifications

import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.base.utils.ServiceEvent
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber


class NotificationReceiver :
    NotificationListenerService(),
    LifecycleOwner {
    private val mediaSessionManager: MediaSessionManager by lazy { getSystemService()!! }

    private val notificationListenerComponent by lazy {
        ComponentName(
            this,
            NotificationReceiver::class.java,
        )
    }

    private val mediaAdapter by lazy { ServiceLocator.mediaAdapter(this) }

    private val activeSessionsChangeListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            mediaAdapter.onActiveMediaSessionChange(controllers ?: emptyList())
        }

    private var lastNotificationKey: String? = null

    private val serviceAdapter: NotificationReceiverAdapterImpl by lazy {
        ServiceLocator.notificationReceiverAdapter(this)
    }

    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        serviceAdapter.eventsToService
            .onEach { event ->
                when (event) {
                    ServiceEvent.DismissLastNotification -> cancelNotification(lastNotificationKey)
                    ServiceEvent.DismissAllNotifications -> cancelAllNotifications()
                    else -> Unit
                }
            }.launchIn(lifecycleScope)
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        lastNotificationKey = sbn?.key
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        if (sbn?.key == lastNotificationKey) {
            lastNotificationKey = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        super.onStartCommand(intent, flags, startId)

    override fun onListenerConnected() {
        super.onListenerConnected()

        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                activeSessionsChangeListener,
                notificationListenerComponent,
            )

            val activeSessions =
                mediaSessionManager.getActiveSessions(notificationListenerComponent)
            mediaAdapter.onActiveMediaSessionChange(activeSessions)
        } catch (e: SecurityException) {
            Timber.e(
                "NotificationReceiver: " +
                    "Failed to add active sessions changed listener due to security exception. $e",
            )
        }
    }

    override fun onListenerDisconnected() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangeListener)

        mediaAdapter.onActiveMediaSessionChange(emptyList())

        super.onListenerDisconnected()
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
