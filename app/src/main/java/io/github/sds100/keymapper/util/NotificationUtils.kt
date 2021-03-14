package io.github.sds100.keymapper.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import io.github.sds100.keymapper.broadcastreceiver.KeyMapperBroadcastReceiver
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.activity.HomeActivity
import io.github.sds100.keymapper.util.AppNotification.*

/**
 * Created by sds100 on 30/09/2018.
 */

object NotificationUtils {
    private const val ID_IME_PICKER = 123
    private const val ID_KEYBOARD_HIDDEN = 747
    private const val ID_TOGGLE_KEYMAPS = 231
    private const val ID_TOGGLE_KEYBOARD = 143
    private const val ID_FEATURE_REMAP_FINGERPRINT_GESTURES = 1
    private const val ID_SETUP_CHOSEN_DEVICES_AGAIN = 2

    const val CHANNEL_TOGGLE_KEYMAPS = "channel_toggle_remaps"
    const val CHANNEL_IME_PICKER = "channel_ime_picker"
    const val CHANNEL_KEYBOARD_HIDDEN = "channel_warning_keyboard_hidden"
    const val CHANNEL_TOGGLE_KEYBOARD = "channel_toggle_keymapper_keyboard"
    const val CHANNEL_NEW_FEATURES = "channel_new_features"

    @Deprecated("Removed in 2.0. This channel shouldn't exist")
    const val CHANNEL_ID_WARNINGS = "channel_warnings"

    @Deprecated("Removed in 2.0. This channel shouldn't exist")
    const val CHANNEL_ID_PERSISTENT = "channel_persistent"

