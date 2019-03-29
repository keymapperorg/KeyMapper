package io.github.sds100.keymapper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.activity.HomeActivity
import io.github.sds100.keymapper.broadcastreceiver.KeyMapperBroadcastReceiver
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.IntentUtils
import io.github.sds100.keymapper.util.NotificationUtils
import io.github.sds100.keymapper.util.bool
import io.github.sds100.keymapper.util.str
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 24/03/2019.
 */

/**
 * Tells widgets (notifications, quick settings) what to display depending on the state of the AccessibilityService.
 */
object AccessibilityServiceWidgetsManager {
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

    fun onEvent(ctx: Context, @Event event: Int) {
        if (!showNotification(ctx)) {
            NotificationUtils.hideNotification(ctx, NotificationUtils.ID_TOGGLE_REMAPPING_PERSISTENT)
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
                        KeyMapperBroadcastReceiver::class.java,
                        MyAccessibilityService.ACTION_RESUME_REMAPPINGS
                )
            }

            EVENT_RESUME_REMAPS, EVENT_SERVICE_START -> {
                titleRes = R.string.notification_remappings_pause_title
                textRes = R.string.notification_remappings_pause_text
                iconRes = R.drawable.ic_notification_pause

                onClickPendingIntent = IntentUtils.createPendingBroadcastIntent(
                        ctx,
                        KeyMapperBroadcastReceiver::class.java,
                        MyAccessibilityService.ACTION_PAUSE_REMAPPINGS
                )
            }

            EVENT_SERVICE_STOPPED -> {
                titleRes = R.string.notification_accessibility_service_disabled_title
                textRes = R.string.notification_accessibility_service_disabled_text
                iconRes = R.drawable.ic_notification_error

                onClickPendingIntent = IntentUtils.createPendingBroadcastIntent(
                        ctx,
                        KeyMapperBroadcastReceiver::class.java,
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
                    KeyMapperBroadcastReceiver::class.java,
                    MyAccessibilityService.ACTION_STOP
            )

            actions.add(NotificationCompat.Action(
                    0,
                    ctx.str(R.string.notification_action_stop_acc_service),
                    actionPendingIntent))
        }

        val openAppPendingIntent = IntentUtils.createPendingActivityIntent(ctx, HomeActivity::class.java)

        actions.add(NotificationCompat.Action(0, ctx.str(R.string.notification_action_open_app), openAppPendingIntent))

        NotificationUtils.showPersistentNotification(
                ctx,
                NotificationUtils.ID_TOGGLE_REMAPPING_PERSISTENT,
                onClickPendingIntent,
                iconRes,
                titleRes,
                textRes,
                showOnLockscreen = true,
                actions = *actions.toTypedArray()
        )
    }

    fun invalidateNotification(ctx: Context) {
        if (MyAccessibilityService.isServiceEnabled(ctx)) {
            ctx.sendBroadcast(Intent(MyAccessibilityService.ACTION_UPDATE_NOTIFICATION))
        } else {
            onEvent(ctx, EVENT_SERVICE_STOPPED)
        }
    }

    private fun showNotification(ctx: Context) = ctx.defaultSharedPreferences.getBoolean(
            ctx.str(R.string.key_pref_show_toggle_remappings_notification),
            ctx.bool(R.bool.default_value_show_toggle_remappings_notification))
}