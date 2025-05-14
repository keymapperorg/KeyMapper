package io.github.sds100.keymapper.sorting.comparators

import io.github.sds100.keymapper.keymaps.KeyMap

class KeyMapOptionsComparator(
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

        val result = compareValuesBy(
            keyMap,
            otherKeyMap,
            { it.vibrate },
            { it.trigger.screenOffTrigger },
            { it.trigger.triggerFromOtherApps },
            { it.showToast },
        )

        return invertIfReverse(result)
    }

    private fun invertIfReverse(result: Int) = if (reverse) {
        result * -1
    } else {
        result
    }
}
