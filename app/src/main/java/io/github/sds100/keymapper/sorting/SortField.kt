package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.sorting.comparators.KeyMapActionsComparator
import io.github.sds100.keymapper.sorting.comparators.KeyMapConstraintsComparator
import io.github.sds100.keymapper.sorting.comparators.KeyMapOptionsComparator
import io.github.sds100.keymapper.sorting.comparators.KeyMapTriggerComparator
import kotlinx.serialization.Serializable

@Serializable
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
