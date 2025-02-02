package io.github.sds100.keymapper.sorting.comparators

import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.mappings.DisplayConstraintUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.then
import io.github.sds100.keymapper.util.valueOrNull

class KeyMapConstraintsComparator(
    private val displayConstraints: DisplayConstraintUseCase,
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

            val result = compareConstraints(constraint, otherConstraint)

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

    private fun compareConstraints(
        constraint: Constraint,
        otherConstraint: Constraint,
    ): Int {
        // If constraints are different, compare their types so they are ordered
        // by their type.
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

        if (constraint.id == otherConstraint.id) {
            // If constraints are the same then sort by a secondary field.
            val comparison = getSecondarySortField(constraint).then { sortData ->
                return@then getSecondarySortField(otherConstraint).then { otherSortData ->
                    Success(sortData.compareTo(otherSortData))
                }
            }

            return comparison.valueOrNull() ?: 0
        }

        return constraint.id.ordinal.compareTo(otherConstraint.id.ordinal)
    }

    private fun getSecondarySortField(constraint: Constraint): Result<String> {
        return when (constraint) {
            is Constraint.AppInForeground -> displayConstraints.getAppName(constraint.packageName)
            is Constraint.AppNotInForeground -> displayConstraints.getAppName(constraint.packageName)
            is Constraint.AppNotPlayingMedia -> displayConstraints.getAppName(constraint.packageName)
            is Constraint.AppPlayingMedia -> displayConstraints.getAppName(constraint.packageName)
            is Constraint.BtDeviceConnected -> Success(constraint.deviceName)
            is Constraint.BtDeviceDisconnected -> Success(constraint.deviceName)
            Constraint.Charging -> Success("")
            Constraint.DeviceIsLocked -> Success("")
            Constraint.DeviceIsUnlocked -> Success("")
            Constraint.Discharging -> Success("")
            is Constraint.FlashlightOff -> Success(constraint.lens.toString())
            is Constraint.FlashlightOn -> Success(constraint.lens.toString())
            is Constraint.ImeChosen -> Success(constraint.imeLabel)
            is Constraint.ImeNotChosen -> Success(constraint.imeLabel)
            Constraint.InPhoneCall -> Success("")
            Constraint.MediaPlaying -> Success("")
            Constraint.NoMediaPlaying -> Success("")
            Constraint.NotInPhoneCall -> Success("")
            is Constraint.OrientationCustom -> Success(constraint.orientation.toString())
            Constraint.OrientationLandscape -> Success("")
            Constraint.OrientationPortrait -> Success("")
            Constraint.PhoneRinging -> Success("")
            Constraint.ScreenOff -> Success("")
            Constraint.ScreenOn -> Success("")
            is Constraint.WifiConnected -> if (constraint.ssid == null) {
                Success("")
            } else {
                Success(constraint.ssid)
            }

            is Constraint.WifiDisconnected -> if (constraint.ssid == null) {
                Success("")
            } else {
                Success(constraint.ssid)
            }

            Constraint.WifiOff -> Success("")
            Constraint.WifiOn -> Success("")
        }
    }
}
