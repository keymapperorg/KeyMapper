package io.github.sds100.keymapper

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.IntDef
import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.AccessibilityUtils
import io.github.sds100.keymapper.util.IntentUtils
import io.github.sds100.keymapper.util.NotificationUtils
import io.github.sds100.keymapper.util.PermissionUtils

/**
 * Created by sds100 on 24/03/2019.
 */

/**
 * Tells notifications what to display
 */
object WidgetsManager {
    @IntDef(value = [
        EVENT_PAUSE_REMAPS,
        EVENT_RESUME_REMAPS,
        EVENT_ACCESSIBILITY_SERVICE_STARTED,
        EVENT_ACCESSIBILITY_SERVICE_STOPPED,
        EVENT_HIDE_KEYBOARD,
        EVENT_SHOW_KEYBOARD])
    annotation class Event

    const val EVENT_PAUSE_REMAPS = 0
    const val EVENT_RESUME_REMAPS = 1
    const val EVENT_ACCESSIBILITY_SERVICE_STARTED = 2
    const val EVENT_ACCESSIBILITY_SERVICE_STOPPED = 3
    const val EVENT_HIDE_KEYBOARD = 4
    const val EVENT_SHOW_KEYBOARD = 5

    fun onEvent(ctx: Context, @Event event: Int) {
        when (event) {
            EVENT_SHOW_KEYBOARD, EVENT_ACCESSIBILITY_SERVICE_STOPPED -> {
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

        NotificationUtils.updateToggleKeymapsNotification(ctx, event)

        if (event == EVENT_ACCESSIBILITY_SERVICE_STARTED) {
            if (AppPreferences.keymapsPaused) {
                onEvent(ctx, EVENT_PAUSE_REMAPS)
            } else {
                onEvent(ctx, EVENT_RESUME_REMAPS)
            }
        }
    }

    fun invalidateNotifications(ctx: Context) {
        if (AccessibilityUtils.isServiceEnabled(ctx)) {
            if (AppPreferences.keymapsPaused) {
                onEvent(ctx, EVENT_PAUSE_REMAPS)
            } else {
                onEvent(ctx, EVENT_RESUME_REMAPS)
            }
        } else {
            onEvent(ctx, EVENT_ACCESSIBILITY_SERVICE_STOPPED)
        }

        if (SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.invalidateChannels(ctx)
        }

        //visibility of the notification is handled by the system on API >= 26 but is only supported up to API 28
        if (AppPreferences.showImePickerNotification ||
            (SDK_INT >= Build.VERSION_CODES.O && SDK_INT < Build.VERSION_CODES.Q)) {

            NotificationUtils.showIMEPickerNotification(ctx)
        } else if (SDK_INT < Build.VERSION_CODES.O) {
            NotificationUtils.dismissNotification(NotificationUtils.ID_IME_PICKER)
        }

        if (PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)
            || AppPreferences.showToggleKeyboardNotification) {
            NotificationUtils.showToggleKeyboardNotification(ctx)

        } else {
            NotificationUtils.dismissNotification(NotificationUtils.ID_TOGGLE_KEYBOARD)
        }
    }
}