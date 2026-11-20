package io.github.sds100.keymapper.base.sorting.comparators

import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.trigger.Trigger
import java.text.Collator

class KeyMapTriggerComparator(
    /**
     * The name of each key in the trigger, in the same order as the trigger's keys.
     */
    private val getKeyNames: (Trigger) -> List<String>,
    /**
     * Each comparator is reversed separately instead of the entire key map list
     * and Comparator.reversed() requires API level 24 so use a custom reverse field.
     */
    private val reverse: Boolean = false,
) : Comparator<KeyMap> {
    private val collator = Collator.getInstance()

    /**
     * Compare trigger key names -> trigger keys -> keys length -> trigger mode
     */
    override fun compare(keyMap: KeyMap?, otherKeyMap: KeyMap?): Int {
        if (keyMap == null || otherKeyMap == null) {
            return 0
        }

        val trigger = keyMap.trigger
        val otherTrigger = otherKeyMap.trigger

        val keyMapTriggerKeysLength = trigger.keys.size
        val otherKeyMapTriggerKeysLength = otherTrigger.keys.size
        val maxLength = keyMapTriggerKeysLength.coerceAtMost(otherKeyMapTriggerKeysLength)

        val keyNames = getKeyNames(trigger)
        val otherKeyNames = getKeyNames(otherTrigger)

        // Compare keys one by one
        for (i in 0 until maxLength) {
            val key = trigger.keys[i]
            val otherKey = otherTrigger.keys[i]

            val nameResult = collator.compare(keyNames[i], otherKeyNames[i])

            if (nameResult != 0) {
                return invertIfReverse(nameResult)
            }

            val result = key.compareTo(otherKey)

            if (result != 0) {
                return invertIfReverse(result)
            }
        }

        // If keys are equal compare length
        // Otherwise compare mode
        val result = compareValuesBy(
            trigger,
            otherTrigger,
            { it.keys.size },
            { it.mode },
        )

        return invertIfReverse(result)
    }

    private fun invertIfReverse(result: Int) = if (reverse) {
        result * -1
    } else {
        result
    }
}
