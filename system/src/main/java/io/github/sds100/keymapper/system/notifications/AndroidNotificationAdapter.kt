package io.github.sds100.keymapper.system.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.KeyMapperClassProvider
import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class AndroidNotificationAdapter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val classProvider: KeyMapperClassProvider,
    private val notificationReceiverAdapter: NotificationReceiverAdapter,
) : NotificationAdapter {

    private val manager: NotificationManagerCompat = NotificationManagerCompat.from(ctx)

    override val onNotificationActionClick = MutableSharedFlow<KMNotificationAction.IntentAction>()
    override val onNotificationRemoteInput = MutableSharedFlow<NotificationRemoteInput>()

    private val postedNotificationStore = PostedNotificationStore()

    override val activeNotifications: StateFlow<List<PostedNotification>> =
        postedNotificationStore.active

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return
            intent ?: return

            onReceiveNotificationActionIntent(intent)

            // dismiss the notification drawer after tapping on the notification. This is deprecated on S+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS).apply {
                    context.sendBroadcast(this)
                }
            }
        }
    }

    init {
        val intentFilter = IntentFilter().apply {
            for (entry in KMNotificationAction.IntentAction.entries) {
                addAction(entry.name)
            }
        }
        ContextCompat.registerReceiver(
            ctx,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun showNotification(notification: NotificationModel) {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val builder = NotificationCompat.Builder(ctx, notification.channel).apply {
            // The system tints the notification itself when dynamic colours are available.
            if (!DynamicColors.isDynamicColorAvailable()) {
                color = ContextCompat.getColor(ctx, R.color.notification_accent)
            }

            setContentTitle(notification.title)
            setContentText(notification.text)

            if (notification.onClickAction != null) {
                val pendingIntent = createActionIntent(notification.onClickAction)
                setContentIntent(pendingIntent)
            }

            setAutoCancel(notification.autoCancel)
            priority = notification.priority

            if (notification.onGoing) {
                setOngoing(true)
            }

            if (notification.bigTextStyle) {
                setStyle(NotificationCompat.BigTextStyle())
            }

            if (notification.timeout != null) {
                this.setTimeoutAfter(notification.timeout)
            }

            if (notification.showIndeterminateProgress) {
                setProgress(1, 1, true)
            }

            setSmallIcon(notification.icon)

            if (!notification.showOnLockscreen) {
                setVisibility(NotificationCompat.VISIBILITY_SECRET)
            }

            for ((action, label) in notification.actions) {
                val pendingIntent = createActionIntent(action)

                val notificationAction = NotificationCompat.Action.Builder(
                    0,
                    label,
                    pendingIntent,
                )

                if (action is KMNotificationAction.RemoteInput) {
                    val remoteInput = RemoteInput.Builder(action.key)
                        .setLabel(label)
                        .build()

                    notificationAction.addRemoteInput(remoteInput)
                }

                addAction(notificationAction.build())
            }

            setSilent(notification.silent)
        }

        manager.notify(notification.id, builder.build())
    }

    override fun dismissNotification(notificationId: Int) {
        manager.cancel(notificationId)
    }

    override fun createChannel(channel: NotificationChannelModel) {
        val androidChannel = NotificationChannel(
            channel.id,
            channel.name,
            channel.importance,
        )

        manager.createNotificationChannel(androidChannel)
    }

    override fun deleteChannel(channelId: String) {
        manager.deleteNotificationChannel(channelId)
    }

    override fun openChannelSettings(channelId: String) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            ctx.startActivity(this)
        }
    }

    fun onReceiveNotificationActionIntent(intent: Intent) {
        val intentAction =
            KMNotificationAction.IntentAction.entries.single { it.name == intent.action }

        Timber.d("Received notification click: actionId=$intentAction")

        // Check if there's RemoteInput data
        val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)

        if (remoteInputBundle != null) {
            for (key in remoteInputBundle.keySet()) {
                val text = remoteInputBundle.getCharSequence(key)?.toString()

                if (!text.isNullOrEmpty()) {
                    coroutineScope.launch {
                        onNotificationRemoteInput.emit(NotificationRemoteInput(intentAction, text))
                    }

                    return
                }
            }
        }

        // No text input, treat as regular action click
        coroutineScope.launch {
            onNotificationActionClick.emit(intentAction)
        }
    }

    private fun createActionIntent(notificationAction: KMNotificationAction): PendingIntent {
        return when (notificationAction) {
            KMNotificationAction.Activity.AccessibilitySettings -> createActivityPendingIntent(
                Settings.ACTION_ACCESSIBILITY_SETTINGS,
            )

            is KMNotificationAction.Activity.MainActivity -> createMainActivityPendingIntent(
                notificationAction.action,
            )

            is KMNotificationAction.Broadcast -> createBroadcastPendingIntent(
                notificationAction.intentAction.name,
            )

            is KMNotificationAction.RemoteInput -> createRemoteInputPendingIntent(
                notificationAction.intentAction.name,
            )
        }
    }

    private fun createRemoteInputPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(ctx.packageName)
        }

        return PendingIntent.getBroadcast(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun createBroadcastPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(ctx.packageName)
        }

        return PendingIntent.getBroadcast(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createActivityPendingIntent(action: String): PendingIntent {
        val intent = Intent(action)

        return PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createMainActivityPendingIntent(action: String?): PendingIntent {
        val intent = Intent(ctx, classProvider.getMainActivity()).apply {
            this.action = action ?: Intent.ACTION_MAIN
        }

        return PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override suspend fun dismissAllNotifications(): KMResult<*> =
        notificationReceiverAdapter.send(NotificationServiceEvent.DismissAllNotifications)

    override suspend fun dismissLastNotification(): KMResult<*> {
        val key = activeNotifications.value.lastOrNull()?.key ?: return Success(Unit)

        return notificationReceiverAdapter.send(NotificationServiceEvent.DismissNotification(key))
    }

    /**
     * Replace everything that is posted, for when the listener connects.
     *
     * Only for [NotificationReceiver].
     */
    fun seedNotifications(notifications: List<PostedNotification>) {
        postedNotificationStore.seed(notifications)
    }

    /**
     * Only for [NotificationReceiver].
     */
    fun onNotificationPosted(notification: PostedNotification) {
        postedNotificationStore.onPosted(notification)
    }

    /**
     * Only for [NotificationReceiver].
     */
    fun onNotificationRemoved(key: String) {
        postedNotificationStore.onRemoved(key)
    }

    /**
     * Only for [NotificationReceiver].
     */
    fun onNotificationListenerDisconnected() {
        postedNotificationStore.clear()
    }
}
