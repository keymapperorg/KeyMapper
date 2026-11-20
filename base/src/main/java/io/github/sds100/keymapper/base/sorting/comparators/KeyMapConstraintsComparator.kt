package io.github.sds100.keymapper.base.sorting.comparators

import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.keymaps.KeyMap
import java.text.Collator

class KeyMapConstraintsComparator(
    private val getTitle: (Constraint) -> String,
    /**
     * Each comparator is reversed separately instead of the entire key map list
     * and Comparator.reversed() requires API level 24 so use a custom reverse field.
     */
    private val reverse: Boolean = false,
) : Comparator<KeyMap> {
    /**
     * A collator is used for locale specific sorting.
     */
    private val collator = Collator.getInstance()

    override fun compare(keyMap: KeyMap?, otherKeyMap: KeyMap?): Int {
        if (keyMap == null || otherKeyMap == null) {
            return 0
        }

        val keyMapConstraintsLength = keyMap.constraintState.allConstraints.size
        val otherKeyMapConstraintsLength = otherKeyMap.constraintState.allConstraints.size
        val maxLength = keyMapConstraintsLength.coerceAtMost(otherKeyMapConstraintsLength)

        // Compare constraints one by one
        for (i in 0 until maxLength) {
            val constraint = keyMap.constraintState.allConstraints.elementAt(i)
            val otherConstraint = otherKeyMap.constraintState.allConstraints.elementAt(i)

            val result = collator.compare(getTitle(constraint), getTitle(otherConstraint))

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
