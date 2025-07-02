package io.github.sds100.keymapper.base.trigger

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.keymaps.requiresImeKeyEventForwardingInPhoneCall
import io.github.sds100.keymapper.base.purchasing.ProductId
import io.github.sds100.keymapper.base.purchasing.PurchasingError
import io.github.sds100.keymapper.system.inputevents.InputEventUtils

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
) {
    companion object {
        private val keysThatRequireDndAccess = arrayOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
        )
    }

    fun getTriggerError(keyMap: KeyMap, key: TriggerKey): TriggerError? {
        purchases.onSuccess { purchases ->
            if (key is AssistantTriggerKey && !purchases.contains(ProductId.ASSISTANT_TRIGGER)) {
                return TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED
            }

            if (key is FloatingButtonKey && !purchases.contains(ProductId.FLOATING_BUTTONS)) {
                return TriggerError.FLOATING_BUTTONS_NOT_PURCHASED
            }
        }.onFailure { error ->
            if ((key is AssistantTriggerKey || key is FloatingButtonKey) && error == PurchasingError.PurchasingProcessError.NetworkError) {
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
            key is KeyCodeTriggerKey && key.keyCode in keysThatRequireDndAccess

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
            key is KeyCodeTriggerKey &&
                InputEventUtils.isDpadKeyCode(
                    key.keyCode,
                ) &&
                key.detectionSource == KeyEventDetectionSource.INPUT_METHOD

        if (showDpadImeSetupError && !isKeyMapperImeChosen && containsDpadKey) {
            return TriggerError.DPAD_IME_NOT_SELECTED
        }

        return null
    }
}
