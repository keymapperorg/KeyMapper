package io.github.sds100.keymapper.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.SystemActionUtils.getDescriptionWithOption
import io.github.sds100.keymapper.util.SystemActionUtils.getDescriptionWithOptionSet
import io.github.sds100.keymapper.util.result.*
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 03/09/2018.
 */

object ActionUtils {

    fun isVolumeAction(actionData: String): Boolean {
        return listOf(
            SystemAction.VOLUME_DECREASE_STREAM,
            SystemAction.VOLUME_INCREASE_STREAM,
            SystemAction.VOLUME_DOWN,
            SystemAction.VOLUME_UP,
            SystemAction.VOLUME_MUTE,
            SystemAction.VOLUME_TOGGLE_MUTE,
            SystemAction.VOLUME_UNMUTE
        ).contains(actionData)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
fun Action.buildModel(ctx: Context): ActionModel {
    var title: String? = null
    var icon: Drawable? = null

    val error = getTitle(ctx).onSuccess { title = it }
        .then { getIcon(ctx).onSuccess { icon = it } }
        .then { canBePerformed(ctx) }
        .failureOrNull()

    val flags = if (flags == 0) {
        null
    } else {
        buildString {
            val flagLabels = getFlagLabelList(ctx)

            flagLabels.forEachIndexed { index, label ->
                if (index != 0) {
                    append(" ${ctx.str(R.string.interpunct)} ")
                }

                append(label)
            }
        }
    }

    return ActionModel(uniqueId, type, title, icon, flags, error, error?.getBriefMessage(ctx))
}

fun Action.buildChipModel(ctx: Context): ActionChipModel {
    var title: String? = null
    var icon: Drawable? = null

    val error = getTitle(ctx).onSuccess { title = it }
        .then { getIcon(ctx).onSuccess { icon = it } }
        .then { canBePerformed(ctx) }
        .failureOrNull()

    val description = buildString {
        val flagLabels = getFlagLabelList(ctx)

        if (title == null) {
            append(error?.getBriefMessage(ctx))
        } else {
            append(title)
        }

        flagLabels.forEach {
            append(" • $it")
        }
    }

    return ActionChipModel(uniqueId, type, description, error, icon)
}

fun Action.getTitle(ctx: Context): Result<String> {
    return when (type) {
        ActionType.APP -> {
            try {
                val applicationInfo = ctx.packageManager.getApplicationInfo(data, PackageManager.GET_META_DATA)

                val applicationLabel = ctx.packageManager.getApplicationLabel(applicationInfo)

                Success(ctx.str(R.string.description_open_app, applicationLabel.toString()))
            } catch (e: PackageManager.NameNotFoundException) {
                //the app isn't installed
                AppNotFound(data)
            }
        }

        ActionType.APP_SHORTCUT -> {
            extras.getData(Action.EXTRA_SHORTCUT_TITLE)
        }

        ActionType.KEY_EVENT -> {
            val key = if (data.toInt() > KeyEvent.getMaxKeyCode()) {
                "Key Code $data"
            } else {
                KeyEvent.keyCodeToString(data.toInt())
            }

            val metaStateString = buildString {

                extras.getData(Action.EXTRA_KEY_EVENT_META_STATE).onSuccess { metaState ->
                    KeyEventUtils.MODIFIER_LABELS.entries.forEach {
                        val modifier = it.key
                        val labelRes = it.value

                        if (metaState.toInt().hasFlag(modifier)) {
                            append("${ctx.str(labelRes)} + ")
                        }
                    }
                }
            }

            Success(ctx.str(R.string.description_keyevent, formatArgArray = arrayOf(metaStateString, key)))
        }

        ActionType.TEXT_BLOCK -> {
            val text = data
            Success(ctx.str(R.string.description_text_block, text))
        }

        ActionType.URL -> {
            Success(ctx.str(R.string.description_url, data))
        }

        ActionType.SYSTEM_ACTION -> {
            val systemActionId = data

            SystemActionUtils.getSystemActionDef(systemActionId) then { systemActionDef ->
                if (systemActionDef.hasOptions) {
                    val optionData = extras.getData(Option.getExtraIdForOption(systemActionId))

                    when (systemActionDef.optionType) {
                        OptionType.SINGLE -> {
                            optionData then {
                                Option.getOptionLabel(ctx, systemActionId, it)
                            } then {
                                Success(systemActionDef.getDescriptionWithOption(ctx, it))

                            } otherwise {
                                if (systemActionId == SystemAction.SWITCH_KEYBOARD) {

                                    extras.getData(Action.EXTRA_IME_NAME) then {
                                        Success(systemActionDef.getDescriptionWithOption(ctx, it))
                                    }

                                } else {
                                    Success(ctx.str(systemActionDef.descriptionRes))
                                }
                            }
                        }

                        OptionType.MULTIPLE -> {
                            optionData then {
                                Option.optionSetFromString(it)
                            } then {
                                Option.labelsFromOptionSet(ctx, systemActionId, it)
                            } then {
                                Success(systemActionDef.getDescriptionWithOptionSet(ctx, it))
                            }
                        }
                    }
                } else {
                    Success(ctx.str(systemActionDef.descriptionRes))
                }
            }
        }

        ActionType.TAP_COORDINATE -> {
            val x = data.split(',')[0]
            val y = data.split(',')[1]

            extras.getData(Action.EXTRA_COORDINATE_DESCRIPTION) then {
                Success(ctx.str(resId = R.string.description_tap_coordinate_with_description, formatArgArray = arrayOf(x, y, it)))
            } otherwise {
                Success(ctx.str(resId = R.string.description_tap_coordinate_default, formatArgArray = arrayOf(x, y)))
            }
        }
    }.then {
        extras.getData(Action.EXTRA_MULTIPLIER).valueOrNull()?.toIntOrNull()?.let { multiplier ->
            return@then Success("(${multiplier}x) $it")
        }

        Success(it)
    }
}

/**
 * Get the icon for any Action
 */
fun Action.getIcon(ctx: Context): Result<Drawable?> = when (type) {
    ActionType.APP -> {
        try {
            Success(ctx.packageManager.getApplicationIcon(data))
        } catch (e: PackageManager.NameNotFoundException) {
            //if the app isn't installed, it can't find the icon for it
            AppNotFound(data)
        }
    }

    ActionType.APP_SHORTCUT -> extras.getData(Action.EXTRA_PACKAGE_NAME).then {
        Success(ctx.packageManager.getApplicationIcon(it))
    } otherwise { Success(null) }

    ActionType.SYSTEM_ACTION -> {
        //convert the string representation of the enum entry into an enum object
        val systemActionId = data

        SystemActionUtils.getSystemActionDef(systemActionId).then {
            Success(null)
            Success(ctx.drawable(it.iconRes))
        }
    }

    else -> Success(null)
}

/**
 * @return if the action can't be performed, it returns an error code.
 * returns null if their if the action can be performed.
 */
fun Action.canBePerformed(ctx: Context): Result<Action> {
    //the action has no data
    if (data.isEmpty()) return NoActionData()

    if (requiresIME) {
        if (!KeyboardUtils.isCompatibleImeEnabled()) {
            return NoCompatibleImeEnabled()
        }

        if (!KeyboardUtils.isCompatibleImeChosen(ctx)) {
            return NoCompatibleImeChosen()
        }
    }

    when (type) {
        ActionType.APP, ActionType.APP_SHORTCUT -> {
            val packageName: Result<String> =
                if (type == ActionType.APP) {
                    Success(data)
                } else {
                    extras.getData(Action.EXTRA_PACKAGE_NAME)
                }

            return packageName.then {
                try {
                    val appInfo = ctx.packageManager.getApplicationInfo(it, 0)

                    //if the app is disabled, show an error message because it won't open
                    if (!appInfo.enabled) {
                        return@then AppDisabled(data)
                    }

                    return@then Success(this)

                } catch (e: Exception) {
                    return@then AppNotFound(data)
                }
            }.otherwise {
                if (type == ActionType.APP_SHORTCUT) {
                    Success(this)
                } else {
                    it
                }
            }
        }

        ActionType.TAP_COORDINATE -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return SdkVersionTooLow(Build.VERSION_CODES.N)
            }
        }

        ActionType.SYSTEM_ACTION -> {
            SystemActionUtils.getSystemActionDef(data).onSuccess { systemActionDef ->

                //If an activity to open doesn't exist, the app crashes.
                if (systemActionDef.id == SystemAction.OPEN_VOICE_ASSISTANT) {
                    val activityExists =
                        Intent(Intent.ACTION_VOICE_COMMAND).resolveActivityInfo(ctx.packageManager, 0) != null

                    if (!activityExists) {
                        return GoogleAppNotFound()
                    }
                }

                if (Build.VERSION.SDK_INT < systemActionDef.minApi) {
                    return SdkVersionTooLow(systemActionDef.minApi)
                }

                if (Build.VERSION.SDK_INT > systemActionDef.maxApi) {
                    return SdkVersionTooHigh(systemActionDef.maxApi)
                }

                systemActionDef.permissions.forEach { permission ->
                    if (!PermissionUtils.isPermissionGranted(permission)) {
                        return PermissionDenied(permission)
                    }
                }

                for (feature in systemActionDef.features) {
                    if (!ctx.packageManager.hasSystemFeature(feature)) {
                        return FeatureUnavailable(feature)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (systemActionDef.id == SystemAction.TOGGLE_FLASHLIGHT
                        || systemActionDef.id == SystemAction.ENABLE_FLASHLIGHT
                        || systemActionDef.id == SystemAction.DISABLE_FLASHLIGHT) {

                        extras.getData(Action.EXTRA_LENS).onSuccess { lensOptionId ->
                            val sdkLensId = Option.OPTION_ID_SDK_ID_MAP[lensOptionId]
                                ?: error("Can't find sdk id for that option id")

                            if (!CameraUtils.hasFlashFacing(sdkLensId)) {

                                when (lensOptionId) {
                                    Option.LENS_FRONT -> FrontFlashNotFound()
                                    Option.LENS_BACK -> BackFlashNotFound()
                                }
                            }
                        }
                    }
                }

                if (systemActionDef.id == SystemAction.SWITCH_KEYBOARD) {

                    extras.getData(Action.EXTRA_IME_ID).onSuccess { imeId ->
                        if (!KeyboardUtils.isImeEnabled(imeId)) {
                            var errorData = imeId

                            extras.getData(Action.EXTRA_IME_NAME).onSuccess { imeName ->
                                errorData = imeName
                            }

                            return ImeNotFound(errorData)
                        }
                    }
                }
            }
        }
    }

    return Success(this)
}

/**
 * A string representation of all the extras in an [Action] that are necessary to perform it.
 */
val Action.dataExtraString: String
    get() = buildString {
        Action.DATA_EXTRAS.forEach {
            extras.getData(it).onSuccess { data ->
                append("$it$data")
            }
        }
    }

val Action.requiresIME: Boolean
    get() {
        return type == ActionType.KEY_EVENT ||
            type == ActionType.TEXT_BLOCK ||
            data == SystemAction.MOVE_CURSOR_TO_END
    }

fun Action.getFlagLabelList(ctx: Context): List<String> = sequence {
    Action.ACTION_FLAG_LABEL_MAP.keys.forEach { flag ->
        if (flags.hasFlag(flag)) {
            yield(ctx.str(Action.ACTION_FLAG_LABEL_MAP.getValue(flag)))
        }
    }
}.toList()