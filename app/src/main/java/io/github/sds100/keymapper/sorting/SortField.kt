package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.constraints.ConstraintUiHelper
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
}

fun SortField.Companion.getTriggerComparator(
    reverse: Boolean,
) = KeyMapTriggerComparator(reverse)

fun SortField.Companion.getActionsComparator(
    reverse: Boolean,
) = KeyMapActionsComparator(reverse)

fun SortField.Companion.getConstraintsComparator(
    constraintUiHelper: ConstraintUiHelper,
    reverse: Boolean,
) = KeyMapConstraintsComparator(constraintUiHelper, reverse)

fun SortField.Companion.getOptionsComparator(
    reverse: Boolean,
) = KeyMapOptionsComparator(reverse)
