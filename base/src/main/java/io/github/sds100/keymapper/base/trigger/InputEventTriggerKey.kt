package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.keymaps.ClickType

sealed interface InputEventTriggerKey {
    val keyCode: Int

    /**
     * Scancodes were only saved to KeyEvent trigger keys in version 4.0.0 so this is null
     * to be backwards compatible.
     */
    val scanCode: Int?
    val clickType: ClickType
}