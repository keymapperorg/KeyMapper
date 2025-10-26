package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.utils.KeyCodeStrings
import io.github.sds100.keymapper.base.utils.ScancodeStrings
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils

sealed interface KeyCodeTriggerKey {
    val keyCode: Int

    /**
     * Scancodes were only saved to KeyEvent trigger keys in version 4.0.0 so this is null
     * to be backwards compatible.
     */
    val scanCode: Int?
    val clickType: ClickType

    /**
     * The user can specify they want to detect with the scancode instead of the key code.
     */
    val detectWithScanCodeUserSetting: Boolean

    /**
     * Whether the event that triggers this key will be consumed and not passed
     * onto subsequent apps. E.g consuming the volume down key event will mean the volume
     * doesn't change.
     */
    val consumeEvent: Boolean

    fun isSameDevice(otherKey: KeyCodeTriggerKey): Boolean
}

fun KeyCodeTriggerKey.detectWithScancode(): Boolean = scanCode != null && (detectWithScanCodeUserSetting || isKeyCodeUnknown())

fun KeyCodeTriggerKey.isKeyCodeUnknown(): Boolean = KeyEventUtils.isKeyCodeUnknown(keyCode)

fun KeyCodeTriggerKey.isScanCodeDetectionUserConfigurable(): Boolean = scanCode != null && !isKeyCodeUnknown()

/**
 * Get the label for the key code or scan code, depending on whether to detect it with a scan code.
 */
fun KeyCodeTriggerKey.getCodeLabel(resourceProvider: ResourceProvider): String {
    if (detectWithScancode() && scanCode != null) {
        val codeLabel =
            ScancodeStrings.getScancodeLabel(scanCode!!)
                ?: resourceProvider.getString(R.string.trigger_key_unknown_scan_code, scanCode!!)

        return "$codeLabel (${resourceProvider.getString(R.string.trigger_key_scan_code_detection_flag)})"
    } else {
        return KeyCodeStrings.keyCodeToString(keyCode)
            ?: resourceProvider.getString(R.string.trigger_key_unknown_key_code, keyCode)
    }
}
