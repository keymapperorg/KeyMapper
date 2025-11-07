package io.github.sds100.keymapper.system.notifications

import android.content.ComponentName
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.system.media.AndroidMediaAdapter
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@AndroidEntryPoint
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

    @Inject
    lateinit var mediaAdapter: AndroidMediaAdapter

    private val activeSessionsChangeListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            mediaAdapter.onActiveMediaSessionChange(controllers ?: emptyList())
        }

    private var lastNotificationKey: String? = null

    @Inject
    lateinit var serviceAdapter: NotificationReceiverAdapterImpl

    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        serviceAdapter.eventsToService
            .onEach { event ->
                when (event) {
                    NotificationServiceEvent.DismissLastNotification -> cancelNotification(
                        lastNotificationKey,
                    )
                    NotificationServiceEvent.DismissAllNotifications -> cancelAllNotifications()
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
