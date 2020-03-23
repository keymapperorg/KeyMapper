package io.github.sds100.keymapper.util

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.result.*
import splitties.init.appCtx
import splitties.resources.appStr

/**
 * Created by sds100 on 17/03/2020.
 */

object ConstraintUtils {
    fun isSupported(@ConstraintId id: String): Failure? {
        when (id) {
            Constraint.BT_DEVICE_CONNECTED -> {
                if (!appCtx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    return SystemFeatureNotSupported(PackageManager.FEATURE_BLUETOOTH)
                }
            }
        }

        return null
    }
}

fun Constraint.buildModel(): ConstraintModel {
    var description: String? = null
    var icon: Drawable? = null

    val error = getDescription().onSuccess { description = it }
        .then { getIcon() }.onSuccess { icon = it }
        .failureOrNull()

    return ConstraintModel(description, error, icon)
}

private fun Constraint.getDescription(): Result<String> {
    return when (id) {
        Constraint.APP_FOREGROUND ->
            getExtraData(Extra.EXTRA_PACKAGE_NAME).then {
                try {
                    val applicationInfo = appCtx.packageManager.getApplicationInfo(it, PackageManager.GET_META_DATA)

                    val applicationLabel = appCtx.packageManager.getApplicationLabel(applicationInfo)

                    Success(appStr(R.string.constraint_app_description, applicationLabel))
                } catch (e: PackageManager.NameNotFoundException) {
                    //the app isn't installed
                    AppNotFound(it)
                }
            }

        else -> ConstraintNotFound()
    }
}

private fun Constraint.getIcon(): Result<Drawable> {
    return when (id) {
        Constraint.APP_FOREGROUND ->
            getExtraData(Extra.EXTRA_PACKAGE_NAME).then {
                try {
                    Success(appCtx.packageManager.getApplicationIcon(it))
                } catch (e: PackageManager.NameNotFoundException) {
                    //if the app isn't installed, it can't find the icon for it
                    AppNotFound(it)
                }
            }

        else -> ConstraintNotFound()
    }
}

fun KeyMap.buildConstraintModels() = sequence {
    constraintList.forEach { constraint ->
        yield(constraint.buildModel())
    }
}.toList()