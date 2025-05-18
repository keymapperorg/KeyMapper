package io.github.sds100.keymapper.base.actions

import androidx.core.net.toUri
import io.github.sds100.keymapper.common.utils.NodeInteractionType
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.db.typeconverter.ConstantTypeConverters
import io.github.sds100.keymapper.data.db.typeconverter.NodeInteractionTypeSetTypeConverter
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.intents.IntentExtraModel
import io.github.sds100.keymapper.system.intents.IntentTarget
import io.github.sds100.keymapper.system.network.HttpMethod
import io.github.sds100.keymapper.system.volume.DndMode
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.common.utils.getKey
import io.github.sds100.keymapper.common.utils.PinchScreenType
import kotlinx.serialization.json.Json
import io.github.sds100.keymapper.common.utils.hasFlag

object ActionDataEntityMapper {

    fun fromEntity(entity: ActionEntity): ActionData? {
        val actionId = when (entity.type) {
            ActionEntity.Type.APP -> ActionId.APP
            ActionEntity.Type.APP_SHORTCUT -> ActionId.APP_SHORTCUT
            ActionEntity.Type.KEY_EVENT -> ActionId.KEY_EVENT
            ActionEntity.Type.TEXT_BLOCK -> ActionId.TEXT
            ActionEntity.Type.URL -> ActionId.URL
            ActionEntity.Type.TAP_COORDINATE -> ActionId.TAP_SCREEN
            ActionEntity.Type.SWIPE_COORDINATE -> ActionId.SWIPE_SCREEN
            ActionEntity.Type.PINCH_COORDINATE -> ActionId.PINCH_SCREEN
            ActionEntity.Type.INTENT -> ActionId.INTENT
            ActionEntity.Type.PHONE_CALL -> ActionId.PHONE_CALL
            ActionEntity.Type.SOUND -> ActionId.SOUND
            ActionEntity.Type.SYSTEM_ACTION -> {
                SYSTEM_ACTION_ID_MAP.getKey(entity.data) ?: return null
            }

            ActionEntity.Type.INTERACT_UI_ELEMENT -> ActionId.INTERACT_UI_ELEMENT
        }

        return when (actionId) {
            ActionId.APP -> ActionData.App(packageName = entity.data)

            ActionId.APP_SHORTCUT -> {
                val packageName =
                    entity.extras.getData(ActionEntity.EXTRA_PACKAGE_NAME).valueOrNull()

                val shortcutTitle =
                    entity.extras.getData(ActionEntity.EXTRA_SHORTCUT_TITLE).valueOrNull()
                        ?: return null

                ActionData.AppShortcut(
                    packageName = packageName,
                    shortcutTitle = shortcutTitle,
                    uri = entity.data,
                )
            }

            ActionId.KEY_EVENT, ActionId.KEY_CODE -> {
                val metaState =
                    entity.extras.getData(ActionEntity.EXTRA_KEY_EVENT_META_STATE).valueOrNull()
                        ?.toInt()
                        ?: 0

                val deviceDescriptor =
                    entity.extras.getData(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR)
                        .valueOrNull()

                val deviceName =
                    entity.extras.getData(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME)
                        .valueOrNull() ?: ""

                val useShell =
                    entity.extras.getData(ActionEntity.EXTRA_KEY_EVENT_USE_SHELL).then {
                        (it == "true").success()
                    }.valueOrNull() ?: false

                val device = if (deviceDescriptor != null) {
                    ActionData.InputKeyEvent.Device(deviceDescriptor, deviceName)
                } else {
                    null
                }

                ActionData.InputKeyEvent(
                    keyCode = entity.data.toInt(),
                    metaState = metaState,
                    useShell = useShell,
                    device = device,
                )
            }

            ActionId.TEXT -> ActionData.Text(text = entity.data)

            ActionId.URL -> ActionData.Url(url = entity.data)

            ActionId.TAP_SCREEN -> {
                val x = entity.data.split(',')[0].toInt()
                val y = entity.data.split(',')[1].toInt()
                val description = entity.extras.getData(ActionEntity.EXTRA_COORDINATE_DESCRIPTION)
                    .valueOrNull()

                ActionData.TapScreen(x = x, y = y, description = description)
            }

            ActionId.SWIPE_SCREEN -> {
                val splitData = entity.data.trim().split(',')
                var xStart = 0
                var yStart = 0
                var xEnd = 0
                var yEnd = 0
                var fingerCount = 1
                var duration = 250

                if (splitData.isNotEmpty()) {
                    xStart = splitData[0].trim().toInt()
                }

                if (splitData.size >= 2) {
                    yStart = splitData[1].trim().toInt()
                }

                if (splitData.size >= 3) {
                    xEnd = splitData[2].trim().toInt()
                }

                if (splitData.size >= 4) {
                    yEnd = splitData[3].trim().toInt()
                }

                if (splitData.size >= 5) {
                    fingerCount = splitData[4].trim().toInt()
                }

                if (splitData.size >= 6) {
                    duration = splitData[5].trim().toInt()
                }

                val description = entity.extras.getData(ActionEntity.EXTRA_COORDINATE_DESCRIPTION)
                    .valueOrNull()

                ActionData.SwipeScreen(
                    xStart = xStart,
                    yStart = yStart,
                    xEnd = xEnd,
                    yEnd = yEnd,
                    fingerCount = fingerCount,
                    duration = duration,
                    description = description,
                )
            }

            ActionId.PINCH_SCREEN -> {
                val splitData = entity.data.trim().split(',')

                var x = 0
                var y = 0
                var pinchType = PinchScreenType.PINCH_IN
                var distance = 0
                var fingerCount = 2
                var duration = 250

                if (splitData.isNotEmpty()) {
                    x = splitData[0].trim().toInt()
                }

                if (splitData.size >= 2) {
                    y = splitData[1].trim().toInt()
                }

                if (splitData.size >= 3) {
                    distance = splitData[2].trim().toInt()
                }

                if (splitData.size >= 4) {
                    val tempType = splitData[3].trim()

                    pinchType = if (tempType == PinchScreenType.PINCH_IN.name) {
                        PinchScreenType.PINCH_IN
                    } else {
                        PinchScreenType.PINCH_OUT
                    }
                }

                if (splitData.size >= 5) {
                    fingerCount = splitData[4].trim().toInt().coerceAtLeast(2)
                }

                if (splitData.size >= 6) {
                    duration = splitData[5].trim().toInt()
                }

                val description = entity.extras.getData(ActionEntity.EXTRA_COORDINATE_DESCRIPTION)
                    .valueOrNull()

                ActionData.PinchScreen(
                    x = x,
                    y = y,
                    distance = distance,
                    pinchType = pinchType,
                    fingerCount = fingerCount,
                    duration = duration,
                    description = description,
                )
            }

            ActionId.INTENT -> {
                val target = entity.extras.getData(ActionEntity.EXTRA_INTENT_TARGET).then {
                    INTENT_TARGET_MAP.getKey(it).success()
                }.valueOrNull() ?: return null

                val description =
                    entity.extras.getData(ActionEntity.EXTRA_INTENT_DESCRIPTION).valueOrNull()
                        ?: return null

                val intentExtras = entity.extras.getData(ActionEntity.EXTRA_INTENT_EXTRAS).then {
                    Json.decodeFromString<List<IntentExtraModel>>(it).success()
                }.valueOrNull()

                return ActionData.Intent(
                    target = target,
                    description = description,
                    uri = entity.data,
                    extras = intentExtras ?: emptyList(),
                )
            }

            ActionId.PHONE_CALL -> ActionData.PhoneCall(number = entity.data)

            ActionId.SOUND -> {
                val isRingtoneUri = try {
                    entity.data.toUri().scheme != null
                } catch (e: Exception) {
                    false
                }

                if (isRingtoneUri) {
                    return ActionData.Sound.Ringtone(entity.data)
                } else {
                    val soundFileDescription =
                        entity.extras.getData(ActionEntity.EXTRA_SOUND_FILE_DESCRIPTION)
                            .valueOrNull() ?: return null

                    ActionData.Sound.SoundFile(
                        soundUid = entity.data,
                        soundDescription = soundFileDescription,
                    )
                }
            }

            ActionId.VOLUME_INCREASE_STREAM,
            ActionId.VOLUME_DECREASE_STREAM,
            -> {
                val stream =
                    entity.extras.getData(ActionEntity.EXTRA_STREAM_TYPE).then {
                        VOLUME_STREAM_MAP.getKey(it)!!.success()
                    }.valueOrNull() ?: return null

                val showVolumeUi =
                    entity.flags.hasFlag(ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI)

                when (actionId) {
                    ActionId.VOLUME_INCREASE_STREAM ->
                        ActionData.Volume.Stream.Increase(showVolumeUi, stream)

                    ActionId.VOLUME_DECREASE_STREAM ->
                        ActionData.Volume.Stream.Decrease(showVolumeUi, stream)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.VOLUME_UP,
            ActionId.VOLUME_DOWN,
            ActionId.VOLUME_TOGGLE_MUTE,
            ActionId.VOLUME_UNMUTE,
            ActionId.VOLUME_MUTE,
            -> {
                val showVolumeUi =
                    entity.flags.hasFlag(ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI)

                when (actionId) {
                    ActionId.VOLUME_UP -> ActionData.Volume.Up(showVolumeUi)
                    ActionId.VOLUME_DOWN -> ActionData.Volume.Down(showVolumeUi)
                    ActionId.VOLUME_TOGGLE_MUTE -> ActionData.Volume.ToggleMute(
                        showVolumeUi,
                    )

                    ActionId.VOLUME_UNMUTE -> ActionData.Volume.UnMute(showVolumeUi)
                    ActionId.VOLUME_MUTE -> ActionData.Volume.Mute(showVolumeUi)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.TOGGLE_FLASHLIGHT,
            ActionId.ENABLE_FLASHLIGHT,
            ActionId.CHANGE_FLASHLIGHT_STRENGTH,
            -> {
                val lens = entity.extras.getData(ActionEntity.EXTRA_LENS).then {
                    LENS_MAP.getKey(it)!!.success()
                }.valueOrNull() ?: return null

                val flashStrength = entity.extras.getData(ActionEntity.EXTRA_FLASH_STRENGTH).then {
                    it.toFloatOrNull().success()
                }.valueOrNull()

                when (actionId) {
                    ActionId.TOGGLE_FLASHLIGHT -> ActionData.Flashlight.Toggle(lens, flashStrength)
                    ActionId.ENABLE_FLASHLIGHT -> ActionData.Flashlight.Enable(lens, flashStrength)

                    ActionId.CHANGE_FLASHLIGHT_STRENGTH -> {
                        flashStrength ?: return null
                        ActionData.Flashlight.ChangeStrength(
                            lens,
                            flashStrength,
                        )
                    }

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.DISABLE_FLASHLIGHT,
            -> {
                val lens = entity.extras.getData(ActionEntity.EXTRA_LENS).then {
                    LENS_MAP.getKey(it)!!.success()
                }.valueOrNull() ?: return null
                ActionData.Flashlight.Disable(lens)
            }

            ActionId.TOGGLE_DND_MODE,
            ActionId.ENABLE_DND_MODE,
            -> {
                val dndMode = entity.extras.getData(ActionEntity.EXTRA_DND_MODE).then {
                    DND_MODE_MAP.getKey(it)!!.success()
                }.valueOrNull() ?: return null

                when (actionId) {
                    ActionId.TOGGLE_DND_MODE ->
                        ActionData.DoNotDisturb.Toggle(dndMode)

                    ActionId.ENABLE_DND_MODE ->
                        ActionData.DoNotDisturb.Enable(dndMode)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.DISABLE_DND_MODE -> {
                return ActionData.DoNotDisturb.Disable
            }

            ActionId.PAUSE_MEDIA_PACKAGE,
            ActionId.PLAY_MEDIA_PACKAGE,
            ActionId.PLAY_PAUSE_MEDIA_PACKAGE,
            ActionId.NEXT_TRACK_PACKAGE,
            ActionId.PREVIOUS_TRACK_PACKAGE,
            ActionId.FAST_FORWARD_PACKAGE,
            ActionId.REWIND_PACKAGE,
            ActionId.STOP_MEDIA_PACKAGE,
            ActionId.STEP_FORWARD_PACKAGE,
            ActionId.STEP_BACKWARD_PACKAGE,
            -> {
                val packageName =
                    entity.extras.getData(ActionEntity.EXTRA_PACKAGE_NAME).valueOrNull()
                        ?: return null

                when (actionId) {
                    ActionId.PAUSE_MEDIA_PACKAGE ->
                        ActionData.ControlMediaForApp.Pause(packageName)

                    ActionId.PLAY_MEDIA_PACKAGE ->
                        ActionData.ControlMediaForApp.Play(packageName)

                    ActionId.PLAY_PAUSE_MEDIA_PACKAGE ->
                        ActionData.ControlMediaForApp.PlayPause(packageName)

                    ActionId.NEXT_TRACK_PACKAGE ->
                        ActionData.ControlMediaForApp.NextTrack(packageName)

                    ActionId.PREVIOUS_TRACK_PACKAGE ->
                        ActionData.ControlMediaForApp.PreviousTrack(packageName)

                    ActionId.FAST_FORWARD_PACKAGE ->
                        ActionData.ControlMediaForApp.FastForward(packageName)

                    ActionId.REWIND_PACKAGE ->
                        ActionData.ControlMediaForApp.Rewind(packageName)

                    ActionId.STOP_MEDIA_PACKAGE ->
                        ActionData.ControlMediaForApp.Stop(packageName)

                    ActionId.STEP_FORWARD_PACKAGE ->
                        ActionData.ControlMediaForApp.StepForward(packageName)

                    ActionId.STEP_BACKWARD_PACKAGE ->
                        ActionData.ControlMediaForApp.StepBackward(packageName)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.CHANGE_RINGER_MODE -> {
                val ringerMode =
                    entity.extras.getData(ActionEntity.EXTRA_RINGER_MODE).then {
                        RINGER_MODE_MAP.getKey(it)!!.success()
                    }.valueOrNull() ?: return null

                ActionData.Volume.SetRingerMode(ringerMode)
            }

            ActionId.SWITCH_KEYBOARD -> {
                val imeId = entity.extras.getData(ActionEntity.EXTRA_IME_ID)
                    .valueOrNull() ?: return null

                val imeName = entity.extras.getData(ActionEntity.EXTRA_IME_NAME)
                    .valueOrNull() ?: return null

                ActionData.SwitchKeyboard(imeId, imeName)
            }

            ActionId.CYCLE_ROTATIONS -> {
                val orientations =
                    entity.extras.getData(ActionEntity.EXTRA_ORIENTATIONS)
                        .then { extraValue ->
                            extraValue
                                .split(",")
                                .map { ConstantTypeConverters.ORIENTATION_MAP.getKey(it)!! }
                                .success()
                        }.valueOrNull() ?: return null

                ActionData.Rotation.CycleRotations(orientations)
            }

            ActionId.TOGGLE_WIFI -> ActionData.Wifi.Toggle
            ActionId.ENABLE_WIFI -> ActionData.Wifi.Enable
            ActionId.DISABLE_WIFI -> ActionData.Wifi.Disable

            ActionId.TOGGLE_BLUETOOTH -> ActionData.Bluetooth.Toggle
            ActionId.ENABLE_BLUETOOTH -> ActionData.Bluetooth.Enable
            ActionId.DISABLE_BLUETOOTH -> ActionData.Bluetooth.Disable

            ActionId.TOGGLE_MOBILE_DATA -> ActionData.MobileData.Toggle
            ActionId.ENABLE_MOBILE_DATA -> ActionData.MobileData.Enable
            ActionId.DISABLE_MOBILE_DATA -> ActionData.MobileData.Disable

            ActionId.TOGGLE_AUTO_BRIGHTNESS -> ActionData.Brightness.ToggleAuto
            ActionId.DISABLE_AUTO_BRIGHTNESS -> ActionData.Brightness.DisableAuto
            ActionId.ENABLE_AUTO_BRIGHTNESS -> ActionData.Brightness.EnableAuto
            ActionId.INCREASE_BRIGHTNESS -> ActionData.Brightness.Increase
            ActionId.DECREASE_BRIGHTNESS -> ActionData.Brightness.Decrease

            ActionId.TOGGLE_AUTO_ROTATE -> ActionData.Rotation.ToggleAuto
            ActionId.ENABLE_AUTO_ROTATE -> ActionData.Rotation.EnableAuto
            ActionId.DISABLE_AUTO_ROTATE -> ActionData.Rotation.DisableAuto
            ActionId.PORTRAIT_MODE -> ActionData.Rotation.Portrait
            ActionId.LANDSCAPE_MODE -> ActionData.Rotation.Landscape
            ActionId.SWITCH_ORIENTATION -> ActionData.Rotation.SwitchOrientation

            ActionId.VOLUME_SHOW_DIALOG -> ActionData.Volume.ShowDialog
            ActionId.CYCLE_RINGER_MODE -> ActionData.Volume.CycleRingerMode
            ActionId.CYCLE_VIBRATE_RING -> ActionData.Volume.CycleVibrateRing

            ActionId.EXPAND_NOTIFICATION_DRAWER -> ActionData.StatusBar.ExpandNotifications
            ActionId.TOGGLE_NOTIFICATION_DRAWER -> ActionData.StatusBar.ToggleNotifications
            ActionId.EXPAND_QUICK_SETTINGS -> ActionData.StatusBar.ExpandQuickSettings
            ActionId.TOGGLE_QUICK_SETTINGS -> ActionData.StatusBar.ToggleQuickSettings
            ActionId.COLLAPSE_STATUS_BAR -> ActionData.StatusBar.Collapse

            ActionId.PAUSE_MEDIA -> ActionData.ControlMedia.Pause
            ActionId.PLAY_MEDIA -> ActionData.ControlMedia.Play
            ActionId.PLAY_PAUSE_MEDIA -> ActionData.ControlMedia.PlayPause
            ActionId.NEXT_TRACK -> ActionData.ControlMedia.NextTrack
            ActionId.PREVIOUS_TRACK -> ActionData.ControlMedia.PreviousTrack
            ActionId.FAST_FORWARD -> ActionData.ControlMedia.FastForward
            ActionId.REWIND -> ActionData.ControlMedia.Rewind
            ActionId.STOP_MEDIA -> ActionData.ControlMedia.Stop
            ActionId.STEP_FORWARD -> ActionData.ControlMedia.StepForward
            ActionId.STEP_BACKWARD -> ActionData.ControlMedia.StepBackward

            ActionId.GO_BACK -> ActionData.GoBack
            ActionId.GO_HOME -> ActionData.GoHome
            ActionId.OPEN_RECENTS -> ActionData.OpenRecents
            ActionId.TOGGLE_SPLIT_SCREEN -> ActionData.ToggleSplitScreen
            ActionId.GO_LAST_APP -> ActionData.GoLastApp
            ActionId.OPEN_MENU -> ActionData.OpenMenu

            ActionId.ENABLE_NFC -> ActionData.Nfc.Enable
            ActionId.DISABLE_NFC -> ActionData.Nfc.Disable
            ActionId.TOGGLE_NFC -> ActionData.Nfc.Toggle

            ActionId.MOVE_CURSOR_TO_END -> ActionData.MoveCursorToEnd
            ActionId.TOGGLE_KEYBOARD -> ActionData.ToggleKeyboard
            ActionId.SHOW_KEYBOARD -> ActionData.ShowKeyboard
            ActionId.HIDE_KEYBOARD -> ActionData.HideKeyboard
            ActionId.SHOW_KEYBOARD_PICKER -> ActionData.ShowKeyboardPicker
            ActionId.TEXT_CUT -> ActionData.CutText
            ActionId.TEXT_COPY -> ActionData.CopyText
            ActionId.TEXT_PASTE -> ActionData.PasteText
            ActionId.SELECT_WORD_AT_CURSOR -> ActionData.SelectWordAtCursor

            ActionId.TOGGLE_AIRPLANE_MODE -> ActionData.AirplaneMode.Toggle
            ActionId.ENABLE_AIRPLANE_MODE -> ActionData.AirplaneMode.Enable
            ActionId.DISABLE_AIRPLANE_MODE -> ActionData.AirplaneMode.Disable

            ActionId.SCREENSHOT -> ActionData.Screenshot
            ActionId.OPEN_VOICE_ASSISTANT -> ActionData.VoiceAssistant
            ActionId.OPEN_DEVICE_ASSISTANT -> ActionData.DeviceAssistant

            ActionId.OPEN_CAMERA -> ActionData.OpenCamera
            ActionId.LOCK_DEVICE -> ActionData.LockDevice
            ActionId.POWER_ON_OFF_DEVICE -> ActionData.ScreenOnOff
            ActionId.SECURE_LOCK_DEVICE -> ActionData.SecureLock
            ActionId.CONSUME_KEY_EVENT -> ActionData.ConsumeKeyEvent
            ActionId.OPEN_SETTINGS -> ActionData.OpenSettings
            ActionId.SHOW_POWER_MENU -> ActionData.ShowPowerMenu
            ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> ActionData.DismissLastNotification
            ActionId.DISMISS_ALL_NOTIFICATIONS -> ActionData.DismissAllNotifications
            ActionId.ANSWER_PHONE_CALL -> ActionData.AnswerCall
            ActionId.END_PHONE_CALL -> ActionData.EndCall
            ActionId.DEVICE_CONTROLS -> ActionData.DeviceControls
            ActionId.HTTP_REQUEST -> {
                val method = entity.extras.getData(ActionEntity.EXTRA_HTTP_METHOD).then {
                    HTTP_METHOD_MAP.getKey(it)!!.success()
                }.valueOrNull() ?: return null

                val description =
                    entity.extras.getData(ActionEntity.EXTRA_HTTP_DESCRIPTION).valueOrNull()
                        ?: return null

                val url = entity.extras.getData(ActionEntity.EXTRA_HTTP_URL).valueOrNull()
                    ?: return null

                val body = entity.extras.getData(ActionEntity.EXTRA_HTTP_BODY).valueOrNull() ?: ""

                val authorizationHeader =
                    entity.extras.getData(ActionEntity.EXTRA_HTTP_AUTHORIZATION_HEADER)
                        .valueOrNull() ?: ""

                ActionData.HttpRequest(
                    description = description,
                    method = method,
                    url = url,
                    body = body,
                    authorizationHeader = authorizationHeader,
                )
            }

            ActionId.INTERACT_UI_ELEMENT -> {
                val packageName =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_PACKAGE_NAME)
                        .valueOrNull()!!

                val contentDescription =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_CONTENT_DESCRIPTION)
                        .valueOrNull()

                val text =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_TEXT).valueOrNull()

                val tooltip =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_TOOLTIP).valueOrNull()

                val hint =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_HINT).valueOrNull()

                val className =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_CLASS_NAME).valueOrNull()

                val viewResourceId =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_VIEW_RESOURCE_ID)
                        .valueOrNull()

                val uniqueId =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_UNIQUE_ID).valueOrNull()

                val actions = entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_ACTIONS).then {
                    Success(NodeInteractionTypeSetTypeConverter().toSet(it.toInt()))
                }.valueOrNull() ?: emptySet()

                val nodeAction =
                    entity.extras.getData(ActionEntity.EXTRA_ACCESSIBILITY_NODE_ACTION).then {
                        convertNodeInteractionType(it)
                    }.valueOrNull() ?: return null

                ActionData.InteractUiElement(
                    description = entity.data,
                    nodeAction = nodeAction,
                    packageName = packageName,
                    text = text,
                    contentDescription = contentDescription,
                    tooltip = tooltip,
                    hint = hint,
                    className = className,
                    viewResourceId = viewResourceId,
                    uniqueId = uniqueId,
                    nodeActions = actions,
                )
            }
        }
    }

    private fun convertNodeInteractionType(string: String): Result<NodeInteractionType> = try {
        Success(NodeInteractionType.valueOf(string))
    } catch (e: IllegalArgumentException) {
        Error.Exception(e)
    }

    fun toEntity(data: ActionData): ActionEntity {
        val type = when (data) {
            is ActionData.Intent -> ActionEntity.Type.INTENT
            is ActionData.InputKeyEvent -> ActionEntity.Type.KEY_EVENT
            is ActionData.App -> ActionEntity.Type.APP
            is ActionData.AppShortcut -> ActionEntity.Type.APP_SHORTCUT
            is ActionData.PhoneCall -> ActionEntity.Type.PHONE_CALL
            is ActionData.TapScreen -> ActionEntity.Type.TAP_COORDINATE
            is ActionData.SwipeScreen -> ActionEntity.Type.SWIPE_COORDINATE
            is ActionData.PinchScreen -> ActionEntity.Type.PINCH_COORDINATE
            is ActionData.Text -> ActionEntity.Type.TEXT_BLOCK
            is ActionData.Url -> ActionEntity.Type.URL
            is ActionData.Sound -> ActionEntity.Type.SOUND
            is ActionData.InteractUiElement -> ActionEntity.Type.INTERACT_UI_ELEMENT
            else -> ActionEntity.Type.SYSTEM_ACTION
        }

        return ActionEntity(
            type = type,
            data = getDataString(data),
            extras = getExtras(data),
            flags = getFlags(data),
        )
    }

    private fun getFlags(data: ActionData): Int {
        val showVolumeUiFlag = when (data) {
            is ActionData.Volume.Stream -> data.showVolumeUi
            is ActionData.Volume.Up -> data.showVolumeUi
            is ActionData.Volume.Down -> data.showVolumeUi
            is ActionData.Volume.Mute -> data.showVolumeUi
            is ActionData.Volume.UnMute -> data.showVolumeUi
            is ActionData.Volume.ToggleMute -> data.showVolumeUi
            else -> false
        }

        if (showVolumeUiFlag) {
            return ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI
        } else {
            return 0
        }
    }

    private fun getDataString(data: ActionData): String = when (data) {
        is ActionData.Intent -> data.uri
        is ActionData.InputKeyEvent -> data.keyCode.toString()
        is ActionData.App -> data.packageName
        is ActionData.AppShortcut -> data.uri
        is ActionData.PhoneCall -> data.number
        is ActionData.TapScreen -> "${data.x},${data.y}"
        is ActionData.SwipeScreen -> "${data.xStart},${data.yStart},${data.xEnd},${data.yEnd},${data.fingerCount},${data.duration}"
        is ActionData.PinchScreen -> "${data.x},${data.y},${data.distance},${data.pinchType},${data.fingerCount},${data.duration}"
        is ActionData.Text -> data.text
        is ActionData.Url -> data.url
        is ActionData.Sound -> when (data) {
            is ActionData.Sound.Ringtone -> data.uri
            is ActionData.Sound.SoundFile -> data.soundUid
        }

        is ActionData.InteractUiElement -> data.description
        is ActionData.ControlMediaForApp.Rewind -> SYSTEM_ACTION_ID_MAP[data.id]!!
        is ActionData.ControlMediaForApp.Stop -> SYSTEM_ACTION_ID_MAP[data.id]!!
        is ActionData.ControlMedia.Rewind -> SYSTEM_ACTION_ID_MAP[data.id]!!
        is ActionData.ControlMedia.Stop -> SYSTEM_ACTION_ID_MAP[data.id]!!
        is ActionData.GoBack -> SYSTEM_ACTION_ID_MAP[data.id]!!
        else -> SYSTEM_ACTION_ID_MAP[data.id]!!
    }

    private fun getExtras(data: ActionData): List<EntityExtra> = when (data) {
        is ActionData.Intent ->
            listOf(
                EntityExtra(ActionEntity.EXTRA_INTENT_DESCRIPTION, data.description),
                EntityExtra(ActionEntity.EXTRA_INTENT_TARGET, INTENT_TARGET_MAP[data.target]!!),
                EntityExtra(ActionEntity.EXTRA_INTENT_EXTRAS, Json.encodeToString(data.extras)),
            )

        is ActionData.InputKeyEvent -> sequence {
            if (data.useShell) {
                val string = if (data.useShell) {
                    "true"
                } else {
                    "false"
                }
                yield(EntityExtra(ActionEntity.EXTRA_KEY_EVENT_USE_SHELL, string))
            }

            if (data.metaState != 0) {
                yield(
                    EntityExtra(
                        ActionEntity.EXTRA_KEY_EVENT_META_STATE,
                        data.metaState.toString(),
                    ),
                )
            }

            if (data.device != null) {
                yield(
                    EntityExtra(
                        ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR,
                        data.device.descriptor,
                    ),
                )

                yield(
                    EntityExtra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, data.device.name),
                )
            }
        }.toList()

        is ActionData.App -> emptyList()
        is ActionData.AppShortcut -> sequence {
            yield(EntityExtra(ActionEntity.EXTRA_SHORTCUT_TITLE, data.shortcutTitle))
            data.packageName?.let { yield(EntityExtra(ActionEntity.EXTRA_PACKAGE_NAME, it)) }
        }.toList()

        is ActionData.PhoneCall -> emptyList()

        is ActionData.DoNotDisturb.Enable -> listOf(
            EntityExtra(ActionEntity.EXTRA_DND_MODE, DND_MODE_MAP[data.dndMode]!!),
        )

        is ActionData.DoNotDisturb.Toggle -> listOf(
            EntityExtra(ActionEntity.EXTRA_DND_MODE, DND_MODE_MAP[data.dndMode]!!),
        )

        is ActionData.Volume.SetRingerMode -> listOf(
            EntityExtra(ActionEntity.EXTRA_RINGER_MODE, RINGER_MODE_MAP[data.ringerMode]!!),
        )

        is ActionData.ControlMediaForApp -> listOf(
            EntityExtra(ActionEntity.EXTRA_PACKAGE_NAME, data.packageName),
        )

        is ActionData.Rotation.CycleRotations -> listOf(
            EntityExtra(
                ActionEntity.EXTRA_ORIENTATIONS,
                data.orientations.joinToString(",") { ConstantTypeConverters.ORIENTATION_MAP[it]!! },
            ),
        )

        is ActionData.Flashlight -> {
            val lensExtra = EntityExtra(ActionEntity.EXTRA_LENS, LENS_MAP[data.lens]!!)

            when (data) {
                is ActionData.Flashlight.Toggle -> buildList {
                    add(lensExtra)

                    if (data.strengthPercent != null) {
                        add(
                            EntityExtra(
                                ActionEntity.EXTRA_FLASH_STRENGTH,
                                data.strengthPercent.toString(),
                            ),
                        )
                    }
                }

                is ActionData.Flashlight.Enable -> buildList {
                    add(lensExtra)

                    if (data.strengthPercent != null) {
                        add(
                            EntityExtra(
                                ActionEntity.EXTRA_FLASH_STRENGTH,
                                data.strengthPercent.toString(),
                            ),
                        )
                    }
                }

                is ActionData.Flashlight.Disable -> listOf(lensExtra)
                is ActionData.Flashlight.ChangeStrength -> buildList {
                    add(lensExtra)
                    add(
                        EntityExtra(
                            ActionEntity.EXTRA_FLASH_STRENGTH,
                            data.percent.toString(),
                        ),
                    )
                }
            }
        }

        is ActionData.SwitchKeyboard -> listOf(
            EntityExtra(ActionEntity.EXTRA_IME_ID, data.imeId),
            EntityExtra(ActionEntity.EXTRA_IME_NAME, data.savedImeName),
        )

        is ActionData.Volume ->
            when (data) {
                is ActionData.Volume.Stream -> listOf(
                    EntityExtra(
                        ActionEntity.EXTRA_STREAM_TYPE,
                        VOLUME_STREAM_MAP[data.volumeStream]!!,
                    ),
                )

                else -> emptyList()
            }

        is ActionData.TapScreen -> sequence {
            if (!data.description.isNullOrBlank()) {
                yield(EntityExtra(ActionEntity.EXTRA_COORDINATE_DESCRIPTION, data.description))
            }
        }.toList()

        is ActionData.SwipeScreen -> sequence {
            if (!data.description.isNullOrBlank()) {
                yield(EntityExtra(ActionEntity.EXTRA_COORDINATE_DESCRIPTION, data.description))
            }
        }.toList()

        is ActionData.PinchScreen -> sequence {
            if (!data.description.isNullOrBlank()) {
                yield(EntityExtra(ActionEntity.EXTRA_COORDINATE_DESCRIPTION, data.description))
            }
        }.toList()

        is ActionData.Text -> emptyList()
        is ActionData.Url -> emptyList()

        is ActionData.Sound.SoundFile -> listOf(
            EntityExtra(ActionEntity.EXTRA_SOUND_FILE_DESCRIPTION, data.soundDescription),
        )

        is ActionData.HttpRequest -> listOf(
            EntityExtra(ActionEntity.EXTRA_HTTP_DESCRIPTION, data.description),
            EntityExtra(ActionEntity.EXTRA_HTTP_METHOD, HTTP_METHOD_MAP[data.method]!!),
            EntityExtra(ActionEntity.EXTRA_HTTP_URL, data.url),
            EntityExtra(ActionEntity.EXTRA_HTTP_BODY, data.body),
            EntityExtra(
                ActionEntity.EXTRA_HTTP_AUTHORIZATION_HEADER,
                data.authorizationHeader,
            ),
        )

        is ActionData.InteractUiElement -> buildList {
            add(
                EntityExtra(
                    ActionEntity.EXTRA_ACCESSIBILITY_NODE_ACTION,
                    data.nodeAction.toString(),
                ),
            )

            add(
                EntityExtra(
                    ActionEntity.EXTRA_ACCESSIBILITY_PACKAGE_NAME,
                    data.packageName,
                ),
            )

            data.contentDescription?.let {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_ACCESSIBILITY_CONTENT_DESCRIPTION,
                        it,
                    ),
                )
            }

