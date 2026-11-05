package io.github.sds100.keymapper.base.sorting.comparators

import io.github.sds100.keymapper.base.keymaps.KeyMap
import java.text.Collator

/**
 * Sorts alphabetically using the rules of the device's locale. Unnamed key maps are always last,
 * even if the order is reversed.
 */
class KeyMapNameComparator(private val reverse: Boolean = false) : Comparator<KeyMap> {
    private val collator = Collator.getInstance()

    override fun compare(keyMap: KeyMap?, otherKeyMap: KeyMap?): Int {
        if (keyMap == null || otherKeyMap == null) {
            return 0
        }

        val isUnnamed = keyMap.name.isBlank()
        val isOtherUnnamed = otherKeyMap.name.isBlank()

        if (isUnnamed || isOtherUnnamed) {
            return isUnnamed.compareTo(isOtherUnnamed)
        }

        val result = collator.compare(keyMap.name, otherKeyMap.name)

        return invertIfReverse(result)
    }

    private fun invertIfReverse(result: Int) = if (reverse) {
        result * -1
    } else {
        result
    }
}
