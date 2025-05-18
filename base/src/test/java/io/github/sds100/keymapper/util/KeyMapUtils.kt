package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.trigger.TriggerKey
import io.github.sds100.keymapper.base.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.base.trigger.TriggerMode

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
): _root_ide_package_.io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey = _root_ide_package_.io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey(
    keyCode = keyCode,
    device = device,
    clickType = clickType,
    consumeEvent = consume,
    detectionSource = detectionSource,
)
