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
object ActionUtils {

    fun buildModel(action: Action): ActionModel {
        var title: String? = null
        var icon: Drawable? = null

        val errorMessage = getTitle(action).onSuccess { title = it }
            .then { getIcon(action) }.onSuccess { icon = it }
            .then { isValid(action) }
            .errorMessageOrNull()

        return ActionModel(title, errorMessage, icon)
    }

    private fun getTitle(action: Action): Result<String> {
        when (action.type) {
            ActionType.APP -> {
                try {
                    val applicationInfo = appCtx.packageManager.getApplicationInfo(
                        action.data,
                        PackageManager.GET_META_DATA
                    )

                    val applicationLabel = appCtx.packageManager.getApplicationLabel(applicationInfo)

                    return Success(appCtx.getString(R.string.description_open_app, applicationLabel.toString()))
                } catch (e: PackageManager.NameNotFoundException) {
                    //the app isn't installed
                    return AppNotFound()
                }
            }
        }
    }

    /**
     * Get the icon for any Action
     */
    private fun getIcon(action: Action): Result<Drawable> {
        return when (action.type) {
            ActionType.APP -> {
                try {
                    Success(appCtx.packageManager.getApplicationIcon(action.data))
                } catch (e: PackageManager.NameNotFoundException) {
                    //if the app isn't installed, it can't find the icon for it
                    AppNotFound()
                }
            }
        }
    }

    /**
     * @return if the action can't be performed, it returns an error code.
     * returns null if their if the action can be performed.
     */
    private fun isValid(action: Action): Result<Action> {
        //the action has no data
        if (action.data.isEmpty()) return NoActionData()

        when (action.type) {
            ActionType.APP -> {
                try {
                    val appInfo = appCtx.packageManager.getApplicationInfo(action.data, 0)

                    //if the app is disabled, show an error message because it won't open
                    if (!appInfo.enabled) {
                        return AppDisabled()
                    }
                } catch (e: Exception) {
                    return AppNotFound()
                }
            }
        }

        return Success(action)
    }
}

fun KeyMap.buildActionModels() = sequence {
    actionList.forEach {
        yield(ActionUtils.buildModel(it))
    }
}.toList()