package io.github.sds100.keymapper.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O_MR1
import android.os.Build.VERSION_CODES.Q
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.broadcastreceiver.KeyMapperBroadcastReceiver
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.activity.HomeActivity
import io.github.sds100.keymapper.util.result.onSuccess
import splitties.init.appCtx
import splitties.systemservices.notificationManager

/**
 * Created by sds100 on 30/09/2018.
 */

object NotificationUtils {
    const val ID_IME_PICKER = 123
    const val ID_KEYBOARD_HIDDEN = 747
    const val ID_TOGGLE_KEYMAPS = 231
    const val ID_TOGGLE_KEYBOARD = 143

    const val CHANNEL_TOGGLE_REMAPS = "channel_toggle_remaps"
    const val CHANNEL_IME_PICKER = "channel_ime_picker"
    const val CHANNEL_KEYBOARD_HIDDEN = "channel_warning_keyboard_hidden"
    const val CHANNEL_TOGGLE_KEYBOARD = "channel_toggle_keymapper_keyboard"

    @Deprecated("Removed in 2.0. This channel shouldn't exist")
    const val CHANNEL_ID_WARNINGS = "channel_warnings"

    @Deprecated("Removed in 2.0. This channel shouldn't exist")
    const val CHANNEL_ID_PERSISTENT = "channel_persistent"

    fun updateToggleKeymapsNotification(ctx: Context, @WidgetsManager.Event event: Int) {
        if (SDK_INT < Build.VERSION_CODES.O) {
            val showNotification = AppPreferences.showToggleKeymapsNotification

            if (!showNotification) {
                dismissNotification(ID_TOGGLE_KEYMAPS)
                return
            }
        }

        val onClickPendingIntent: PendingIntent
        val actions = mutableListOf<NotificationCompat.Action>()

        @StringRes val titleRes: Int
        @StringRes val textRes: Int
        @DrawableRes val iconRes: Int

        when (event) {
            WidgetsManager.EVENT_PAUSE_REMAPS -> {
                titleRes = R.string.notification_remappings_start_title
                textRes = R.string.notification_remappings_start_text
                iconRes = R.drawable.ic_notification_play

                onClickPendingIntent = IntentUtils.createPendingBroadcastIntent(
                    ctx,
                    MyAccessibilityService.ACTION_RESUME_REMAPPINGS
                )

                if (AppPreferences.toggleKeyboardOnToggleKeymaps) {
                    AppPreferences.defaultIme?.let {
                        KeyboardUtils.switchIme(ctx, it)
                    }
                }
            }

            WidgetsManager.EVENT_RESUME_REMAPS -> {
                titleRes = R.string.notification_remappings_pause_title
                textRes = R.string.notification_remappings_pause_text
                iconRes = R.drawable.ic_notification_pause

                onClickPendingIntent = IntentUtils.createPendingBroadcastIntent(
                    ctx,
                    MyAccessibilityService.ACTION_PAUSE_REMAPPINGS
                )

                if (event == WidgetsManager.EVENT_RESUME_REMAPS) {
                    val defaultIme = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

                    KeyboardUtils.getImeId(KeyboardUtils.selectedImePackageName).onSuccess {
                        if (defaultIme != it) {
                            AppPreferences.defaultIme = defaultIme
                        }
                    }

                    if (AppPreferences.toggleKeyboardOnToggleKeymaps) {
                        KeyboardUtils.getImeId(KeyboardUtils.selectedImePackageName).onSuccess {
                            KeyboardUtils.switchIme(ctx, it)
                        }
                    }
                }
            }

            WidgetsManager.EVENT_ACCESSIBILITY_SERVICE_STOPPED -> {
                titleRes = R.string.notification_accessibility_service_disabled_title
                textRes = R.string.notification_accessibility_service_disabled_text
                iconRes = R.drawable.ic_notification_error

                onClickPendingIntent = IntentUtils.createPendingBroadcastIntent(
                    ctx,
                    MyAccessibilityService.ACTION_START
                )
            }

            else -> return
        }

        if ((event == WidgetsManager.EVENT_RESUME_REMAPS) or (event == WidgetsManager.EVENT_PAUSE_REMAPS)) {

            val stopAccessibilityServicePendingIntent = IntentUtils.createPendingBroadcastIntent(
                ctx,
                MyAccessibilityService.ACTION_STOP
            )

            val dismissPendingIntent = IntentUtils.createPendingBroadcastIntent(
                ctx,
                KeyMapperBroadcastReceiver.ACTION_DISMISS_PAUSE_KEYMAPS_NOTIFICATION
            )

            actions.add(NotificationCompat.Action(
                0,
                ctx.str(R.string.notification_action_stop_acc_service),
                stopAccessibilityServicePendingIntent
            ))

            actions.add(NotificationCompat.Action(
                0,
                ctx.str(R.string.notification_action_dismiss),
                dismissPendingIntent
            ))
        }

        val openAppPendingIntent = IntentUtils.createPendingActivityIntent(ctx, HomeActivity::class.java)

        actions.add(NotificationCompat.Action(0, ctx.str(R.string.notification_action_open_app), openAppPendingIntent))

        showNotification(
            ctx,
            ID_TOGGLE_KEYMAPS,
            CHANNEL_TOGGLE_REMAPS,
            onClickPendingIntent,
            iconRes,
            titleRes,
            textRes,
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            actions = actions.toTypedArray()
        )
    }

