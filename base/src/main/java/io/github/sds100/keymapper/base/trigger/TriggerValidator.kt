package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.keymaps.ClickType

/**
 * Checks that the trigger is still valid. If it is a parallel trigger it removes
 * conflicting keys that can't both be pressed down at the same time. And if it is a single
 * key trigger then it will check it has one key. If not then it will convert it into a sequence
 * trigger and not a parallel trigger so no keys are removed.
 */
fun Trigger.validate(): Trigger {
    when (this.mode) {
        is TriggerMode.Parallel -> {
            return validateParallelTrigger(this)
        }

        TriggerMode.Undefined -> {
            return validateSingleKeyTrigger(this)
        }

        TriggerMode.Sequence -> {
            // No validation needed for sequence triggers. Any keys can be pressed in any order
            if (keys.size <= 1) {
                return copy(mode = TriggerMode.Undefined)
            }

            return this
        }
    }
}

private fun validateSingleKeyTrigger(trigger: Trigger): Trigger {
    if (trigger.keys.size > 1) {
        // If there are multiple keys then we convert it to a sequence trigger
        return Trigger(mode = TriggerMode.Sequence, keys = trigger.keys.take(1))
    } else {
        return trigger
    }
}

private fun validateParallelTrigger(trigger: Trigger): Trigger {
    if (trigger.keys.size <= 1) {
        return trigger.copy(mode = TriggerMode.Undefined)
    }

    var newMode = trigger.mode

    outerLoop@ for (key in trigger.keys) {
        // If there are conflicting keys then set the mode to sequence trigger
        for (otherKey in trigger.keys) {
            if (key != otherKey && key.isLogicallyEqual(otherKey)) {
                newMode = TriggerMode.Sequence
                break@outerLoop
            }
        }

        // Set the trigger mode to a short press if any keys are not compatible with the selected
        // trigger mode
        if (newMode is TriggerMode.Parallel) {
            if (
                (newMode.clickType == ClickType.LONG_PRESS && !key.allowedLongPress) ||
                (newMode.clickType == ClickType.DOUBLE_PRESS && !key.allowedDoublePress)
            ) {
                newMode = TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)
            }
        }
    }

    var newKeys = trigger.keys

    // If the trigger is still a parallel trigger then check that all the keys can be
    // pressed at the same time.
    if (newMode is TriggerMode.Parallel) {
        newKeys = trigger.keys.distinctBy { key ->
            when (key) {
                // You can't mix assistant trigger types in a parallel trigger because there is no notion of a "down" key event, which means they can't be pressed at the same time
                is AssistantTriggerKey, is FingerprintTriggerKey -> 0
                is FloatingButtonKey -> key.buttonUid

                is KeyEventTriggerKey -> {
                    if (key.detectWithScancode()) {
                        Pair(key.scanCode, key.device)
                    } else {
                        Pair(key.keyCode, key.device)
                    }
                }

                is EvdevTriggerKey -> {
                    if (key.detectWithScancode()) {
                        Pair(key.scanCode, key.device)
                    } else {
                        Pair(key.keyCode, key.device)
                    }
                }
            }
        }.toMutableList()
    }

    return trigger.copy(mode = newMode, keys = newKeys)
}
