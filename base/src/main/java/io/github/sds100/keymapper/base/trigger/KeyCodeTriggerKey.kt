package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.keymaps.ClickType

/**
 * This is a type for trigger keys that are detected by key code. This is a different meaning to
 * key *event*.
 */
sealed interface KeyCodeTriggerKey {
    val keyCode: Int
    val scanCode: Int?
    val clickType: ClickType
}