package io.github.sds100.keymapper.system.notifications

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.color.DynamicColors
import io.github.sds100.keymapper.MainActivity
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.base.util.color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNotificationAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
) : NotificationAdapter {

    private val ctx = context.applicationContext
    private val manager: NotificationManagerCompat = NotificationManagerCompat.from(ctx)

    override val onNotificationActionClick = MutableSharedFlow<String>()

    override fun showNotification(notification: NotificationModel) {
        val builder = NotificationCompat.Builder(ctx, notification.channel).apply {
            if (!DynamicColors.isDynamicColorAvailable()) {
                color = ctx.color(R.color.md_theme_secondary)
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

            setSmallIcon(notification.icon)

            if (!notification.showOnLockscreen) {
                setVisibility(NotificationCompat.VISIBILITY_SECRET)
            }

            for (action in notification.actions) {
                addAction(
                    NotificationCompat.Action(
                        0,
                        action.text,
                        createActionIntent(action.intentType),
                    ),
                )
            }
        }

        manager.notify(notification.id, builder.build())
    }

    override fun dismissNotification(notificationId: Int) {
        manager.cancel(notificationId)
    }

    override fun createChannel(channel: NotificationChannelModel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channel.id,
                    channel.name,
                    channel.importance,
                ),
            )
        }
    }

    override fun deleteChannel(channelId: String) {
        manager.deleteNotificationChannel(channelId)
    }

    fun onReceiveNotificationActionIntent(intent: Intent) {
        val actionId = intent.action ?: return

        coroutineScope.launch {
            onNotificationActionClick.emit(actionId)
        }
    }

    private fun createActionIntent(intentType: NotificationIntentType): PendingIntent {
        when (intentType) {
            is NotificationIntentType.Broadcast -> {
                val intent = Intent(ctx, NotificationClickReceiver::class.java).apply {
                    action = intentType.action
                }

                return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }

            is NotificationIntentType.MainActivity -> {
                val intent = Intent(ctx, MainActivity::class.java).apply {
                    action = intentType.customIntentAction ?: Intent.ACTION_MAIN
                }

                return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }

            is NotificationIntentType.Activity -> {
                val intent = Intent(intentType.action)

                return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }
        }
    }
}
