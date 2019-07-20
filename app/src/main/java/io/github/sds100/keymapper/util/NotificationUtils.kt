package io.github.sds100.keymapper.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.broadcastreceiver.KeyMapperBroadcastReceiver
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 30/09/2018.
 */

object NotificationUtils {
    const val ID_IME_PERSISTENT = 123
    const val ID_KEYBOARD_HIDDEN = 747
    const val ID_TOGGLE_REMAPPING_PERSISTENT = 231

    const val CHANNEL_ID_WARNINGS = "channel_warnings"
    const val CHANNEL_ID_PERSISTENT = "channel_persistent"

    fun showIMEPickerNotification(ctx: Context) {
        val pendingIntent = IntentUtils.createPendingBroadcastIntent(
                ctx,
                KeyMapperBroadcastReceiver.ACTION_SHOW_IME_PICKER
        )

        showNotification(
                ctx,
                ID_IME_PERSISTENT,
                CHANNEL_ID_PERSISTENT,
                pendingIntent,
                R.drawable.ic_notification_keyboard,
                R.string.notification_ime_persistent_title,
                R.string.notification_ime_persistent_text,
                onGoing = true
        )
    }

    fun invalidateNotifications(ctx: Context) {
        ctx.apply {
            if (defaultSharedPreferences.getBoolean(
                            str(R.string.key_pref_show_ime_notification),
                            bool(R.bool.default_value_show_ime_notification))) {
                showIMEPickerNotification(this)
            } else {
                dismissNotification(this, ID_IME_PERSISTENT)
            }

            WidgetsManager.invalidateNotification(ctx)
        }
    }

    fun dismissNotification(ctx: Context, id: Int) {
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(id)
    }

    fun showNotification(ctx: Context,
                         id: Int,
                         channel: String,
                         intent: PendingIntent? = null,
                         @DrawableRes icon: Int,
                         @StringRes title: Int,
                         @StringRes text: Int,
                         showOnLockscreen: Boolean = false,
                         onGoing: Boolean = false,
                         priority: Int = NotificationCompat.PRIORITY_DEFAULT,
                         vararg actions: NotificationCompat.Action = arrayOf()) {

        ctx.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannels(ctx)
            }

            val builder = NotificationCompat.Builder(ctx, channel).apply {
                color = color(R.color.colorAccent)
                setContentTitle(str(title))
                setContentText(str(text))
                setContentIntent(intent)
                setPriority(priority)


                if (onGoing) {
                    setOngoing(true)
                }

                //can't use vector drawables for KitKat
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    val bitmap = VectorDrawableCompat.create(ctx.resources, icon, ctx.theme)?.toBitmap()
                    setLargeIcon(bitmap)
                    setSmallIcon(R.mipmap.ic_launcher)
                } else {
                    setSmallIcon(icon)
                }

                if (!showOnLockscreen) setVisibility(NotificationCompat.VISIBILITY_SECRET) //hide on lockscreen

                actions.forEach { addAction(it) }
            }


            val notification = builder.build()

            //show the notification
            with(NotificationManagerCompat.from(ctx)) {
                notify(id, notification)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannels(ctx: Context) {
        ctx.apply {
            val channels = listOf(
                    NotificationChannel(
                            CHANNEL_ID_PERSISTENT,
                            str(R.string.notification_channel_persistent),
                            NotificationManager.IMPORTANCE_MIN
                    ),

                    NotificationChannel(
                            CHANNEL_ID_WARNINGS,
                            str(R.string.notification_channel_warnings),
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
            )

            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(channels)
        }
    }
}

fun Context.notification(id: Int,
                         channel: String,
                         intent: PendingIntent? = null,
                         @DrawableRes icon: Int,
                         @StringRes title: Int,
                         @StringRes text: Int,
                         showOnLockscreen: Boolean = false,
                         onGoing: Boolean = false,
                         priority: Int = NotificationCompat.PRIORITY_DEFAULT,
                         vararg actions: NotificationCompat.Action = arrayOf()) = NotificationUtils.showNotification(
        this,
        id,
        channel,
        intent,
        icon,
        title,
        text,
        showOnLockscreen,
        onGoing,
        priority,
        *actions
)