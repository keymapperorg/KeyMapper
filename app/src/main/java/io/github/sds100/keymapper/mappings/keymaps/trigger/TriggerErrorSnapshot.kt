package io.github.sds100.keymapper.mappings.keymaps.trigger

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.requiresImeKeyEventForwardingInPhoneCall
import io.github.sds100.keymapper.purchasing.ProductId
import io.github.sds100.keymapper.system.inputevents.InputEventUtils

/**
 * Store the data required for determining trigger errors to reduce the number of calls with
 * significant overhead.
 */
data class TriggerErrorSnapshot(
    val isKeyMapperImeChosen: Boolean,
    val isDndAccessGranted: Boolean,
    val isRootGranted: Boolean,
    val purchases: Set<ProductId>,
    val showDpadImeSetupError: Boolean,
) {
    companion object {
        private val keysThatRequireDndAccess = arrayOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
        )
    }

    fun getTriggerError(keyMap: KeyMap, key: TriggerKey): TriggerError? {
        if (key is AssistantTriggerKey && !purchases.contains(ProductId.ASSISTANT_TRIGGER)) {
            return TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED
        }

        if (key is FloatingButtonKey && !purchases.contains(ProductId.FLOATING_BUTTONS)) {
            return TriggerError.FLOATING_BUTTONS_NOT_PURCHASED
        } else if (key is FloatingButtonKey && key.button == null) {
            return TriggerError.FLOATING_BUTTON_DELETED
        }

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

        val containsDpadKey =
            key is KeyCodeTriggerKey && InputEventUtils.isDpadKeyCode(key.keyCode) && key.detectionSource == KeyEventDetectionSource.INPUT_METHOD

        if (showDpadImeSetupError && !isKeyMapperImeChosen && containsDpadKey) {
            return TriggerError.DPAD_IME_NOT_SELECTED
        }

        // TODO if button deleted go through the process of adding the button again.
        // TODO do not show "button deleted" twice in trigger key list item

        return null
    }
}
