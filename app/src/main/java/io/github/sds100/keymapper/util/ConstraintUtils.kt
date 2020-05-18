package io.github.sds100.keymapper.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.result.*

/**
 * Created by sds100 on 17/03/2020.
 */

object ConstraintUtils {

    fun isSupported(ctx: Context, @ConstraintType id: String): Failure? {
        when (id) {
            Constraint.BT_DEVICE_CONNECTED -> {
                if (!ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    return SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
                }
            }
        }

        return null
    }
}

fun Constraint.buildChipModel(ctx: Context): ConstraintModel {
    var description: String? = null
    var icon: Drawable? = null

    val error = getDescription(ctx).onSuccess { description = it }
        .then { getIcon(ctx) }.onSuccess { icon = it }
        .failureOrNull()

    return ConstraintModel(uniqueId, description, error, error?.getBriefMessage(ctx), icon)
}

private fun Constraint.getDescription(ctx: Context): Result<String> {
    return when (type) {
        Constraint.APP_FOREGROUND ->
            getExtraData(Extra.EXTRA_PACKAGE_NAME).then {
                try {
                    val applicationInfo = ctx.packageManager.getApplicationInfo(it, PackageManager.GET_META_DATA)

                    val applicationLabel = ctx.packageManager.getApplicationLabel(applicationInfo)

                    Success(ctx.str(R.string.constraint_app_description, applicationLabel))
                } catch (e: PackageManager.NameNotFoundException) {
                    //the app isn't installed
                    AppNotFound(it)
                }
            }

        else -> ConstraintNotFound()
    }
}

private fun Constraint.getIcon(ctx: Context): Result<Drawable> {
    return when (type) {
        Constraint.APP_FOREGROUND ->
            getExtraData(Extra.EXTRA_PACKAGE_NAME).then {
                try {
                    Success(ctx.packageManager.getApplicationIcon(it))
                } catch (e: PackageManager.NameNotFoundException) {
                    //if the app isn't installed, it can't find the icon for it
                    AppNotFound(it)
                }
            }

        else -> ConstraintNotFound()
    }
}