package io.github.sds100.keymapper.util

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.ActionChipModel
import io.github.sds100.keymapper.data.model.ActionModel
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.util.result.*
import splitties.init.appCtx
import splitties.resources.appStr

/**
 * Created by sds100 on 03/09/2018.
 */

fun Action.buildModel(): ActionModel {
    var title: String? = null
    var icon: Drawable? = null

    val error = getTitle().onSuccess { title = it }
        .then { getIcon() }.onSuccess { icon = it }
        .then { canBePerformed() }
        .failureOrNull()

    val flags = if (flags == 0) {
        null
    } else {
        buildString {
            val flagLabels = getFlagLabelList()

            flagLabels.forEachIndexed { index, label ->
                if (index != 0) {
                    append(appStr(R.string.interpunct))
                }

                append(label)
            }
        }
    }

    return ActionModel(uniqueId, title, icon, flags, error)
}

fun Action.buildChipModel(): ActionChipModel {
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

    return ActionChipModel(uniqueId, description, error, icon)
}

private fun Action.getTitle(): Result<String> = when (type) {
    ActionType.APP -> {
        try {
            val applicationInfo = appCtx.packageManager.getApplicationInfo(data, PackageManager.GET_META_DATA)

            val applicationLabel = appCtx.packageManager.getApplicationLabel(applicationInfo)

            Success(appCtx.getString(R.string.description_open_app, applicationLabel.toString()))
        } catch (e: PackageManager.NameNotFoundException) {
            //the app isn't installed
            AppNotFound(data)
        }
    }

    ActionType.APP_SHORTCUT -> {
        getExtraData(Action.EXTRA_SHORTCUT_TITLE)
    }
}

/**
 * Get the icon for any Action
 */
private fun Action.getIcon(): Result<Drawable?> = when (type) {
    ActionType.APP -> {
        try {
            Success(appCtx.packageManager.getApplicationIcon(data))
        } catch (e: PackageManager.NameNotFoundException) {
            //if the app isn't installed, it can't find the icon for it
            AppNotFound(data)
        }
    }

    ActionType.APP_SHORTCUT -> getExtraData(Action.EXTRA_PACKAGE_NAME).then {
        Success(appCtx.packageManager.getApplicationIcon(it))
    }
}

/**
 * @return if the action can't be performed, it returns an error code.
 * returns null if their if the action can be performed.
 */
private fun Action.canBePerformed()
    : Result<Action> {
    //the action has no data
    if (data.isEmpty()) return NoActionData()

    when (type) {
        ActionType.APP, ActionType.APP_SHORTCUT -> {
            val packageName: Result<String> = when (type) {
                ActionType.APP -> Success(data)
                ActionType.APP_SHORTCUT -> getExtraData(Action.EXTRA_PACKAGE_NAME)
            }

            return packageName.then {
                try {
                    val appInfo = appCtx.packageManager.getApplicationInfo(it, 0)

                    //if the app is disabled, show an error message because it won't open
                    if (!appInfo.enabled) {
                        return@then AppDisabled(data)
                    }

                    return@then Success(this)

                } catch (e: Exception) {
                    return@then AppNotFound(data)
                }
            }
        }
    }
}

fun Action.getAvailableFlags(): List<Int> = sequence<Int> {

}.toList()

fun KeyMap.buildActionChipModels() = sequence {
    actionList.forEach { action ->
        yield(action.buildChipModel())
    }
}.toList()