package io.github.sds100.keymapper.sorting.comparators

import io.github.sds100.keymapper.mappings.keymaps.KeyMap

// Comparator.reversed() requires API level 24
class KeyMapTriggerComparator(
    private val reverse: Boolean = false,
) : Comparator<KeyMap> {
    // key -> keys length -> mode
    override fun compare(
        keyMap: KeyMap?,
        otherKeyMap: KeyMap?,
    ): Int {
        if (keyMap == null || otherKeyMap == null) {
            return 0
        }

        val trigger = keyMap.trigger
        val otherTrigger = otherKeyMap.trigger

        val keyMapTriggerKeysLength = trigger.keys.size
        val otherKeyMapTriggerKeysLength = otherTrigger.keys.size
        val maxLength = keyMapTriggerKeysLength.coerceAtMost(otherKeyMapTriggerKeysLength)

        // Compare keys one by one
        for (i in 0 until maxLength) {
            val key = trigger.keys[i]
            val otherKey = otherTrigger.keys[i]

            val result = key.compareTo(otherKey)

            if (result != 0) {
                return doFinal(result)
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

        return doFinal(result)
    }

    fun doFinal(result: Int) = if (reverse) {
        result * -1
    } else {
        result
    }
}
