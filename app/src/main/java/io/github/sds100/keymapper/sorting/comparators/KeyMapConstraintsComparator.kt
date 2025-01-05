package io.github.sds100.keymapper.sorting.comparators

import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.mappings.keymaps.KeyMap

class KeyMapConstraintsComparator(
    private val constraintUiHelper: ConstraintUiHelper,
    /**
     * Each comparator is reversed separately instead of the entire key map list
     * and Comparator.reversed() requires API level 24 so use a custom reverse field.
     */
    private val reverse: Boolean = false,
) : Comparator<KeyMap> {
    override fun compare(
        keyMap: KeyMap?,
        otherKeyMap: KeyMap?,
    ): Int {
        if (keyMap == null || otherKeyMap == null) {
            return 0
        }

        val keyMapConstraintsLength = keyMap.constraintState.constraints.size
        val otherKeyMapConstraintsLength = otherKeyMap.constraintState.constraints.size
        val maxLength = keyMapConstraintsLength.coerceAtMost(otherKeyMapConstraintsLength)

        // Compare constraints one by one
        for (i in 0 until maxLength) {
            val constraint = keyMap.constraintState.constraints.elementAt(i)
            val otherConstraint = otherKeyMap.constraintState.constraints.elementAt(i)

            val result = constraintUiHelper.compareConstraints(constraint, otherConstraint)

            if (result != 0) {
                return invertIfReverse(result)
            }
        }

        // If constraints are equal, compare the length
        val comparison = keyMapConstraintsLength.compareTo(otherKeyMapConstraintsLength)

        return invertIfReverse(comparison)
    }

    private fun invertIfReverse(result: Int) = if (reverse) {
        result * -1
    } else {
        result
    }
}

private fun ConstraintUiHelper.compareConstraints(
    constraint: Constraint,
    otherConstraint: Constraint,
): Int {
    // If constraints are different, compare their generic titles.
    //
    // This ensures that there won't be a case like this:
    // 1. "A" is in foreground
    // 2. "A" is not in foreground
    // 3. "B" is in foreground
    //
    // Instead, it will be like this:
    // 1. "A" is in foreground
    // 2. "B" is in foreground
    // 3. "A" is not in foreground

    return if (constraint::class != otherConstraint::class) {
        val generic = getGenericTitle(constraint).lowercase()
        val otherGeneric = getGenericTitle(otherConstraint).lowercase()
        generic.compareTo(otherGeneric)
    } else {
        val leftString = getTitle(constraint).lowercase()
        val rightString = getTitle(otherConstraint).lowercase()
        leftString.compareTo(rightString)
    }
}
