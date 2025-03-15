package io.github.sds100.keymapper.mappings.keymaps

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.FloatingButtonKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKey
import io.github.sds100.keymapper.system.inputevents.InputEventUtils

/**
 * Store the data required for determining trigger errors to reduce the number of calls with
 * significant overhead.
 */
data class TriggerErrorSnapshot(
    val isKeyMapperImeChosen: Boolean,
    val isDndAccessGranted: Boolean,
    val isRootGranted: Boolean,
    val isAssistantTriggerPurchased: Boolean,
    val isFloatingButtonsPurchased: Boolean,
    val showDpadImeSetupError: Boolean,
) {
    companion object {
        private val keysThatRequireDndAccess = arrayOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
        )
    }

    fun getTriggerError(keyMap: KeyMap, key: TriggerKey): TriggerError? {
        // can only detect volume button presses during a phone call with an input method service
        if (!isKeyMapperImeChosen && keyMap.requiresImeKeyEventForwardingInPhoneCall(key)) {
            return TriggerError.CANT_DETECT_IN_PHONE_CALL
        }

        val requiresDndAccess = key is KeyCodeTriggerKey && key.keyCode in keysThatRequireDndAccess

        if (requiresDndAccess) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isDndAccessGranted) {
                return TriggerError.DND_ACCESS_DENIED
            }
        }

        if (keyMap.trigger.screenOffTrigger &&
            !isRootGranted &&
            keyMap.trigger.isDetectingWhenScreenOffAllowed()
        ) {
            return TriggerError.SCREEN_OFF_ROOT_DENIED
        }

        if (key is AssistantTriggerKey && !isAssistantTriggerPurchased) {
            return TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED
        }

        val containsDpadKey =
            key is KeyCodeTriggerKey && InputEventUtils.isDpadKeyCode(key.keyCode) && key.detectionSource == KeyEventDetectionSource.INPUT_METHOD

        if (showDpadImeSetupError && !isKeyMapperImeChosen && containsDpadKey) {
            return TriggerError.DPAD_IME_NOT_SELECTED
        }

        if (!isFloatingButtonsPurchased && key is FloatingButtonKey) {
            return TriggerError.FLOATING_BUTTONS_NOT_PURCHASED
        } else if (key is FloatingButtonKey && key.button == null) {
            return TriggerError.FLOATING_BUTTON_DELETED
        }

        // TODO if button deleted go through the process of adding the button again.
        // TODO do not show "button deleted" twice in trigger key list item
        // TODO do not show the settings button on trigger key list item

        return null
    }
}
