package io.github.sds100.keymapper.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.activity.HomeActivity
import io.github.sds100.keymapper.util.AccessibilityUtils
import io.github.sds100.keymapper.util.AppNotification
import io.github.sds100.keymapper.util.DismissNotification
import io.github.sds100.keymapper.util.KeyboardUtils
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 24/03/2019.
 */

class KeyMapperBroadcastReceiver : BroadcastReceiver() {
    companion object {
        /**
         * Only send this action if the app isn't the active window.
         */
        const val ACTION_SHOW_IME_PICKER = "$PACKAGE_NAME.ACTION_SHOW_IME_PICKER"
        const val ACTION_TOGGLE_KEYBOARD = "$PACKAGE_NAME.ACTION_TOGGLE_KEYBOARD"
        const val ACTION_DISMISS_TOGGLE_KEYMAPS_NOTIFICATION =
            "$PACKAGE_NAME.ACTION_DISMISS_NOTIFICATION"

        const val ACTION_ON_FINGERPRINT_FEAT_NOTIFICATION_CLICK =
            "$PACKAGE_NAME.ACTION_ON_FINGERPRINT_FEAT_NOTIFICATION_CLICK"

        const val ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN_NOTIFICATION_CLICK =
            "$PACKAGE_NAME.ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN_NOTIFICATION_CLICK"

        const val ACTION_PAUSE_KEYMAPS = "$PACKAGE_NAME.PAUSE_KEYMAPS"
        const val ACTION_RESUME_KEYMAPS = "$PACKAGE_NAME.RESUME_KEYMAPS"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return

        when (intent.action) {
            ACTION_SHOW_IME_PICKER -> KeyboardUtils.showInputMethodPickerDialogOutsideApp(context)
            ACTION_TOGGLE_KEYBOARD -> KeyboardUtils.toggleCompatibleIme(
                context,
                fromForeground = false
            )

            ACTION_PAUSE_KEYMAPS -> context.globalPreferences.set(Keys.keymapsPaused, true)
            ACTION_RESUME_KEYMAPS -> context.globalPreferences.set(Keys.keymapsPaused, false)

            MyAccessibilityService.ACTION_START_SERVICE -> AccessibilityUtils.enableService(context)

            MyAccessibilityService.ACTION_STOP_SERVICE -> AccessibilityUtils.disableService(context)

            ACTION_DISMISS_TOGGLE_KEYMAPS_NOTIFICATION -> {
                ServiceLocator.notificationController(context)
                    .onEvent(
                        DismissNotification(
                            AppNotification.ToggleKeymaps(AppNotification.ToggleKeymaps.State.ANY)
                        )
                    )
            }

            ACTION_ON_FINGERPRINT_FEAT_NOTIFICATION_CLICK -> {
                runBlocking {
                    ServiceLocator.globalPreferences(context)
                        .set(Keys.approvedFingerprintFeaturePrompt, true)
                }

                goToHomeScreen(context)
            }

            ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN_NOTIFICATION_CLICK -> {
                runBlocking {
                    ServiceLocator.globalPreferences(context)
                        .set(Keys.approvedSetupChosenDevicesAgain, true)
                }

                goToHomeScreen(context)
            }

            else -> context.sendBroadcast(Intent(intent.action))
        }
    }

    private fun goToHomeScreen(ctx: Context) {
        Intent(ctx, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ctx.startActivity(this)
        }
    }
}