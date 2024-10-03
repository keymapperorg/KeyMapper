package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.trigger.Trigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode

/**
 * Created by sds100 on 19/04/2021.
 */

fun singleKeyTrigger(key: TriggerKey): Trigger = Trigger(
    keys = listOf(key),
    mode = TriggerMode.Undefined,
)

fun parallelTrigger(vararg keys: TriggerKey): Trigger = Trigger(
    keys = keys.toList(),
    mode = TriggerMode.Parallel(keys[0].clickType),
)

fun sequenceTrigger(vararg keys: TriggerKey): Trigger = Trigger(
    keys = keys.toList(),
    mode = TriggerMode.Sequence,
)

fun triggerKey(
    keyCode: Int,
    device: TriggerKeyDevice = TriggerKeyDevice.Internal,
    clickType: ClickType = ClickType.SHORT_PRESS,
    consume: Boolean = true,
): TriggerKey = TriggerKey(
    keyCode = keyCode,
    device = device,
    clickType = clickType,
    consumeKeyEvent = consume,
)
