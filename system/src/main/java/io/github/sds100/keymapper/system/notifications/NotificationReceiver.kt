package io.github.sds100.keymapper.system.notifications

import android.app.Notification
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

    @Inject
    lateinit var serviceAdapter: NotificationReceiverAdapterImpl

    @Inject
    lateinit var notificationAdapter: AndroidNotificationAdapter

    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        serviceAdapter.eventsToService
            .onEach { event ->
                when (event) {
                    is NotificationServiceEvent.DismissNotification ->
                        cancelNotification(event.key)

                    NotificationServiceEvent.DismissAllNotifications -> cancelAllNotifications()
                }
            }.launchIn(lifecycleScope)
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { notificationAdapter.onNotificationPosted(it.toPostedNotification()) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        sbn?.let { notificationAdapter.onNotificationRemoved(it.key) }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        seedPostedNotifications()

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

        notificationAdapter.onNotificationListenerDisconnected()

        super.onListenerDisconnected()
    }

    /**
     * Notifications can be posted while the listener is not running, so the list has to be read
     * once on connecting rather than relying on [onNotificationPosted] alone.
     */
    private fun seedPostedNotifications() {
        try {
            val notifications = activeNotifications
                ?.map { it.toPostedNotification() }
                ?: emptyList()

            notificationAdapter.seedNotifications(notifications)
        } catch (e: SecurityException) {
            // Thrown when the listener is not connected yet.
            Timber.e("NotificationReceiver: Failed to read the posted notifications. $e")
        }
    }

    private fun StatusBarNotification.toPostedNotification(): PostedNotification {
        val extras = notification.extras

        return PostedNotification(
            key = key,
            packageName = packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            // Fall back so that notifications using an expanded style still have matchable text.
            text = (
                extras.getCharSequence(Notification.EXTRA_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
                )?.toString(),
        )
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