    fun showIMEPickerNotification(ctx: Context) {
        val pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_SHOW_IME_PICKER
        )

        showNotification(
            ctx,
            ID_IME_PICKER,
            CHANNEL_IME_PICKER,
            pendingIntent,
            R.drawable.ic_notification_keyboard,
            R.string.notification_ime_persistent_title,
            R.string.notification_ime_persistent_text,
            priority = NotificationCompat.PRIORITY_MIN,
            onGoing = true
        )
    }

    fun showToggleKeyboardNotification(ctx: Context) {
        val pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_TOGGLE_KEYBOARD
        )

        showNotification(
            ctx,
            ID_TOGGLE_KEYBOARD,
            CHANNEL_TOGGLE_KEYBOARD,
            pendingIntent,
            R.drawable.ic_notification_keyboard,
            R.string.notification_toggle_keyboard_title,
            R.string.notification_toggle_keyboard_text,
            showOnLockscreen = true,
            priority = NotificationCompat.PRIORITY_MIN,
            onGoing = true,
            actions = arrayOf(NotificationCompat.Action(0, ctx.str(R.string.toggle), pendingIntent))
        )
    }

    fun dismissNotification(id: Int) = notificationManager.cancel(id)

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

        if (SDK_INT >= Build.VERSION_CODES.O) {
            invalidateChannels(ctx)
        }

        val builder = NotificationCompat.Builder(ctx, channel).apply {
            color = ctx.color(R.color.colorAccent)
            setContentTitle(ctx.str(title))
            setContentText(ctx.str(text))
            setContentIntent(intent)
            setPriority(priority)

            if (onGoing) {
                setOngoing(true)
            }

            //can't use vector drawables for KitKat
            if (SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun invalidateChannels(ctx: Context) {
        notificationManager.deleteNotificationChannel(CHANNEL_ID_WARNINGS)
        notificationManager.deleteNotificationChannel(CHANNEL_ID_PERSISTENT)

        val channels = mutableListOf(
            NotificationChannel(
                CHANNEL_TOGGLE_REMAPS,
                ctx.str(R.string.notification_channel_toggle_remaps),
                NotificationManager.IMPORTANCE_MIN
            ),

            NotificationChannel(
                CHANNEL_KEYBOARD_HIDDEN,
                ctx.str(R.string.notification_channel_keyboard_hidden),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        if (PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            val toggleKeyboardChannel = NotificationChannel(
                CHANNEL_TOGGLE_KEYBOARD,
                ctx.str(R.string.notification_channel_toggle_keyboard),
                NotificationManager.IMPORTANCE_MIN
            )

            channels.add(toggleKeyboardChannel)
        } else {
            notificationManager.deleteNotificationChannel(CHANNEL_TOGGLE_KEYBOARD)
        }

        if ((AppPreferences.hasRootPermission && SDK_INT >= O_MR1 && SDK_INT < Q) || SDK_INT < O_MR1) {

            val imePickerChannel = NotificationChannel(
                CHANNEL_IME_PICKER,
                ctx.str(R.string.notification_channel_ime_picker),
                NotificationManager.IMPORTANCE_MIN
            )

            channels.add(imePickerChannel)
        } else {
            notificationManager.deleteNotificationChannel(CHANNEL_IME_PICKER)
        }

        notificationManager.createNotificationChannels(channels)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openChannelSettings(channelId: String) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, Constants.PACKAGE_NAME)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            appCtx.startActivity(this)
        }
    }
}