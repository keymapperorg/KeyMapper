package io.github.sds100.keymapper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.activity.HomeActivity
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.*
import org.jetbrains.anko.defaultSharedPreferences

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
                NotificationUtils.dismissNotification(ctx, NotificationUtils.ID_KEYBOARD_HIDDEN)
            }

            EVENT_HIDE_KEYBOARD -> {
                val intent = IntentUtils.createPendingBroadcastIntent(
                        ctx,
                        MyAccessibilityService.ACTION_SHOW_KEYBOARD
                )

                ctx.notification(
                        id = NotificationUtils.ID_KEYBOARD_HIDDEN,
                        icon = R.drawable.ic_notification_keyboard_hide,
                        title = R.string.notification_keyboard_hidden_title,
                        text = R.string.notification_keyboard_hidden_text,
                        intent = intent,
                        onGoing = true,
                        priority = NotificationCompat.PRIORITY_LOW,
                        channel = NotificationUtils.CHANNEL_ID_WARNINGS)

                return
            }
        }

        updateToggleRemappingsNotification(ctx, event)
    }

    fun invalidateNotification(ctx: Context) {
        if (AccessibilityUtils.isServiceEnabled(ctx)) {
            ctx.sendBroadcast(Intent(MyAccessibilityService.ACTION_UPDATE_NOTIFICATION))
        } else {
            onEvent(ctx, EVENT_SERVICE_STOPPED)
        }
    }


    private fun updateToggleRemappingsNotification(ctx: Context, @Event event: Int) {
        val showNotification = ctx.defaultSharedPreferences.getBoolean(
                ctx.str(R.string.key_pref_show_toggle_remappings_notification),
                ctx.bool(R.bool.default_value_show_toggle_remappings_notification))

        if (!showNotification) {
            NotificationUtils.dismissNotification(ctx, NotificationUtils.ID_TOGGLE_REMAPPING_PERSISTENT)
            return
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
                iconRes = R.drawable.ic_notification_play_arrow

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
                    ctx.str(R.string.notification_action_stop_acc_service),
                    actionPendingIntent))
        }

        val openAppPendingIntent = IntentUtils.createPendingActivityIntent(ctx, HomeActivity::class.java)

        actions.add(NotificationCompat.Action(0, ctx.str(R.string.notification_action_open_app), openAppPendingIntent))

        ctx.notification(
                NotificationUtils.ID_TOGGLE_REMAPPING_PERSISTENT,
                NotificationUtils.CHANNEL_ID_PERSISTENT,
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