package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 04/04/2021.
 */

@Serializable
data class ConstraintState(
    val constraints: Set<Constraint> = emptySet(),
    val mode: ConstraintMode = ConstraintMode.AND,
)

// Comparator.reversed() requires API level 24
class KeyMapConstraintsComparator(
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
            val constraint1 = keyMap.constraintState.constraints.elementAt(i)
            val constraint2 = otherKeyMap.constraintState.constraints.elementAt(i)

            val result = constraint1.compareTo(constraint2)

            if (result != 0) {
                return doFinal(result)
            }
        }

        // If constraints are equal, compare the length
        val comparison = keyMapConstraintsLength.compareTo(otherKeyMapConstraintsLength)

        return doFinal(comparison)
    }

    fun doFinal(result: Int) = if (reverse) {
        result * -1
    } else {
        result
    }
}
