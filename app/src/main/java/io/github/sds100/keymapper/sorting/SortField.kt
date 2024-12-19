package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.constraints.KeyMapConstraintsComparator
import io.github.sds100.keymapper.mappings.keymaps.KeyMapActionsComparator
import io.github.sds100.keymapper.mappings.keymaps.KeyMapOptionsComparator
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTriggerComparator

enum class SortField {
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
