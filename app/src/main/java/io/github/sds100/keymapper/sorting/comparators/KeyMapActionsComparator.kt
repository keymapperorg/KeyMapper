package io.github.sds100.keymapper.sorting.comparators

import io.github.sds100.keymapper.mappings.keymaps.KeyMap

class KeyMapActionsComparator(
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

        val keyMapActionsLength = keyMap.actionList.size
        val otherKeyMapActionsLength = otherKeyMap.actionList.size
        val maxLength = keyMapActionsLength.coerceAtMost(otherKeyMapActionsLength)

        // compare actions one by one
        for (i in 0 until maxLength) {
            val action1 = keyMap.actionList[i]
            val action2 = otherKeyMap.actionList[i]

            val result = compareValuesBy(
                action1,
                action2,
                { it.data },
                { it.repeat },
                { it.multiplier },
                { it.repeatLimit },
                { it.repeatRate },
                { it.repeatDelay },
                { it.repeatMode },
                { it.delayBeforeNextAction },
            )

            if (result != 0) {
                return invertIfReverse(result)
            }
        }

        // if actions are equal compare length
        val comparison = keyMap.actionList.size.compareTo(otherKeyMap.actionList.size)

        return invertIfReverse(comparison)
    }

    private fun invertIfReverse(result: Int) = if (reverse) {
        result * -1
    } else {
        result
    }
}
