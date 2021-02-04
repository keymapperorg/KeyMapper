package io.github.sds100.keymapper.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.hasRootPermission
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.showDeviceDescriptors
import io.github.sds100.keymapper.globalPreferences
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
fun Action.buildModel(ctx: Context, deviceInfoList: List<DeviceInfo>): ActionModel {
    var title: String? = null
    var icon: Drawable? = null

    val error = getTitle(ctx, deviceInfoList).onSuccess { title = it }
        .then { getIcon(ctx).onSuccess { icon = it } }
        .then { canBePerformed(ctx) }
        .failureOrNull()

    val extraInfo = buildString {
        val interpunct = ctx.str(R.string.interpunct)
        val flagLabels = getFlagLabelList(ctx)

        flagLabels.forEachIndexed { index, label ->
            if (index != 0) {
                append(" $interpunct ")
            }

            append(label)
        }

        extras.getData(Action.EXTRA_DELAY_BEFORE_NEXT_ACTION).onSuccess {
            if (this.isNotBlank()) {
                append(" $interpunct ")
            }

            append(ctx.str(R.string.action_title_wait, it))
        }
    }.takeIf { it.isNotBlank() }

    return ActionModel(uid, type, title, icon, extraInfo, error, error?.getBriefMessage(ctx))
}

fun Action.buildChipModel(ctx: Context, deviceInfoList: List<DeviceInfo>): ActionChipModel {
    var title: String? = null
    var icon: Drawable? = null

    val error = getTitle(ctx, deviceInfoList).onSuccess { title = it }
        .then { getIcon(ctx).onSuccess { icon = it } }
        .then { canBePerformed(ctx) }
        .failureOrNull()

    val description = buildString {
        val interpunct = ctx.str(R.string.interpunct)

        val flagLabels = getFlagLabelList(ctx)

        if (title == null) {
            append(error?.getBriefMessage(ctx))
        } else {
            append(title)
        }

        flagLabels.forEach {
            append(" $interpunct $it")
        }

        extras.getData(Action.EXTRA_DELAY_BEFORE_NEXT_ACTION).onSuccess {
            append(" $interpunct ${ctx.str(R.string.action_title_wait, it)}")
        }
    }.takeIf { it.isNotBlank() }

    return ActionChipModel(type, description, error, icon)
}

