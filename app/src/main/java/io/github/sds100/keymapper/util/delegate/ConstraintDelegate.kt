package io.github.sds100.keymapper.util.delegate

import android.view.Surface
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.ConstraintMode
import io.github.sds100.keymapper.util.IConstraintDelegate
import io.github.sds100.keymapper.util.IConstraintState
import io.github.sds100.keymapper.util.ScreenRotationUtils
import io.github.sds100.keymapper.util.result.valueOrNull

/**
 * Created by sds100 on 13/12/20.
 */
class ConstraintDelegate(constraintState: IConstraintState
) : IConstraintState by constraintState, IConstraintDelegate {

    override fun Array<Constraint>.constraintsSatisfied(@ConstraintMode mode: Int): Boolean {
        if (this.isEmpty()) return true

        return if (mode == Constraint.MODE_AND) {
            all { constraintSatisfied(it) }
        } else {
            any { constraintSatisfied(it) }
        }
    }

    private fun constraintSatisfied(constraint: Constraint): Boolean {
        val data = when (constraint.type) {
            Constraint.APP_FOREGROUND, Constraint.APP_NOT_FOREGROUND, Constraint.APP_PLAYING_MEDIA ->
                constraint.getExtraData(Constraint.EXTRA_PACKAGE_NAME).valueOrNull()

            Constraint.BT_DEVICE_CONNECTED, Constraint.BT_DEVICE_DISCONNECTED ->
                constraint.getExtraData(Constraint.EXTRA_BT_ADDRESS).valueOrNull()

            Constraint.SCREEN_ON,
            Constraint.SCREEN_OFF,
            in Constraint.ORIENTATION_CONSTRAINTS -> ""

            else -> throw Exception(
                "Don't know how to get the relevant data from this Constraint! ${constraint.type}")
        } ?: return false

        return constraintSatisfied(constraint.type, data)
    }

    private fun constraintSatisfied(id: String, data: String): Boolean {
        return when (id) {
            Constraint.APP_FOREGROUND -> data == currentPackageName
            Constraint.APP_NOT_FOREGROUND -> data != currentPackageName
            Constraint.APP_PLAYING_MEDIA -> data == highestPriorityPackagePlayingMedia

            Constraint.BT_DEVICE_CONNECTED -> isBluetoothDeviceConnected(data)
            Constraint.BT_DEVICE_DISCONNECTED -> !isBluetoothDeviceConnected(data)

            Constraint.SCREEN_ON -> isScreenOn
            Constraint.SCREEN_OFF -> !isScreenOn

            Constraint.ORIENTATION_PORTRAIT -> orientation?.let { ScreenRotationUtils.isPortrait(it) }
                ?: false
            Constraint.ORIENTATION_LANDSCAPE -> orientation?.let { ScreenRotationUtils.isLandscape(it) }
                ?: false
            Constraint.ORIENTATION_0 -> orientation?.let { it == Surface.ROTATION_0 }
                ?: false
            Constraint.ORIENTATION_90 -> orientation?.let { it == Surface.ROTATION_90 }
                ?: false
            Constraint.ORIENTATION_180 -> orientation?.let { it == Surface.ROTATION_180 }
                ?: false
            Constraint.ORIENTATION_270 -> orientation?.let { it == Surface.ROTATION_270 }
                ?: false

            else -> true
        }
    }
}