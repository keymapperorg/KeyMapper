package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.mappings.DisplayConstraintUseCase
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.handle

/**
 * Created by sds100 on 18/03/2021.
 */

class ConstraintUiHelper(
    displayConstraintUseCase: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider
) : DisplayConstraintUseCase by displayConstraintUseCase, ResourceProvider by resourceProvider {

    fun getTitle(constraint: Constraint): String = when (constraint) {
        is Constraint.AppInForeground ->
            getAppName(constraint.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_foreground) }
            )

            is Constraint.AppNotInForeground ->
                getAppName(constraint.packageName).handle(
                    onSuccess = { getString(R.string.constraint_app_not_foreground_description, it) },
                    onError = { getString(R.string.constraint_choose_app_not_foreground) }
                )

        is Constraint.AppPlayingMedia ->
            getAppName(constraint.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_playing_media_description, it) },
                onError = { getString(R.string.constraint_choose_app_playing_media) }
            )

        is Constraint.BtDeviceConnected ->
            getString(
                R.string.constraint_bt_device_connected_description,
                constraint.deviceName
            )

        is Constraint.BtDeviceDisconnected ->
            getString(
                R.string.constraint_bt_device_disconnected_description,
                constraint.deviceName
            )

        is Constraint.OrientationCustom -> {
            val resId = when (constraint.orientation) {
                Orientation.ORIENTATION_0 -> R.string.constraint_choose_orientation_0
                Orientation.ORIENTATION_90 -> R.string.constraint_choose_orientation_90
                Orientation.ORIENTATION_180 -> R.string.constraint_choose_orientation_180
                Orientation.ORIENTATION_270 -> R.string.constraint_choose_orientation_270
            }

            getString(resId)
        }

        Constraint.OrientationLandscape ->
            getString(R.string.constraint_choose_orientation_landscape)

        Constraint.OrientationPortrait ->
            getString(R.string.constraint_choose_orientation_landscape)

        Constraint.ScreenOff ->
            getString(R.string.constraint_screen_off_description)

        Constraint.ScreenOn ->
            getString(R.string.constraint_screen_on_description)
    }

    fun getIcon(constraint: Constraint): IconInfo? = when (constraint) {
        is Constraint.AppInForeground -> getAppIconInfo(constraint.packageName)
        is Constraint.AppNotInForeground -> getAppIconInfo(constraint.packageName)
        is Constraint.AppPlayingMedia -> getAppIconInfo(constraint.packageName)
        is Constraint.BtDeviceConnected -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_bluetooth_connected_24),
            tintType = TintType.ON_SURFACE
        )

        is Constraint.BtDeviceDisconnected -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_bluetooth_disabled_24),
            tintType = TintType.ON_SURFACE
        )

        is Constraint.OrientationCustom -> {
            val resId = when (constraint.orientation) {
                Orientation.ORIENTATION_0 -> R.drawable.ic_outline_stay_current_portrait_24
                Orientation.ORIENTATION_90 -> R.drawable.ic_outline_stay_current_landscape_24
                Orientation.ORIENTATION_180 -> R.drawable.ic_outline_stay_current_portrait_24
                Orientation.ORIENTATION_270 -> R.drawable.ic_outline_stay_current_landscape_24
            }

            IconInfo(
                drawable = getDrawable(resId),
                tintType = TintType.ON_SURFACE
            )
        }

        Constraint.OrientationLandscape -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_stay_current_landscape_24),
            tintType = TintType.ON_SURFACE
        )

        Constraint.OrientationPortrait -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_stay_current_portrait_24),
            tintType = TintType.ON_SURFACE
        )

        Constraint.ScreenOff -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_stay_current_portrait_24),
            tintType = TintType.ON_SURFACE
        )

        Constraint.ScreenOn -> IconInfo(
            drawable = getDrawable(R.drawable.ic_baseline_mobile_off_24),
            tintType = TintType.ON_SURFACE
        )
    }

    private fun getAppIconInfo(packageName: String): IconInfo? {
        return getAppIcon(packageName).handle(
            onSuccess = { IconInfo(it, TintType.NONE) },
            onError = { null }
        )
    }
}