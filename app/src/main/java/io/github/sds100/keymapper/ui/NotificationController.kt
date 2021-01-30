package io.github.sds100.keymapper

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import io.github.sds100.keymapper.data.PreferenceKeys
import io.github.sds100.keymapper.data.showImePickerNotification
import io.github.sds100.keymapper.util.*

/**
 * Created by sds100 on 24/03/2019.
 */

class NotificationController {
    fun onEvent(ctx: Context, event: UpdateNotificationEvent) {
        //dismiss the notification if turned off in the settings
        var showToggleKeymapsNotification = false

        if (SDK_INT < Build.VERSION_CODES.O) {
            showToggleKeymapsNotification =
                ctx.globalPreferences.getFlow(PreferenceKeys.showToggleKeymapsNotification)
                    .firstBlocking()
                    ?: false

            if (!showToggleKeymapsNotification) {
                NotificationUtils.dismissNotification(NotificationUtils.ID_TOGGLE_KEYMAPS)
            }
        }

        when (event) {
            is AccessibilityServiceStarted -> {

            }

            is AccessibilityServiceStopped -> {

            }
        }
    }

//    fun onEvent(ctx: Context, event: Event) {
//        when (event) {
//            EVENT_SHOW_KEYBOARD, EVENT_ACCESSIBILITY_SERVICE_STOPPED -> {
//                NotificationUtils.dismissNotification(NotificationUtils.ID_KEYBOARD_HIDDEN)
//            }
//
//            EVENT_HIDE_KEYBOARD -> {
//                val intent = IntentUtils.createPendingBroadcastIntent(
//                    ctx,
//                    MyAccessibilityService.ACTION_SHOW_KEYBOARD
//                )
//
//                NotificationUtils.showNotification(
//                    ctx,
//                    id = NotificationUtils.ID_KEYBOARD_HIDDEN,
//                    icon = R.drawable.ic_notification_keyboard_hide,
//                    title = R.string.notification_keyboard_hidden_title,
//                    text = R.string.notification_keyboard_hidden_text,
//                    intent = intent,
//                    onGoing = true,
//                    priority = NotificationCompat.PRIORITY_LOW,
//                    channel = NotificationUtils.CHANNEL_KEYBOARD_HIDDEN)
//
//                return
//            }
//        }
//
//        NotificationUtils.updateToggleKeymapsNotification(ctx, event)

//     TODO   if (event == EVENT_ACCESSIBILITY_SERVICE_STARTED) {
//            if (AppPreferences.keymapsPaused) {
//                onEvent(ctx, EVENT_PAUSE_REMAPS)
//            } else {
//                onEvent(ctx, EVENT_RESUME_REMAPS)
//            }
//        }
//    }

    fun invalidateNotifications(ctx: Context) {
//   TODO     if (AccessibilityUtils.isServiceEnabled(ctx)) {
//            if (AppPreferences.keymapsPaused) {
//                onEvent(ctx, EVENT_PAUSE_REMAPS)
//            } else {
//                onEvent(ctx, EVENT_RESUME_REMAPS)
//            }
//        } else {
//            onEvent(ctx, EVENT_ACCESSIBILITY_SERVICE_STOPPED)
//        }

        if (SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.invalidateChannels(ctx)
        }

        //visibility of the notification is handled by the system on API >= 26 but is only supported up to API 28
        if (ctx.globalPreferences.showImePickerNotification.firstBlocking() ||
            (SDK_INT >= Build.VERSION_CODES.O && SDK_INT < Build.VERSION_CODES.Q)) {

            NotificationUtils.showIMEPickerNotification(ctx)
        } else if (SDK_INT < Build.VERSION_CODES.O) {
            NotificationUtils.dismissNotification(NotificationUtils.ID_IME_PICKER)
        }

        val showToggleKeyboardNotification =
            ctx.globalPreferences
                .getFlow(Keys.showToggleKeyboardNotification).firstBlocking()
                ?: false

        if (PermissionUtils.isPermissionGranted(ctx, Manifest.permission.WRITE_SECURE_SETTINGS)
            || showToggleKeyboardNotification) {
            NotificationUtils.showToggleKeyboardNotification(ctx)

        } else {
            NotificationUtils.dismissNotification(NotificationUtils.ID_TOGGLE_KEYBOARD)
        }
    }
}