            data.text?.let { add(EntityExtra(ActionEntity.EXTRA_ACCESSIBILITY_TEXT, it)) }

            data.tooltip?.let { add(EntityExtra(ActionEntity.EXTRA_ACCESSIBILITY_TOOLTIP, it)) }

            data.hint?.let { add(EntityExtra(ActionEntity.EXTRA_ACCESSIBILITY_HINT, it)) }

            data.className?.let {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_ACCESSIBILITY_CLASS_NAME,
                        it,
                    ),
                )
            }

            data.viewResourceId?.let {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_ACCESSIBILITY_VIEW_RESOURCE_ID,
                        it,
                    ),
                )
            }

            data.uniqueId?.let {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_ACCESSIBILITY_UNIQUE_ID,
                        it,
                    ),
                )
            }

            if (data.nodeActions.isNotEmpty()) {
                add(
                    EntityExtra(
                        ActionEntity.EXTRA_ACCESSIBILITY_ACTIONS,
                        NodeInteractionTypeSetTypeConverter().toMask(data.nodeActions).toString(),
                    ),
                )
            }
        }

        else -> emptyList()
    }

    private val RINGER_MODE_MAP = mapOf(
        RingerMode.NORMAL to "option_ringer_mode_normal",
        RingerMode.SILENT to "option_ringer_mode_silent",
        RingerMode.VIBRATE to "option_ringer_mode_vibrate",
    )

    private val LENS_MAP = mapOf(
        CameraLens.BACK to "option_lens_back",
        CameraLens.FRONT to "option_lens_front",
    )

    private val VOLUME_STREAM_MAP = mapOf(
        VolumeStream.ACCESSIBILITY to "option_stream_accessibility",
        VolumeStream.ALARM to "option_stream_alarm",
        VolumeStream.DTMF to "option_stream_dtmf",
        VolumeStream.MUSIC to "option_stream_music",
        VolumeStream.NOTIFICATION to "option_stream_notification",
        VolumeStream.RING to "option_stream_ring",
        VolumeStream.SYSTEM to "option_stream_system",
        VolumeStream.VOICE_CALL to "option_stream_voice_call",
    )

    private val DND_MODE_MAP = mapOf(
        DndMode.ALARMS to "do_not_disturb_alarms",
        DndMode.PRIORITY to "do_not_disturb_priority",
        DndMode.NONE to "do_not_disturb_none",
    )

    private val INTENT_TARGET_MAP = mapOf(
        IntentTarget.ACTIVITY to "ACTIVITY",
        IntentTarget.BROADCAST_RECEIVER to "BROADCAST_RECEIVER",
        IntentTarget.SERVICE to "SERVICE",
    )

    private val HTTP_METHOD_MAP = mapOf(
        HttpMethod.GET to "GET",
        HttpMethod.POST to "POST",
        HttpMethod.PUT to "PUT",
        HttpMethod.DELETE to "DELETE",
        HttpMethod.PATCH to "PATCH",
    )

    /**
     * DON'T CHANGE THESE
     */
    private val SYSTEM_ACTION_ID_MAP = mapOf(
        ActionId.TOGGLE_WIFI to "toggle_wifi",
        ActionId.ENABLE_WIFI to "enable_wifi",
        ActionId.DISABLE_WIFI to "disable_wifi",

        ActionId.TOGGLE_BLUETOOTH to "toggle_bluetooth",
        ActionId.ENABLE_BLUETOOTH to "enable_bluetooth",
        ActionId.DISABLE_BLUETOOTH to "disable_bluetooth",

        ActionId.TOGGLE_MOBILE_DATA to "toggle_mobile_data",
        ActionId.ENABLE_MOBILE_DATA to "enable_mobile_data",
        ActionId.DISABLE_MOBILE_DATA to "disable_mobile_data",

        ActionId.TOGGLE_AUTO_BRIGHTNESS to "toggle_auto_brightness",
        ActionId.DISABLE_AUTO_BRIGHTNESS to "disable_auto_brightness",
        ActionId.ENABLE_AUTO_BRIGHTNESS to "enable_auto_brightness",
        ActionId.INCREASE_BRIGHTNESS to "increase_brightness",
        ActionId.DECREASE_BRIGHTNESS to "decrease_brightness",

        ActionId.TOGGLE_AUTO_ROTATE to "toggle_auto_rotate",
        ActionId.ENABLE_AUTO_ROTATE to "enable_auto_rotate",
        ActionId.DISABLE_AUTO_ROTATE to "disable_auto_rotate",
        ActionId.PORTRAIT_MODE to "portrait_mode",
        ActionId.LANDSCAPE_MODE to "landscape_mode",
        ActionId.SWITCH_ORIENTATION to "switch_orientation",
        ActionId.CYCLE_ROTATIONS to "cycle_rotations",

        ActionId.VOLUME_UP to "volume_up",
        ActionId.VOLUME_DOWN to "volume_down",
        ActionId.VOLUME_SHOW_DIALOG to "volume_show_dialog",
        ActionId.VOLUME_DECREASE_STREAM to "volume_decrease_stream",
        ActionId.VOLUME_INCREASE_STREAM to "volume_increase_stream",
        ActionId.CYCLE_RINGER_MODE to "ringer_mode_cycle",
        ActionId.CHANGE_RINGER_MODE to "ringer_mode_change",
        ActionId.CYCLE_VIBRATE_RING to "ringer_mode_cycle_vibrate_ring",
        ActionId.TOGGLE_DND_MODE to "toggle_do_not_disturb_mode",
        ActionId.ENABLE_DND_MODE to "set_do_not_disturb_mode",
        ActionId.DISABLE_DND_MODE to "disable_do_not_disturb_mode",
        ActionId.VOLUME_UNMUTE to "volume_unmute",
        ActionId.VOLUME_MUTE to "volume_mute",
        ActionId.VOLUME_TOGGLE_MUTE to "volume_toggle_mute",

        ActionId.EXPAND_NOTIFICATION_DRAWER to "expand_notification_drawer",
        ActionId.TOGGLE_NOTIFICATION_DRAWER to "toggle_notification_drawer",
        ActionId.EXPAND_QUICK_SETTINGS to "expand_quick_settings",
        ActionId.TOGGLE_QUICK_SETTINGS to "toggle_quick_settings_drawer",
        ActionId.COLLAPSE_STATUS_BAR to "collapse_status_bar",

        ActionId.PAUSE_MEDIA to "pause_media",
        ActionId.PAUSE_MEDIA_PACKAGE to "pause_media_package",
        ActionId.PLAY_MEDIA to "play_media",
        ActionId.PLAY_MEDIA_PACKAGE to "play_media_package",
        ActionId.PLAY_PAUSE_MEDIA to "play_pause_media",
        ActionId.PLAY_PAUSE_MEDIA_PACKAGE to "play_pause_media_package",
        ActionId.NEXT_TRACK to "next_track",
        ActionId.NEXT_TRACK_PACKAGE to "next_track_package",
        ActionId.PREVIOUS_TRACK to "previous_track",
        ActionId.PREVIOUS_TRACK_PACKAGE to "previous_track_package",
        ActionId.FAST_FORWARD to "fast_forward",
        ActionId.FAST_FORWARD_PACKAGE to "fast_forward_package",
        ActionId.REWIND to "rewind",
        ActionId.REWIND_PACKAGE to "rewind_package",
        ActionId.STOP_MEDIA to "stop_media",
        ActionId.STOP_MEDIA_PACKAGE to "stop_media_package",
        ActionId.STEP_FORWARD to "step_forward",
        ActionId.STEP_FORWARD_PACKAGE to "step_forward_package",
        ActionId.STEP_BACKWARD to "step_backward",
        ActionId.STEP_BACKWARD_PACKAGE to "step_backward_package",

        ActionId.GO_BACK to "go_back",
        ActionId.GO_HOME to "go_home",
        ActionId.OPEN_RECENTS to "open_recents",
        ActionId.TOGGLE_SPLIT_SCREEN to "toggle_split_screen",
        ActionId.GO_LAST_APP to "go_last_app",
        ActionId.OPEN_MENU to "open_menu",

        ActionId.TOGGLE_FLASHLIGHT to "toggle_flashlight",
        ActionId.ENABLE_FLASHLIGHT to "enable_flashlight",
        ActionId.DISABLE_FLASHLIGHT to "disable_flashlight",
        ActionId.CHANGE_FLASHLIGHT_STRENGTH to "change_flashlight_strength",

        ActionId.ENABLE_NFC to "nfc_enable",
        ActionId.DISABLE_NFC to "nfc_disable",
        ActionId.TOGGLE_NFC to "nfc_toggle",

        ActionId.MOVE_CURSOR_TO_END to "move_cursor_to_end",
        ActionId.TOGGLE_KEYBOARD to "toggle_keyboard",
        ActionId.SHOW_KEYBOARD to "show_keyboard",
        ActionId.HIDE_KEYBOARD to "hide_keyboard",
        ActionId.SHOW_KEYBOARD_PICKER to "show_keyboard_picker",
        ActionId.TEXT_CUT to "text_cut",
        ActionId.TEXT_COPY to "text_copy",
        ActionId.TEXT_PASTE to "text_paste",
        ActionId.SELECT_WORD_AT_CURSOR to "select_word_at_cursor",

        ActionId.SWITCH_KEYBOARD to "switch_keyboard",

        ActionId.TOGGLE_AIRPLANE_MODE to "toggle_airplane_mode",
        ActionId.ENABLE_AIRPLANE_MODE to "enable_airplane_mode",
        ActionId.DISABLE_AIRPLANE_MODE to "disable_airplane_mode",

        ActionId.SCREENSHOT to "screenshot",
        ActionId.OPEN_VOICE_ASSISTANT to "open_assistant",
        ActionId.OPEN_DEVICE_ASSISTANT to "open_device_assistant",
        ActionId.OPEN_CAMERA to "open_camera",
        ActionId.LOCK_DEVICE to "lock_device",
        ActionId.POWER_ON_OFF_DEVICE to "power_on_off_device",
        ActionId.SECURE_LOCK_DEVICE to "secure_lock_device",
        ActionId.CONSUME_KEY_EVENT to "consume_key_event",
        ActionId.OPEN_SETTINGS to "open_settings",
        ActionId.SHOW_POWER_MENU to "show_power_menu",

        ActionId.DISMISS_MOST_RECENT_NOTIFICATION to "dismiss_most_recent_notification",
        ActionId.DISMISS_ALL_NOTIFICATIONS to "dismiss_all_notifications",

        ActionId.ANSWER_PHONE_CALL to "answer_phone_call",
        ActionId.END_PHONE_CALL to "end_phone_call",
        ActionId.DEVICE_CONTROLS to "device_controls",
        ActionId.HTTP_REQUEST to "http_request",
    )
}
