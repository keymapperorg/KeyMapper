package io.github.sds100.keymapper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.activity.HomeActivity
import io.github.sds100.keymapper.util.AccessibilityUtils
import io.github.sds100.keymapper.util.IntentUtils
import io.github.sds100.keymapper.util.NotificationUtils
import splitties.resources.appStr

/**
 * Created by sds100 on 24/03/2019.
 */

/**
 * Tells widgets (notifications, quick settings) what to display
 */
object WidgetsManager {
    @IntDef(value = [
        EVENT_PAUSE_REMAPS,
        EVENT_RESUME_REMAPS,
        EVENT_SERVICE_START,
        EVENT_SERVICE_STOPPED])
    annotation class Event

    const val EVENT_PAUSE_REMAPS = 0
    const val EVENT_RESUME_REMAPS = 1
    const val EVENT_SERVICE_START = 2
    const val EVENT_SERVICE_STOPPED = 3
    const val EVENT_HIDE_KEYBOARD = 4
    const val EVENT_SHOW_KEYBOARD = 5

    fun onEvent(ctx: Context, @Event event: Int) {
        when (event) {
            EVENT_SHOW_KEYBOARD, EVENT_SERVICE_STOPPED -> {
                NotificationUtils.dismissNotification(NotificationUtils.ID_KEYBOARD_HIDDEN)
            }

            EVENT_HIDE_KEYBOARD -> {
                val intent = IntentUtils.createPendingBroadcastIntent(
                    ctx,
                    MyAccessibilityService.ACTION_SHOW_KEYBOARD
                )

                NotificationUtils.showNotification(
                    ctx,
                    id = NotificationUtils.ID_KEYBOARD_HIDDEN,
                    icon = R.drawable.ic_notification_keyboard_hide,
                    title = R.string.notification_keyboard_hidden_title,
                    text = R.string.notification_keyboard_hidden_text,
                    intent = intent,
                    onGoing = true,
                    priority = NotificationCompat.PRIORITY_LOW,
                    channel = NotificationUtils.CHANNEL_KEYBOARD_HIDDEN)

                return
            }
        }

        updateToggleRemappingsNotification(ctx, event)
    }

    fun invalidateNotifications(ctx: Context) {
        if (AccessibilityUtils.isServiceEnabled(ctx)) {
            ctx.sendBroadcast(Intent(MyAccessibilityService.ACTION_UPDATE_NOTIFICATION))
        } else {
            onEvent(ctx, EVENT_SERVICE_STOPPED)
        }

        if (SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.invalidateChannels()
        }

        //visibility of the notification is handled by the system on API >= 26 but is only supported up to API 28
        if (AppPreferences.showImePickerNotification ||
            (SDK_INT >= Build.VERSION_CODES.O && SDK_INT < Build.VERSION_CODES.Q)) {

            NotificationUtils.showIMEPickerNotification(ctx)
        } else if (SDK_INT < Build.VERSION_CODES.O) {
            NotificationUtils.dismissNotification(NotificationUtils.ID_IME_PICKER)
        }
    }

    private fun updateToggleRemappingsNotification(ctx: Context, @Event event: Int) {
        if (SDK_INT < Build.VERSION_CODES.O) {
            val showNotification = AppPreferences.showToggleRemapsNotification

            if (!showNotification) {
                NotificationUtils.dismissNotification(NotificationUtils.ID_TOGGLE_REMAPS)
                return
            }
        }

        val onClickPendingIntent: PendingIntent
        val actions = mutableListOf<NotificationCompat.Action>()

        @StringRes val titleRes: Int
        @StringRes val textRes: Int
        @DrawableRes val iconRes: Int

        when (event) {
            EVENT_PAUSE_REMAPS -> {
                titleRes = R.string.notification_remappings_start_title
                textRes = R.string.notification_remappings_start_text
                iconRes = R.drawable.ic_notification_play

                onClickPendingIntent = IntentUtils.createPendingBroadcastIntent(
                    ctx,
                    MyAccessibilityService.ACTION_RESUME_REMAPPINGS
                )
            }

            EVENT_RESUME_REMAPS, EVENT_SERVICE_START -> {
                titleRes = R.string.notification_remappings_pause_title
                textRes = R.string.notification_remappings_pause_text
                iconRes = R.drawable.ic_notification_pause

                onClickPendingIntent = IntentUtils.createPendingBroadcastIntent(
                    ctx,
                    MyAccessibilityService.ACTION_PAUSE_REMAPPINGS
                )
            }

            EVENT_SERVICE_STOPPED -> {
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

        if ((event == EVENT_RESUME_REMAPS)
            or (event == EVENT_SERVICE_START)
            or (event == EVENT_PAUSE_REMAPS)) {

            val actionPendingIntent = IntentUtils.createPendingBroadcastIntent(
                ctx,
                MyAccessibilityService.ACTION_STOP
            )

            actions.add(NotificationCompat.Action(
                0,
                appStr(R.string.notification_action_stop_acc_service),
                actionPendingIntent))
        }

        val openAppPendingIntent = IntentUtils.createPendingActivityIntent(ctx, HomeActivity::class.java)

        actions.add(NotificationCompat.Action(0, appStr(R.string.notification_action_open_app), openAppPendingIntent))

        NotificationUtils.showNotification(
            ctx,
            NotificationUtils.ID_TOGGLE_REMAPS,
            NotificationUtils.CHANNEL_TOGGLE_REMAPS,
            onClickPendingIntent,
            iconRes,
            titleRes,
            textRes,
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            actions = *actions.toTypedArray()
        )
    }
}