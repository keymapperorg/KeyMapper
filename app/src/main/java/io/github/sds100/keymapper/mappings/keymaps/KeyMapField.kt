package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.constraints.KeyMapConstraintsComparator
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTriggerComparator

enum class KeyMapField {
    TRIGGER,
    ACTIONS,
    CONSTRAINTS,
    OPTIONS,
    ;

    fun getComparator(reverse: Boolean) = when (this) {
        TRIGGER -> KeyMapTriggerComparator(reverse)
        ACTIONS -> KeyMapActionsComparator(reverse)
        CONSTRAINTS -> KeyMapConstraintsComparator(reverse)
        OPTIONS -> KeyMapOptionsComparator(reverse)
    }
}
