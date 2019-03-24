package io.github.sds100.keymapper.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.broadcastreceiver.OpenIMEPickerBroadcastReceiver

/**
 * Created by sds100 on 30/09/2018.
 */

object NotificationUtils {
    const val ID_IME_PERSISTENT = 123
    private const val CHANNEL_ID_PERSISTENT = "channel_persistent"

    fun showIMEPickerNotification(ctx: Context) {
        val intent = Intent(ctx, OpenIMEPickerBroadcastReceiver::class.java).apply {
            action = OpenIMEPickerBroadcastReceiver.ACTION_SHOW_IME_PICKER
        }

        val pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0)

        showPersistentNotification(
                ctx,
                ID_IME_PERSISTENT,
                pendingIntent,
                R.drawable.ic_keyboard_notification,
                R.string.notification_ime_persistent_title,
                R.string.notification_ime_persistent_text
        )
    }
    fun hideNotification(ctx: Context, id: Int) {
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(id)
    }

    private fun showPersistentNotification(ctx: Context,
                                           id: Int,
                                           intent: PendingIntent,
                                           @DrawableRes icon: Int,
                                           @StringRes title: Int,
                                           @StringRes text: Int) {
        ctx.apply {
            //create the channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val channel = NotificationChannel(
                        CHANNEL_ID_PERSISTENT,
                        str(R.string.notification_channel_persistent),
                        NotificationManager.IMPORTANCE_MIN
                )

                val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(ctx, CHANNEL_ID_PERSISTENT)
                    //TODO change notification icon
                    .setSmallIcon(icon)
                    .setColor(color(R.color.colorAccent))
                    .setContentTitle(str(title))
                    .setContentText(str(text))
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(true)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET) //hide on lockscreen
                    .setContentIntent(intent) //show IME picker on click

            val notification = builder.build()

            //show the notification
            with(NotificationManagerCompat.from(ctx)) {
                notify(id, notification)
            }
        }
    }
}