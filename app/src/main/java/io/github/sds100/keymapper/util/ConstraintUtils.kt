package io.github.sds100.keymapper.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.ConstraintModel
import io.github.sds100.keymapper.data.model.ConstraintType
import io.github.sds100.keymapper.util.result.*

/**
 * Created by sds100 on 17/03/2020.
 */

object ConstraintUtils {

    fun isSupported(ctx: Context, @ConstraintType id: String): Failure? {
        when (id) {
            Constraint.BT_DEVICE_CONNECTED, Constraint.BT_DEVICE_DISCONNECTED -> {
                if (!ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    return SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
                }
            }

            Constraint.SCREEN_OFF, Constraint.SCREEN_ON -> {
                if (!PermissionUtils.isPermissionGranted(ctx, Constants.PERMISSION_ROOT)) {
                    return PermissionDenied(Constants.PERMISSION_ROOT)
                }
            }

            in Constraint.ORIENTATION_CONSTRAINTS -> {
                if (!PermissionUtils.isPermissionGranted(ctx, Manifest.permission.WRITE_SETTINGS)) {
                    return PermissionDenied(Manifest.permission.WRITE_SETTINGS)
                }
            }

            Constraint.APP_PLAYING_MEDIA -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    return SdkVersionTooLow(Build.VERSION_CODES.LOLLIPOP)
                }

                if (!PermissionUtils.isPermissionGranted(ctx, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)) {
                    return PermissionDenied(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                }
            }
        }

        return null
    }
}

fun Constraint.buildModel(ctx: Context): ConstraintModel {
    var description: String? = null
    var icon: Drawable? = null

    val error = getDescription(ctx).onSuccess { description = it }
        .then { getIcon(ctx) }.onSuccess { icon = it }
        .then { ConstraintUtils.isSupported(ctx, type) ?: Success(Unit) }
        .failureOrNull()

    val iconTintOnSurface = when (type) {
        Constraint.APP_FOREGROUND,
        Constraint.APP_NOT_FOREGROUND,
        Constraint.APP_PLAYING_MEDIA -> false

        else -> true
    }

    return ConstraintModel(uniqueId, description, error, error?.getBriefMessage(ctx), icon, iconTintOnSurface)
}

private fun Constraint.getDescription(ctx: Context): Result<String> {
    return when (type) {
        Constraint.APP_FOREGROUND, Constraint.APP_NOT_FOREGROUND, Constraint.APP_PLAYING_MEDIA ->
            getExtraData(Constraint.EXTRA_PACKAGE_NAME).then {
                try {
                    val applicationInfo = ctx.packageManager.getApplicationInfo(it, PackageManager.GET_META_DATA)

                    val applicationLabel = ctx.packageManager.getApplicationLabel(applicationInfo)

                    val descriptionRes = when (type) {
                        Constraint.APP_FOREGROUND -> R.string.constraint_app_foreground_description
                        Constraint.APP_NOT_FOREGROUND -> R.string.constraint_app_not_foreground_description
                        Constraint.APP_PLAYING_MEDIA -> R.string.constraint_app_playing_media_description
                        else -> return@then ConstraintNotFound()
                    }

                    Success(ctx.str(descriptionRes, applicationLabel))
                } catch (e: PackageManager.NameNotFoundException) {
                    //the app isn't installed
                    AppNotFound(it)
                }
            }

        Constraint.BT_DEVICE_CONNECTED, Constraint.BT_DEVICE_DISCONNECTED -> getExtraData(Constraint.EXTRA_BT_NAME).then {
            val descriptionRes = if (type == Constraint.BT_DEVICE_CONNECTED) {
                R.string.constraint_bt_device_connected_description
            } else {
                R.string.constraint_bt_device_disconnected_description
            }

            Success(ctx.str(descriptionRes, it))
        }

        Constraint.SCREEN_ON -> Success(ctx.str(R.string.constraint_screen_on_description))
        Constraint.SCREEN_OFF -> Success(ctx.str(R.string.constraint_screen_off_description))

        Constraint.ORIENTATION_PORTRAIT -> Success(ctx.str(R.string.constraint_choose_orientation_portrait))
        Constraint.ORIENTATION_LANDSCAPE -> Success(ctx.str(R.string.constraint_choose_orientation_landscape))
        Constraint.ORIENTATION_0 -> Success(ctx.str(R.string.constraint_choose_orientation_0))
        Constraint.ORIENTATION_90 -> Success(ctx.str(R.string.constraint_choose_orientation_90))
        Constraint.ORIENTATION_180 -> Success(ctx.str(R.string.constraint_choose_orientation_180))
        Constraint.ORIENTATION_270 -> Success(ctx.str(R.string.constraint_choose_orientation_270))

        else -> ConstraintNotFound()
    }
}

private fun Constraint.getIcon(ctx: Context): Result<Drawable> {
    return when (type) {
        Constraint.APP_FOREGROUND, Constraint.APP_NOT_FOREGROUND, Constraint.APP_PLAYING_MEDIA ->
            getExtraData(Constraint.EXTRA_PACKAGE_NAME).then {
                try {
                    Success(ctx.packageManager.getApplicationIcon(it))
                } catch (e: PackageManager.NameNotFoundException) {
                    //if the app isn't installed, it can't find the icon for it
                    AppNotFound(it)
                }
            }

        Constraint.BT_DEVICE_CONNECTED ->
            Success(ctx.safeVectorDrawable(R.drawable.ic_outline_bluetooth_connected_24)!!)
        Constraint.BT_DEVICE_DISCONNECTED ->
            Success(ctx.safeVectorDrawable(R.drawable.ic_outline_bluetooth_disabled_24)!!)

        Constraint.SCREEN_ON ->
            Success(ctx.safeVectorDrawable(R.drawable.ic_outline_stay_current_portrait_24)!!)
        Constraint.SCREEN_OFF ->
            Success(ctx.safeVectorDrawable(R.drawable.ic_baseline_mobile_off_24)!!)

        Constraint.ORIENTATION_PORTRAIT, Constraint.ORIENTATION_0, Constraint.ORIENTATION_180 ->
            Success(ctx.safeVectorDrawable(R.drawable.ic_outline_stay_current_portrait_24)!!)
        Constraint.ORIENTATION_LANDSCAPE, Constraint.ORIENTATION_90, Constraint.ORIENTATION_270 ->
            Success(ctx.safeVectorDrawable(R.drawable.ic_outline_stay_current_landscape_24)!!)

        else -> ConstraintNotFound()
    }
}