    @RequiresApi(Build.VERSION_CODES.O)
    fun openChannelSettings(ctx: Context, channelId: String) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, Constants.PACKAGE_NAME)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            ctx.startActivity(this)
        }
    }

    fun showNotification(ctx: Context, notification: AppNotification) {
        when (notification) {
            is ToggleKeymaps -> when (notification.state) {
                ToggleKeymaps.State.KEYMAPS_PAUSED -> keymapsPausedNotification(ctx)
                ToggleKeymaps.State.KEYMAPS_RESUMED -> keymapsResumedNotification(ctx)
                ToggleKeymaps.State.SERVICE_DISABLED -> accessibilityServiceDisabledNotification(ctx)
            }
            FingerprintFeature -> fingerprintFeatureNotification(ctx)
            KeyboardHidden -> keyboardHiddenNotification(ctx)

            SetupChosenDevicesAgain -> setupChosenDevicesAgainNotification(ctx)
            ShowImePicker -> showImePickerNotification(ctx)
            ToggleKeyboard -> toggleKeyboardNotification(ctx)
        }
    }

    fun dismissNotification(ctx: Context, notification: AppNotification) {
        val id = when (notification) {
            FingerprintFeature -> ID_FEATURE_REMAP_FINGERPRINT_GESTURES
            KeyboardHidden -> ID_KEYBOARD_HIDDEN
            SetupChosenDevicesAgain -> ID_SETUP_CHOSEN_DEVICES_AGAIN
            ShowImePicker -> ID_IME_PICKER
            ToggleKeyboard -> ID_TOGGLE_KEYBOARD
            is ToggleKeymaps -> ID_TOGGLE_KEYMAPS
        }

        NotificationManagerCompat.from(ctx).cancel(id)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(ctx: Context, vararg channelId: String) {
        channelId.forEach { createChannel(ctx, it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(ctx: Context, channelId: String) {
        val channel = when (channelId) {
            CHANNEL_TOGGLE_KEYMAPS -> NotificationChannel(
                CHANNEL_TOGGLE_KEYMAPS,
                ctx.str(R.string.notification_channel_toggle_remaps),
                NotificationManager.IMPORTANCE_MIN
            )

            CHANNEL_KEYBOARD_HIDDEN -> NotificationChannel(
                CHANNEL_KEYBOARD_HIDDEN,
                ctx.str(R.string.notification_channel_keyboard_hidden),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            CHANNEL_NEW_FEATURES -> NotificationChannel(
                CHANNEL_NEW_FEATURES,
                ctx.str(R.string.notification_channel_new_features),
                NotificationManager.IMPORTANCE_LOW
            )

            CHANNEL_TOGGLE_KEYBOARD -> NotificationChannel(
                CHANNEL_TOGGLE_KEYBOARD,
                ctx.str(R.string.notification_channel_toggle_keyboard),
                NotificationManager.IMPORTANCE_MIN
            )

            CHANNEL_IME_PICKER -> NotificationChannel(
                CHANNEL_IME_PICKER,
                ctx.str(R.string.notification_channel_ime_picker),
                NotificationManager.IMPORTANCE_MIN
            )

            else -> throw Exception("don't know how to create this channel $channelId")
        }

        NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
    }

    fun deleteChannel(ctx: Context, channelId: String) {
        NotificationManagerCompat.from(ctx).deleteNotificationChannel(channelId)
    }

    private fun showNotification(
        ctx: Context,
        id: Int,
        pendingIntent: PendingIntent? = null,
        channel: String,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes text: Int,
        showOnLockScreen: Boolean = false,
        onGoing: Boolean = false,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        vararg actions: NotificationCompat.Action = arrayOf(),
        autoCancel: Boolean = false,
        bigTextStyle: Boolean = false
    ) {

        val builder = NotificationCompat.Builder(ctx, channel).apply {
            color = ctx.color(R.color.colorAccent)
            setContentTitle(ctx.str(title))
            setContentText(ctx.str(text))
            setContentIntent(pendingIntent)
            setAutoCancel(autoCancel)
            this.priority = priority

            if (bigTextStyle) {
                setStyle(NotificationCompat.BigTextStyle())
            }

            if (onGoing) {
                setOngoing(true)
            }

            //can't use vector drawables for KitKat or older
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val bitmap = VectorDrawableCompat.create(ctx.resources, icon, ctx.theme)?.toBitmap()
                setLargeIcon(bitmap)
                setSmallIcon(R.mipmap.ic_launcher)
            } else {
                setSmallIcon(icon)
            }

            if (!showOnLockScreen) setVisibility(NotificationCompat.VISIBILITY_SECRET) //hide on lockscreen

            actions.forEach {
                addAction(it)
            }
        }

        val notification = builder.build()

        //show the notification
        with(NotificationManagerCompat.from(ctx)) {
            notify(id, notification)
        }
    }

    private fun keymapsPausedNotification(ctx: Context) = showNotification(
        ctx,
        id = ID_TOGGLE_KEYMAPS,
        channel = CHANNEL_TOGGLE_KEYMAPS,
        title = R.string.notification_keymaps_paused_title,
        text = R.string.notification_keymaps_paused_text,
        icon = R.drawable.ic_notification_play,
        pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_RESUME_KEYMAPS
        ),
        showOnLockScreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = arrayOf(
            stopAccessibilityServiceAction(ctx),
            dismissToggleKeymapsNotificationAction(ctx),
            openKeyMapperAction(ctx)
        ),
    )

    private fun keymapsResumedNotification(ctx: Context) = showNotification(
        ctx,
        id = ID_TOGGLE_KEYMAPS,
        channel = CHANNEL_TOGGLE_KEYMAPS,
        title = R.string.notification_keymaps_resumed_title,
        text = R.string.notification_keymaps_resumed_text,
        icon = R.drawable.ic_notification_pause,
        pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_PAUSE_KEYMAPS
        ),
        showOnLockScreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = arrayOf(
            stopAccessibilityServiceAction(ctx),
            dismissToggleKeymapsNotificationAction(ctx),
            openKeyMapperAction(ctx)
        )
    )

    private fun accessibilityServiceDisabledNotification(ctx: Context) = showNotification(
        ctx,
        id = ID_TOGGLE_KEYMAPS,
        channel = CHANNEL_TOGGLE_KEYMAPS,
        title = R.string.notification_accessibility_service_disabled_title,
        text = R.string.notification_accessibility_service_disabled_text,
        icon = R.drawable.ic_notification_error,
        pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            MyAccessibilityService.ACTION_START_SERVICE
        ),
        showOnLockScreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = arrayOf(
            dismissToggleKeymapsNotificationAction(ctx),
            openKeyMapperAction(ctx)
        )
    )

    private fun toggleKeyboardNotification(ctx: Context) = showNotification(
        ctx,
        id = ID_TOGGLE_KEYBOARD,
        channel = CHANNEL_TOGGLE_KEYBOARD,
        title = R.string.notification_toggle_keyboard_title,
        text = R.string.notification_toggle_keyboard_text,
        icon = R.drawable.ic_notification_keyboard,
        pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_TOGGLE_KEYBOARD
        ),
        showOnLockScreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = arrayOf(toggleKeyboardAction(ctx))
    )

    private fun showImePickerNotification(ctx: Context) = showNotification(
        ctx,
        id = ID_IME_PICKER,
        channel = CHANNEL_IME_PICKER,
        title = R.string.notification_ime_persistent_title,
        text = R.string.notification_ime_persistent_text,
        icon = R.drawable.ic_notification_keyboard,
        pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_SHOW_IME_PICKER
        ),
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN
    )

    private fun keyboardHiddenNotification(ctx: Context) = showNotification(
        ctx,
        id = ID_KEYBOARD_HIDDEN,
        channel = CHANNEL_KEYBOARD_HIDDEN,
        title = R.string.notification_keyboard_hidden_title,
        text = R.string.notification_keyboard_hidden_text,
        icon = R.drawable.ic_notification_keyboard_hide,
        pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            MyAccessibilityService.ACTION_SHOW_KEYBOARD
        ),
        onGoing = true,
        priority = NotificationCompat.PRIORITY_LOW
    )

    private fun fingerprintFeatureNotification(ctx: Context) = showNotification(
        ctx,
        id = ID_FEATURE_REMAP_FINGERPRINT_GESTURES,
        channel = CHANNEL_NEW_FEATURES,
        title = R.string.notification_feature_fingerprint_title,
        text = R.string.notification_feature_fingerprint_text,
        icon = R.drawable.ic_notification_fingerprint,
        pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_ON_FINGERPRINT_FEAT_NOTIFICATION_CLICK
        ),
        autoCancel = true,
        priority = NotificationCompat.PRIORITY_LOW,
        bigTextStyle = true
    )

    private fun setupChosenDevicesAgainNotification(ctx: Context) = showNotification(
        ctx,
        id = ID_SETUP_CHOSEN_DEVICES_AGAIN,
        channel = CHANNEL_NEW_FEATURES,
        title = R.string.notification_setup_chosen_devices_again_title,
        text = R.string.notification_setup_chosen_devices_again_text,
        icon = R.drawable.ic_notifications_settings,
        pendingIntent = IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN_NOTIFICATION_CLICK
        ),
        autoCancel = true,
        priority = NotificationCompat.PRIORITY_LOW,
        bigTextStyle = true
    )

    private fun dismissToggleKeymapsNotificationAction(ctx: Context) = NotificationCompat.Action(
        0,
        ctx.str(R.string.notification_action_dismiss),
        IntentUtils.createPendingBroadcastIntent(
            ctx,
            action = KeyMapperBroadcastReceiver.ACTION_DISMISS_TOGGLE_KEYMAPS_NOTIFICATION
        )
    )

    private fun stopAccessibilityServiceAction(ctx: Context) = NotificationCompat.Action(
        0,
        ctx.str(R.string.notification_action_stop_acc_service),
        IntentUtils.createPendingBroadcastIntent(ctx, MyAccessibilityService.ACTION_STOP_SERVICE)
    )

    private fun openKeyMapperAction(ctx: Context) = NotificationCompat.Action(
        0,
        ctx.str(R.string.notification_action_open_app),
        IntentUtils.createPendingActivityIntent(ctx, HomeActivity::class.java)
    )

    private fun toggleKeyboardAction(ctx: Context) = NotificationCompat.Action(
        0,
        ctx.str(R.string.toggle),
        IntentUtils.createPendingBroadcastIntent(
            ctx,
            KeyMapperBroadcastReceiver.ACTION_TOGGLE_KEYBOARD
        )
    )
}

sealed class AppNotification {
    data class ToggleKeymaps(val state: State) : AppNotification() {
        enum class State {
            ANY,
            KEYMAPS_RESUMED,
            KEYMAPS_PAUSED,
            SERVICE_DISABLED
        }
    }

    object ToggleKeyboard : AppNotification()
    object ShowImePicker : AppNotification()
    object KeyboardHidden : AppNotification()
    object FingerprintFeature : AppNotification()
    object SetupChosenDevicesAgain : AppNotification()
}