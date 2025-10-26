package io.github.sds100.keymapper.base.sorting.comparators

import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.constraints.DisplayConstraintUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueOrNull
import java.time.LocalDate
import java.time.ZoneOffset

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

    private fun getSecondarySortField(constraint: Constraint): KMResult<String> {
        return when (constraint.data) {
            is ConstraintData.AppInForeground -> displayConstraints.getAppName(constraint.data.packageName)
            is ConstraintData.AppNotInForeground -> displayConstraints.getAppName(constraint.data.packageName)
            is ConstraintData.AppNotPlayingMedia -> displayConstraints.getAppName(constraint.data.packageName)
            is ConstraintData.AppPlayingMedia -> displayConstraints.getAppName(constraint.data.packageName)
            is ConstraintData.BtDeviceConnected -> Success(constraint.data.deviceName)
            is ConstraintData.BtDeviceDisconnected -> Success(constraint.data.deviceName)
            is ConstraintData.Charging -> Success("")
            is ConstraintData.DeviceIsLocked -> Success("")
            is ConstraintData.DeviceIsUnlocked -> Success("")
            is ConstraintData.Discharging -> Success("")
            is ConstraintData.FlashlightOff -> Success(constraint.data.lens.toString())
            is ConstraintData.FlashlightOn -> Success(constraint.data.lens.toString())
            is ConstraintData.ImeChosen -> Success(constraint.data.imeLabel)
            is ConstraintData.ImeNotChosen -> Success(constraint.data.imeLabel)
            is ConstraintData.InPhoneCall -> Success("")
            is ConstraintData.MediaPlaying -> Success("")
            is ConstraintData.NoMediaPlaying -> Success("")
            is ConstraintData.NotInPhoneCall -> Success("")
            is ConstraintData.OrientationCustom -> Success(constraint.data.orientation.toString())
            is ConstraintData.OrientationLandscape -> Success("")
            is ConstraintData.OrientationPortrait -> Success("")
            is ConstraintData.PhoneRinging -> Success("")
            is ConstraintData.ScreenOff -> Success("")
            is ConstraintData.ScreenOn -> Success("")
            is ConstraintData.WifiConnected -> if (constraint.data.ssid == null) {
                Success("")
            } else {
                Success(constraint.data.ssid)
            }

            is ConstraintData.WifiDisconnected -> if (constraint.data.ssid == null) {
                Success("")
            } else {
                Success(constraint.data.ssid)
            }

            is ConstraintData.WifiOff -> Success("")
            is ConstraintData.WifiOn -> Success("")
            is ConstraintData.LockScreenNotShowing -> Success("")
            is ConstraintData.LockScreenShowing -> Success("")
            is ConstraintData.Time -> Success(
                constraint.data.startTime
                    .toEpochSecond(LocalDate.now(), ZoneOffset.UTC)
                    .toString(),
            )

            ConstraintData.HingeClosed -> Success("")
            ConstraintData.HingeOpen -> Success("")
        }
    }
}
