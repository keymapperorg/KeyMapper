package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.keymaps.requiresImeKeyEventForwardingInPhoneCall
import io.github.sds100.keymapper.base.purchasing.ProductId
import io.github.sds100.keymapper.base.purchasing.PurchasingError
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils

/**
 * Store the data required for determining trigger errors to reduce the number of calls with
 * significant overhead.
 */
data class TriggerErrorSnapshot(
    val isKeyMapperImeChosen: Boolean,
    val isDndAccessGranted: Boolean,
    val isRootGranted: Boolean,
    val purchases: KMResult<Set<ProductId>>,
    val showDpadImeSetupError: Boolean,
    /**
     * Can be null if the sdk version is not high enough.
     */
    val isSystemBridgeConnected: Boolean?,
    /**
     * Can be null if the sdk version is not high enough.
     */
    val evdevDevices: List<EvdevDeviceInfo>?,
) {
    companion object {
        private val keysThatRequireDndAccess = arrayOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
        )

        fun isScreenOffTriggerMigrationRequired(trigger: Trigger, key: TriggerKey): Boolean =
            trigger.legacyDetectScreenOff &&
                key is KeyEventTriggerKey &&
                key.keyCode in LEGACY_SCREEN_OFF_KEY_CODES

        /**
         * These are the key codes that were detected with the getevent command prior to v4.0.0.
         */
        val LEGACY_SCREEN_OFF_KEY_CODES: Set<Int> = setOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_SEARCH,
        )
    }

    fun getTriggerError(keyMap: KeyMap, key: TriggerKey): TriggerError? {
        if (isScreenOffTriggerMigrationRequired(keyMap.trigger, key)) {
            return TriggerError.MIGRATE_SCREEN_OFF_TRIGGER
        }

        purchases.onSuccess { purchases ->
            if (key is AssistantTriggerKey && !purchases.contains(ProductId.ASSISTANT_TRIGGER)) {
                return TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED
            }

            if (key is FloatingButtonKey && !purchases.contains(ProductId.FLOATING_BUTTONS)) {
                return TriggerError.FLOATING_BUTTONS_NOT_PURCHASED
            }
        }.onFailure { error ->
            if ((key is AssistantTriggerKey || key is FloatingButtonKey) &&
                error == PurchasingError.PurchasingProcessError.NetworkError
            ) {
                return TriggerError.PURCHASE_VERIFICATION_FAILED
            }
        }

        if (key is FloatingButtonKey && key.button == null) {
            return TriggerError.FLOATING_BUTTON_DELETED
        }

        // can only detect volume button presses during a phone call with an input method service
        if (!isKeyMapperImeChosen && keyMap.requiresImeKeyEventForwardingInPhoneCall(key)) {
            return TriggerError.CANT_DETECT_IN_PHONE_CALL
        }

        val requiresDndAccess =
            key is KeyEventTriggerKey && key.keyCode in keysThatRequireDndAccess

        if (requiresDndAccess) {
            if (!isDndAccessGranted) {
                return TriggerError.DND_ACCESS_DENIED
            }
        }

        val containsDpadKey =
            key is KeyEventTriggerKey &&
                KeyEventUtils.isDpadKeyCode(key.keyCode) &&
                key.requiresIme

        if (showDpadImeSetupError && !isKeyMapperImeChosen && containsDpadKey) {
            return TriggerError.DPAD_IME_NOT_SELECTED
        }

        if (key is EvdevTriggerKey) {
            if (isSystemBridgeConnected == null) {
                return TriggerError.SYSTEM_BRIDGE_UNSUPPORTED
            }

            if (!isSystemBridgeConnected) {
                return TriggerError.SYSTEM_BRIDGE_DISCONNECTED
            }

            if (evdevDevices != null && !evdevDevices.contains(key.device)) {
                return TriggerError.EVDEV_DEVICE_NOT_FOUND
            }
        }

        return null
    }
}
