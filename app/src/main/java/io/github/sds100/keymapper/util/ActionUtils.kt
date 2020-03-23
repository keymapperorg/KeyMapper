package io.github.sds100.keymapper.util

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.ActionModel
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.util.result.*
import splitties.init.appCtx

/**
 * Created by sds100 on 03/09/2018.
 */

/**
 * Provides functions commonly used with [Action]s
 */
fun Action.buildModel(): ActionModel {
    var title: String? = null
    var icon: Drawable? = null

    val error = getTitle().onSuccess { title = it }
        .then { getIcon() }.onSuccess { icon = it }
        .then { canBePerformed() }
        .failureOrNull()

    val description = buildString {
        val flagLabels = getFlagLabelList()

        if (title == null) {
            append(error?.briefMessage)
        } else {
            append(title)
        }

        flagLabels.forEach {
            append(" • $it")
        }
    }

    return ActionModel(description, error, icon)
}

private fun Action.getTitle(): Result<String> {
    when (type) {
        ActionType.APP -> {
            try {
                val applicationInfo = appCtx.packageManager.getApplicationInfo(data, PackageManager.GET_META_DATA)

                val applicationLabel = appCtx.packageManager.getApplicationLabel(applicationInfo)

                return Success(appCtx.getString(R.string.description_open_app, applicationLabel.toString()))
            } catch (e: PackageManager.NameNotFoundException) {
                //the app isn't installed
                return AppNotFound(data)
            }
        }
    }
}

/**
 * Get the icon for any Action
 */
private fun Action.getIcon(): Result<Drawable> {
    return when (type) {
        ActionType.APP -> {
            try {
                Success(appCtx.packageManager.getApplicationIcon(data))
            } catch (e: PackageManager.NameNotFoundException) {
                //if the app isn't installed, it can't find the icon for it
                AppNotFound(data)
            }
        }
    }
}

/**
 * @return if the action can't be performed, it returns an error code.
 * returns null if their if the action can be performed.
 */
private fun Action.canBePerformed(): Result<Action> {
    //the action has no data
    if (data.isEmpty()) return NoActionData()

    when (type) {
        ActionType.APP -> {
            try {
                val appInfo = appCtx.packageManager.getApplicationInfo(data, 0)

                //if the app is disabled, show an error message because it won't open
                if (!appInfo.enabled) {
                    return AppDisabled(data)
                }
            } catch (e: Exception) {
                return AppNotFound(data)
            }
        }
    }

    return Success(this)
}

fun KeyMap.buildActionModels() = sequence {
    actionList.forEach { action ->
        yield(action.buildModel())
    }
}.toList()