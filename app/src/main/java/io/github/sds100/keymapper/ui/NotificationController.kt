package io.github.sds100.keymapper

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.data.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.NotificationUtils.CHANNEL_ID_PERSISTENT
import io.github.sds100.keymapper.util.NotificationUtils.CHANNEL_ID_WARNINGS
import io.github.sds100.keymapper.util.NotificationUtils.CHANNEL_IME_PICKER
import io.github.sds100.keymapper.util.NotificationUtils.CHANNEL_KEYBOARD_HIDDEN
import io.github.sds100.keymapper.util.NotificationUtils.CHANNEL_NEW_FEATURES
import io.github.sds100.keymapper.util.NotificationUtils.CHANNEL_TOGGLE_KEYBOARD
import io.github.sds100.keymapper.util.NotificationUtils.CHANNEL_TOGGLE_KEYMAPS
import io.github.sds100.keymapper.util.NotificationUtils.ID_IME_PICKER
import io.github.sds100.keymapper.util.NotificationUtils.ID_KEYBOARD_HIDDEN
import io.github.sds100.keymapper.util.NotificationUtils.ID_TOGGLE_KEYBOARD
import io.github.sds100.keymapper.util.NotificationUtils.ID_TOGGLE_KEYMAPS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 24/03/2019.
 */

class NotificationController(
    coroutineScope: CoroutineScope,
    private val manager: INotificationManagerWrapper,
    private val globalPreferences: IGlobalPreferences,
    iNotificationController: INotificationController
) : INotificationController by iNotificationController {

    init {
        coroutineScope.launch {
            combine(
                globalPreferences.showImePickerNotification,
                globalPreferences.showToggleKeyboardNotification,
                globalPreferences.showToggleKeymapsNotification,
                globalPreferences.keymapsPaused) { _, _, _, _ ->

                invalidateNotifications()
            }.collect()
        }
    }

    fun onEvent(event: UpdateNotificationEvent) {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            invalidateChannels()
        }

        when (event) {
            is OnBootEvent -> invalidateNotifications()

            is OnAccessibilityServiceStarted -> {
                val keymapsPaused = globalPreferences.keymapsPaused.firstBlocking()

                invalidateToggleKeymapsNotification(keymapsPaused)
            }

            is OnAccessibilityServiceStopped -> {
                manager.showNotification(AppNotification.ACCESSIBILITY_SERVICE_DISABLED)
                manager.dismissNotification(ID_KEYBOARD_HIDDEN)
            }

            is OnHideKeyboard ->
                manager.showNotification(AppNotification.KEYBOARD_HIDDEN)

            is OnShowKeyboard ->
                manager.dismissNotification(ID_KEYBOARD_HIDDEN)

            is ShowFingerprintFeatureNotification ->
                manager.showNotification(AppNotification.FINGERPRINT_FEATURE)

            is DismissFingerprintFeatureNotification ->
                manager.dismissNotification(NotificationUtils.ID_FEATURE_REMAP_FINGERPRINT_GESTURES)

            is DismissNotification -> manager.dismissNotification(event.id)
        }
    }

    fun invalidateNotifications() {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            invalidateChannels()
        }

        if (isAccessibilityServiceEnabled()) {
            val keymapsPaused = globalPreferences.keymapsPaused.firstBlocking()
            invalidateToggleKeymapsNotification(keymapsPaused)

        } else {
            manager.showNotification(AppNotification.ACCESSIBILITY_SERVICE_DISABLED)
        }

        //visibility of the notification is handled by the system on API >= 26 but is only supported up to API 28
        if (globalPreferences.showImePickerNotification.firstBlocking() ||
            (SDK_INT >= Build.VERSION_CODES.O && SDK_INT < Build.VERSION_CODES.Q)) {

            manager.showNotification(AppNotification.SHOW_IME_PICKER)
        } else if (SDK_INT < Build.VERSION_CODES.O) {
            manager.dismissNotification(ID_IME_PICKER)
        }

        val showToggleKeyboardNotification =
            globalPreferences
                .getFlow(Keys.showToggleKeyboardNotification).firstBlocking()
                ?: false

        if (haveWriteSecureSettingsPermission() || showToggleKeyboardNotification) {
            manager.showNotification(AppNotification.TOGGLE_KEYBOARD)

        } else {
            manager.dismissNotification(ID_TOGGLE_KEYBOARD)
        }
    }

    private fun invalidateToggleKeymapsNotification(keymapsPaused: Boolean) {
        if (SDK_INT < Build.VERSION_CODES.O) {
            val showNotification = globalPreferences
                .getFlow(Keys.showToggleKeymapsNotification)
                .firstBlocking() ?: false

            if (!showNotification) {
                manager.dismissNotification(ID_TOGGLE_KEYMAPS)
                return
            }
        }

        if (keymapsPaused) {
            manager.showNotification(AppNotification.KEYMAPS_PAUSED)
        } else {
            manager.showNotification(AppNotification.KEYMAPS_RESUMED)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun invalidateChannels() {
        manager.deleteChannel(CHANNEL_ID_WARNINGS)
        manager.deleteChannel(CHANNEL_ID_PERSISTENT)

        val channels = mutableListOf(
            CHANNEL_TOGGLE_KEYMAPS,
            CHANNEL_KEYBOARD_HIDDEN,
            CHANNEL_NEW_FEATURES
        )

        if (haveWriteSecureSettingsPermission()) {
            channels.add(CHANNEL_TOGGLE_KEYBOARD)
        } else {
            manager.deleteChannel(CHANNEL_TOGGLE_KEYBOARD)
        }

        if ((globalPreferences.hasRootPermission.firstBlocking()
                && SDK_INT >= Build.VERSION_CODES.O_MR1 && SDK_INT < Build.VERSION_CODES.Q)
            || SDK_INT < Build.VERSION_CODES.O_MR1) {

            channels.add(CHANNEL_IME_PICKER)
        } else {
            manager.deleteChannel(CHANNEL_IME_PICKER)
        }

        manager.createChannel(*channels.toTypedArray())
    }
}

interface INotificationController {
    fun isAccessibilityServiceEnabled(): Boolean
    fun haveWriteSecureSettingsPermission(): Boolean
}