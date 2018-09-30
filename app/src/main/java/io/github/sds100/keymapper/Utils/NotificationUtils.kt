package io.github.sds100.keymapper.Utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 30/09/2018.
 */

object NotificationUtils {
    private const val CHANNEL_ID_PERSISTENT = "channel_persistent"
    const val PERSISTENT_NOTIFICATION_ID = 123

    fun buildIMEPickerNotification(ctx: Context): Notification {
        //create the channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    CHANNEL_ID_PERSISTENT,
                    ctx.getString(R.string.notification_channel_persistent),
                    NotificationManager.IMPORTANCE_MIN
            )

            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID_PERSISTENT)
                //TODO change notification icon
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(ctx.getString(R.string.notification_persistent_title))
                .setContentText(ctx.getString(R.string.notification_persistent_text))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET) //hide on lockscreen

        return builder.build()
    }
}