fun Action.getTitle(ctx: Context, deviceInfoList: List<DeviceInfo>): Result<String> {
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

            val useShell = extras.getData(Action.EXTRA_KEY_EVENT_USE_SHELL)
                .valueOrNull()
                .toBoolean()

            val title = extras.getData(Action.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR).handle(
                onSuccess = { descriptor ->
                    val deviceName = deviceInfoList.find { it.descriptor == descriptor }?.name?.let { name ->
                        if (ctx.globalPreferences.showDeviceDescriptors.firstBlocking()) {
                            "$name (${descriptor.substring(0..4)})"
                        } else {
                            name
                        }
                    }

                    val strRes = if (useShell) {
                        R.string.description_keyevent_from_device_through_shell
                    } else {
                        R.string.description_keyevent_from_device
                    }

                    ctx.str(
                        strRes,
                        formatArgArray = arrayOf(metaStateString, key, deviceName)
                    )
                },

                onFailure = {
                    val strRes = if (useShell) {
                        R.string.description_keyevent_through_shell
                    } else {
                        R.string.description_keyevent
                    }

                    ctx.str(strRes, formatArgArray = arrayOf(metaStateString, key))
                }
            )

            Success(title)
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

        ActionType.INTENT -> {
            extras.getData(Action.EXTRA_INTENT_DESCRIPTION) then { description ->
                extras.getData(Action.EXTRA_INTENT_TARGET) then { target ->
                    val title = when (IntentTarget.valueOf(target)) {
                        IntentTarget.ACTIVITY ->
                            ctx.str(R.string.action_title_intent_start_activity, description)

                        IntentTarget.BROADCAST_RECEIVER ->
                            ctx.str(R.string.action_title_intent_send_broadcast, description)

                        IntentTarget.SERVICE ->
                            ctx.str(R.string.action_title_intent_start_service, description)
                    }

                    Success(title)
                }
            }
        }

        ActionType.PHONE_CALL -> Success(ctx.str(R.string.description_phone_call, data))

    }
        .then {
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
        try {
            Success(ctx.packageManager.getApplicationIcon(it))
        } catch (e: PackageManager.NameNotFoundException) {
            AppNotFound(it)
        }
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

    if (!PermissionUtils.canUseShizuku(ctx) && requiresIME) {
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

        ActionType.KEY_EVENT -> {
            val useShell = extras.getData(Action.EXTRA_KEY_EVENT_USE_SHELL)
                .valueOrNull()
                .toBoolean()

            if (useShell && !ctx.globalPreferences.hasRootPermission.firstBlocking()) {
                return PermissionDenied(Constants.PERMISSION_ROOT)
            }
        }

        ActionType.TAP_COORDINATE -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return SdkVersionTooLow(Build.VERSION_CODES.N)
            }
        }

        ActionType.PHONE_CALL -> {
            if (!PermissionUtils.isPermissionGranted(ctx, Manifest.permission.CALL_PHONE)) {
                return PermissionDenied(Manifest.permission.CALL_PHONE)
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
                    if (!PermissionUtils.isPermissionGranted(ctx, permission)) {
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

val Action.canBeHeldDown: Boolean
    get() {
        val useShell = extras.getData(Action.EXTRA_KEY_EVENT_USE_SHELL).valueOrNull().toBoolean()

        return (type == ActionType.KEY_EVENT && !useShell)
            || (type == ActionType.TAP_COORDINATE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
    }

val Action.requiresIME: Boolean
    get() {
        val useShell = extras.getData(Action.EXTRA_KEY_EVENT_USE_SHELL).valueOrNull().toBoolean()
        return (type == ActionType.KEY_EVENT && !useShell) ||
            type == ActionType.TEXT_BLOCK ||
            data == SystemAction.MOVE_CURSOR_TO_END
    }

val Action.repeat: Boolean
    get() = flags.hasFlag(Action.ACTION_FLAG_REPEAT)

val Action.holdDown: Boolean
    get() = flags.hasFlag(Action.ACTION_FLAG_HOLD_DOWN)

val Action.showVolumeUi: Boolean
    get() = flags.hasFlag(Action.ACTION_FLAG_SHOW_VOLUME_UI)

val Action.stopRepeatingWhenTriggerPressedAgain: Boolean
    get() = extras.getData(Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR).valueOrNull()?.toInt() ==
        Action.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN

val Action.stopRepeatingWhenTriggerReleased: Boolean
    get() = !stopRepeatingWhenTriggerPressedAgain

val Action.stopHoldDownWhenTriggerPressedAgain: Boolean
    get() = extras.getData(Action.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR).valueOrNull()?.toInt() ==
        Action.STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN

val Action.stopHoldDownWhenTriggerReleased: Boolean
    get() = !stopHoldDownWhenTriggerPressedAgain

val Action.delayBeforeNextAction: Int?
    get() = extras.getData(Action.EXTRA_DELAY_BEFORE_NEXT_ACTION).valueOrNull()?.toInt()

val Action.multiplier: Int?
    get() = extras.getData(Action.EXTRA_MULTIPLIER).valueOrNull()?.toInt()

val Action.holdDownDuration: Int?
    get() = extras.getData(Action.EXTRA_HOLD_DOWN_DURATION).valueOrNull()?.toInt()

val Action.repeatRate: Int?
    get() = extras.getData(Action.EXTRA_REPEAT_RATE).valueOrNull()?.toInt()

val Action.repeatDelay: Int?
    get() = extras.getData(Action.EXTRA_REPEAT_DELAY).valueOrNull()?.toInt()

fun Action.getFlagLabelList(ctx: Context): List<String> = sequence {
    Action.ACTION_FLAG_LABEL_MAP.keys.forEach { flag ->
        if (flags.hasFlag(flag)) {
            yield(ctx.str(Action.ACTION_FLAG_LABEL_MAP.getValue(flag)))
        }
    }
}.toList()