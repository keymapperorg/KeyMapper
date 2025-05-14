package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.keymaps.ClickType
import io.github.sds100.keymapper.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.trigger.Trigger
import io.github.sds100.keymapper.trigger.TriggerKey
import io.github.sds100.keymapper.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.trigger.TriggerMode

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
    detectionSource: KeyEventDetectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
): _root_ide_package_.io.github.sds100.keymapper.trigger.KeyCodeTriggerKey = _root_ide_package_.io.github.sds100.keymapper.trigger.KeyCodeTriggerKey(
    keyCode = keyCode,
    device = device,
    clickType = clickType,
    consumeEvent = consume,
    detectionSource = detectionSource,
)
