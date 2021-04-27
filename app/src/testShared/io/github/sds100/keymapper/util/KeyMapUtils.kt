package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTrigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.mappings.ClickType

/**
 * Created by sds100 on 19/04/2021.
 */


 fun singleKeyTrigger(key: TriggerKey): KeyMapTrigger {
    return KeyMapTrigger(
        keys = listOf(key),
        mode = TriggerMode.Undefined
    )
}

 fun parallelTrigger(vararg keys: TriggerKey): KeyMapTrigger {
    return KeyMapTrigger(
        keys = keys.toList(),
        mode = TriggerMode.Parallel(keys[0].clickType)
    )
}

 fun sequenceTrigger(vararg keys: TriggerKey): KeyMapTrigger {
    return KeyMapTrigger(
        keys = keys.toList(),
        mode = TriggerMode.Sequence
    )
}

 fun triggerKey(
     keyCode: Int,
     device: TriggerKeyDevice = TriggerKeyDevice.Internal,
     clickType: ClickType = ClickType.SHORT_PRESS,
     consume: Boolean = true
): TriggerKey {
    return TriggerKey(
        keyCode = keyCode,
        device = device,
        clickType = clickType,
        consumeKeyEvent = consume
    )
}