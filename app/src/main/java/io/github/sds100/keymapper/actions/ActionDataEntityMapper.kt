package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.intents.IntentTarget
import io.github.sds100.keymapper.system.volume.DndMode
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.util.getKey
import io.github.sds100.keymapper.util.success
import io.github.sds100.keymapper.util.then
import io.github.sds100.keymapper.util.valueOrNull
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 13/03/2021.
 */

object ActionDataEntityMapper {

    fun fromEntity(entity: ActionEntity): ActionData? {
        val actionId = when (entity.type) {
            ActionEntity.Type.APP -> ActionId.APP
            ActionEntity.Type.APP_SHORTCUT -> ActionId.APP_SHORTCUT
            ActionEntity.Type.KEY_EVENT -> ActionId.KEY_EVENT
            ActionEntity.Type.TEXT_BLOCK -> ActionId.TEXT
            ActionEntity.Type.URL -> ActionId.URL
            ActionEntity.Type.TAP_COORDINATE -> ActionId.TAP_SCREEN
            ActionEntity.Type.INTENT -> ActionId.INTENT
            ActionEntity.Type.PHONE_CALL -> ActionId.PHONE_CALL
            ActionEntity.Type.SOUND -> ActionId.SOUND
            ActionEntity.Type.SYSTEM_ACTION -> {
                SYSTEM_ACTION_ID_MAP.getKey(entity.data) ?: return null
            }
        }

        return when (actionId) {
            ActionId.APP -> OpenAppAction(packageName = entity.data)

            ActionId.APP_SHORTCUT -> {
                val packageName =
                    entity.extras.getData(ActionEntity.EXTRA_PACKAGE_NAME).valueOrNull()

                val shortcutTitle =
                    entity.extras.getData(ActionEntity.EXTRA_SHORTCUT_TITLE).valueOrNull()
                        ?: return null

                OpenAppShortcutAction(
                    packageName = packageName,
                    shortcutTitle = shortcutTitle,
                    uri = entity.data
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
                    KeyEventAction.Device(deviceDescriptor, deviceName)
                } else {
                    null
                }

                KeyEventAction(
                    keyCode = entity.data.toInt(),
                    metaState = metaState,
                    useShell = useShell,
                    device = device
                )
            }

            ActionId.TEXT -> TextAction(text = entity.data)
            ActionId.URL -> UrlAction(url = entity.data)
            ActionId.TAP_SCREEN -> {
                val x = entity.data.split(',')[0].toInt()
                val y = entity.data.split(',')[1].toInt()
                val description = entity.extras.getData(ActionEntity.EXTRA_COORDINATE_DESCRIPTION)
                    .valueOrNull()

                TapCoordinateAction(x = x, y = y, description = description)
            }

            ActionId.INTENT -> {
                val target = entity.extras.getData(ActionEntity.EXTRA_INTENT_TARGET).then {
                    INTENT_TARGET_MAP.getKey(it).success()
                }.valueOrNull() ?: return null

                val description =
                    entity.extras.getData(ActionEntity.EXTRA_INTENT_DESCRIPTION).valueOrNull()
                        ?: return null

                return IntentAction(
                    target = target,
                    description = description,
                    uri = entity.data
                )
            }

            ActionId.PHONE_CALL -> PhoneCallAction(number = entity.data)

            ActionId.SOUND -> {
                val soundFileDescription =
                    entity.extras.getData(ActionEntity.EXTRA_SOUND_FILE_DESCRIPTION)
                        .valueOrNull() ?: return null

                SoundAction(soundUid = entity.data, soundDescription = soundFileDescription)
            }

            ActionId.VOLUME_INCREASE_STREAM,
            ActionId.VOLUME_DECREASE_STREAM -> {
                val stream =
                    entity.extras.getData(ActionEntity.EXTRA_STREAM_TYPE).then {
                        VOLUME_STREAM_MAP.getKey(it)!!.success()
                    }.valueOrNull() ?: return null

                val showVolumeUi =
                    entity.flags.hasFlag(ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI)

                when (actionId) {
                    ActionId.VOLUME_INCREASE_STREAM ->
                        VolumeAction.Stream.Increase(showVolumeUi, stream)

                    ActionId.VOLUME_DECREASE_STREAM ->
                        VolumeAction.Stream.Decrease(showVolumeUi, stream)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.VOLUME_UP,
            ActionId.VOLUME_DOWN,
            ActionId.VOLUME_TOGGLE_MUTE,
            ActionId.VOLUME_UNMUTE,
            ActionId.VOLUME_MUTE -> {
                val showVolumeUi =
                    entity.flags.hasFlag(ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI)

                when (actionId) {
                    ActionId.VOLUME_UP -> VolumeAction.Up(showVolumeUi)
                    ActionId.VOLUME_DOWN -> VolumeAction.Down(showVolumeUi)
                    ActionId.VOLUME_TOGGLE_MUTE -> VolumeAction.ToggleMute(
                        showVolumeUi
                    )
                    ActionId.VOLUME_UNMUTE -> VolumeAction.UnMute(showVolumeUi)
                    ActionId.VOLUME_MUTE -> VolumeAction.Mute(showVolumeUi)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.TOGGLE_FLASHLIGHT,
            ActionId.ENABLE_FLASHLIGHT,
            ActionId.DISABLE_FLASHLIGHT -> {
                val lens = entity.extras.getData(ActionEntity.EXTRA_LENS).then {
                    LENS_MAP.getKey(it)!!.success()
                }.valueOrNull() ?: return null

                when (actionId) {
                    ActionId.TOGGLE_FLASHLIGHT ->
                        FlashlightAction.Toggle(lens)

                    ActionId.ENABLE_FLASHLIGHT ->
                        FlashlightAction.Enable(lens)

                    ActionId.DISABLE_FLASHLIGHT ->
                        FlashlightAction.Disable(lens)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.TOGGLE_DND_MODE,
            ActionId.ENABLE_DND_MODE -> {
                val dndMode = entity.extras.getData(ActionEntity.EXTRA_DND_MODE).then {
                    DND_MODE_MAP.getKey(it)!!.success()
                }.valueOrNull() ?: return null

                when (actionId) {
                    ActionId.TOGGLE_DND_MODE ->
                        DndModeAction.Toggle(dndMode)

                    ActionId.ENABLE_DND_MODE ->
                        DndModeAction.Enable(dndMode)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.DISABLE_DND_MODE -> {
                return DndModeAction.Disable
            }

            ActionId.PAUSE_MEDIA_PACKAGE,
            ActionId.PLAY_MEDIA_PACKAGE,
            ActionId.PLAY_PAUSE_MEDIA_PACKAGE,
            ActionId.NEXT_TRACK_PACKAGE,
            ActionId.PREVIOUS_TRACK_PACKAGE,
            ActionId.FAST_FORWARD_PACKAGE,
            ActionId.REWIND_PACKAGE -> {
                val packageName =
                    entity.extras.getData(ActionEntity.EXTRA_PACKAGE_NAME).valueOrNull()
                        ?: return null

                when (actionId) {
                    ActionId.PAUSE_MEDIA_PACKAGE ->
                        ControlMediaForAppAction.Pause(packageName)
                    ActionId.PLAY_MEDIA_PACKAGE ->
                        ControlMediaForAppAction.Play(packageName)
                    ActionId.PLAY_PAUSE_MEDIA_PACKAGE ->
                        ControlMediaForAppAction.PlayPause(packageName)
                    ActionId.NEXT_TRACK_PACKAGE ->
                        ControlMediaForAppAction.NextTrack(packageName)
                    ActionId.PREVIOUS_TRACK_PACKAGE ->
                        ControlMediaForAppAction.PreviousTrack(packageName)
                    ActionId.FAST_FORWARD_PACKAGE ->
                        ControlMediaForAppAction.FastForward(packageName)
                    ActionId.REWIND_PACKAGE ->
                        ControlMediaForAppAction.Rewind(packageName)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.CHANGE_RINGER_MODE -> {
                val ringerMode =
                    entity.extras.getData(ActionEntity.EXTRA_RINGER_MODE).then {
                        RINGER_MODE_MAP.getKey(it)!!.success()
                    }.valueOrNull() ?: return null

                VolumeAction.SetRingerMode(ringerMode)
            }

            ActionId.SWITCH_KEYBOARD -> {
                val imeId = entity.extras.getData(ActionEntity.EXTRA_IME_ID)
                    .valueOrNull() ?: return null

                val imeName = entity.extras.getData(ActionEntity.EXTRA_IME_NAME)
                    .valueOrNull() ?: return null

                SwitchKeyboardAction(imeId, imeName)
            }

            ActionId.CYCLE_ROTATIONS -> {
                val orientations =
                    entity.extras.getData(ActionEntity.EXTRA_ORIENTATIONS)
                        .then { extraValue ->
                            extraValue
                                .split(",")
                                .map { ORIENTATION_MAP.getKey(it)!! }
                                .success()
                        }.valueOrNull() ?: return null

                RotationAction.CycleRotations(orientations)
            }

            ActionId.TOGGLE_WIFI -> WifiAction.Toggle
            ActionId.ENABLE_WIFI -> WifiAction.Enable
            ActionId.DISABLE_WIFI -> WifiAction.Disable

            ActionId.TOGGLE_BLUETOOTH -> BluetoothAction.Toggle
            ActionId.ENABLE_BLUETOOTH -> BluetoothAction.Enable
            ActionId.DISABLE_BLUETOOTH -> BluetoothAction.Disable

            ActionId.TOGGLE_MOBILE_DATA -> MobileDataAction.Toggle
            ActionId.ENABLE_MOBILE_DATA -> MobileDataAction.Enable
            ActionId.DISABLE_MOBILE_DATA -> MobileDataAction.Disable

            ActionId.TOGGLE_AUTO_BRIGHTNESS -> BrightnessAction.ToggleAuto
            ActionId.DISABLE_AUTO_BRIGHTNESS -> BrightnessAction.DisableAuto
            ActionId.ENABLE_AUTO_BRIGHTNESS -> BrightnessAction.EnableAuto
            ActionId.INCREASE_BRIGHTNESS -> BrightnessAction.Increase
            ActionId.DECREASE_BRIGHTNESS -> BrightnessAction.Decrease

            ActionId.TOGGLE_AUTO_ROTATE -> RotationAction.ToggleAuto
            ActionId.ENABLE_AUTO_ROTATE -> RotationAction.EnableAuto
            ActionId.DISABLE_AUTO_ROTATE -> RotationAction.DisableAuto
            ActionId.PORTRAIT_MODE -> RotationAction.Portrait
            ActionId.LANDSCAPE_MODE -> RotationAction.Landscape
            ActionId.SWITCH_ORIENTATION -> RotationAction.SwitchOrientation

            ActionId.VOLUME_SHOW_DIALOG -> VolumeAction.ShowDialog
            ActionId.CYCLE_RINGER_MODE -> VolumeAction.CycleRingerMode
            ActionId.CYCLE_VIBRATE_RING -> VolumeAction.CycleVibrateRing

            ActionId.EXPAND_NOTIFICATION_DRAWER -> StatusBarAction.ExpandNotifications
            ActionId.TOGGLE_NOTIFICATION_DRAWER -> StatusBarAction.ToggleNotifications
            ActionId.EXPAND_QUICK_SETTINGS -> StatusBarAction.ExpandQuickSettings
            ActionId.TOGGLE_QUICK_SETTINGS -> StatusBarAction.ToggleQuickSettings
            ActionId.COLLAPSE_STATUS_BAR -> StatusBarAction.Collapse

            ActionId.PAUSE_MEDIA -> ControlMediaAction.Pause
            ActionId.PLAY_MEDIA -> ControlMediaAction.Play
            ActionId.PLAY_PAUSE_MEDIA -> ControlMediaAction.PlayPause
            ActionId.NEXT_TRACK -> ControlMediaAction.NextTrack
            ActionId.PREVIOUS_TRACK -> ControlMediaAction.PreviousTrack
            ActionId.FAST_FORWARD -> ControlMediaAction.FastForward
            ActionId.REWIND -> ControlMediaAction.Rewind

            ActionId.GO_BACK -> GoBackAction
            ActionId.GO_HOME -> GoHomeAction
            ActionId.OPEN_RECENTS -> OpenRecentsAction
            ActionId.TOGGLE_SPLIT_SCREEN -> ToggleSplitScreenAction
            ActionId.GO_LAST_APP -> GoLastAppAction
            ActionId.OPEN_MENU -> OpenMenuAction

            ActionId.ENABLE_NFC -> NfcAction.Enable
            ActionId.DISABLE_NFC -> NfcAction.Disable
            ActionId.TOGGLE_NFC -> NfcAction.Toggle

            ActionId.MOVE_CURSOR_TO_END -> MoveCursorToEndAction
            ActionId.TOGGLE_KEYBOARD -> ToggleKeyboardAction
            ActionId.SHOW_KEYBOARD -> ShowKeyboardAction
            ActionId.HIDE_KEYBOARD -> HideKeyboardAction
            ActionId.SHOW_KEYBOARD_PICKER -> ShowKeyboardPickerAction
            ActionId.TEXT_CUT -> CutTextAction
            ActionId.TEXT_COPY -> CopyTextAction
            ActionId.TEXT_PASTE -> PasteTextAction
            ActionId.SELECT_WORD_AT_CURSOR -> SelectWordAtCursorAction

            ActionId.TOGGLE_AIRPLANE_MODE -> AirplaneModeAction.Toggle
            ActionId.ENABLE_AIRPLANE_MODE -> AirplaneModeAction.Enable
            ActionId.DISABLE_AIRPLANE_MODE -> AirplaneModeAction.Disable

            ActionId.SCREENSHOT -> ScreenshotAction
            ActionId.OPEN_VOICE_ASSISTANT -> VoiceAssistantAction
            ActionId.OPEN_DEVICE_ASSISTANT -> DeviceAssistantAction

            ActionId.OPEN_CAMERA -> OpenCameraAction
            ActionId.LOCK_DEVICE -> LockDeviceAction
            ActionId.POWER_ON_OFF_DEVICE -> ScreenOnOffAction
            ActionId.SECURE_LOCK_DEVICE -> SecureLockAction
            ActionId.CONSUME_KEY_EVENT -> ConsumeKeyEventAction
            ActionId.OPEN_SETTINGS -> OpenSettingsAction
            ActionId.SHOW_POWER_MENU -> ShowPowerMenuAction
        }
    }

    fun toEntity(data: ActionData): ActionEntity {
        val type = when (data) {
            is IntentAction -> ActionEntity.Type.INTENT
            is KeyEventAction -> ActionEntity.Type.KEY_EVENT
            is OpenAppAction -> ActionEntity.Type.APP
            is OpenAppShortcutAction -> ActionEntity.Type.APP_SHORTCUT
            is PhoneCallAction -> ActionEntity.Type.PHONE_CALL
            is TapCoordinateAction -> ActionEntity.Type.TAP_COORDINATE
            is TextAction -> ActionEntity.Type.TEXT_BLOCK
            is UrlAction -> ActionEntity.Type.URL
            is SoundAction -> ActionEntity.Type.SOUND
            else -> ActionEntity.Type.SYSTEM_ACTION
        }

        return ActionEntity(
            type = type,
            data = getDataString(data),
            extras = getExtras(data),
            flags = getFlags(data)
        )
    }

    private fun getFlags(data: ActionData): Int {
        val showVolumeUiFlag = when (data) {
            is VolumeAction.Stream -> data.showVolumeUi
            is VolumeAction.Up -> data.showVolumeUi
            is VolumeAction.Down -> data.showVolumeUi
            is VolumeAction.Mute -> data.showVolumeUi
            is VolumeAction.UnMute -> data.showVolumeUi
            is VolumeAction.ToggleMute -> data.showVolumeUi
            else -> false
        }

        if (showVolumeUiFlag) {
            return ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI
        } else {
            return 0
        }
    }

    private fun getDataString(data: ActionData): String = when (data) {
        is IntentAction -> data.uri
        is KeyEventAction -> data.keyCode.toString()
        is OpenAppAction -> data.packageName
        is OpenAppShortcutAction -> data.uri
        is PhoneCallAction -> data.number
        is TapCoordinateAction -> "${data.x},${data.y}"
        is TextAction -> data.text
        is UrlAction -> data.url
        is SoundAction -> data.soundUid
        else -> SYSTEM_ACTION_ID_MAP[data.id]!!
    }

    private fun getExtras(data: ActionData): List<Extra> = when (data) {
        is IntentAction ->
            listOf(
                Extra(ActionEntity.EXTRA_INTENT_DESCRIPTION, data.description),
                Extra(ActionEntity.EXTRA_INTENT_TARGET, INTENT_TARGET_MAP[data.target]!!)
            )

        is KeyEventAction -> sequence {
            if (data.useShell) {
                val string = if (data.useShell) {
                    "true"
                } else {
                    "false"
                }
                yield(Extra(ActionEntity.EXTRA_KEY_EVENT_USE_SHELL, string))
            }

            if (data.metaState != 0) {
                yield(Extra(ActionEntity.EXTRA_KEY_EVENT_META_STATE, data.metaState.toString()))
            }

            if (data.device != null) {
                yield(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, data.device.descriptor)
                )

                yield(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, data.device.name)
                )
            }
        }.toList()

        is OpenAppAction -> emptyList()
        is OpenAppShortcutAction -> sequence {
            yield(Extra(ActionEntity.EXTRA_SHORTCUT_TITLE, data.shortcutTitle))
            data.packageName?.let { yield(Extra(ActionEntity.EXTRA_PACKAGE_NAME, it)) }
        }.toList()

        is PhoneCallAction -> emptyList()

        is DndModeAction.Enable -> listOf(
            Extra(ActionEntity.EXTRA_DND_MODE, DND_MODE_MAP[data.dndMode]!!)
        )

        is DndModeAction.Toggle -> listOf(
            Extra(ActionEntity.EXTRA_DND_MODE, DND_MODE_MAP[data.dndMode]!!)
        )

        is VolumeAction.SetRingerMode -> listOf(
            Extra(ActionEntity.EXTRA_RINGER_MODE, RINGER_MODE_MAP[data.ringerMode]!!)
        )

        is ControlMediaForAppAction -> listOf(
            Extra(ActionEntity.EXTRA_PACKAGE_NAME, data.packageName)
        )

        is RotationAction.CycleRotations -> listOf(
            Extra(
                ActionEntity.EXTRA_ORIENTATIONS,
                data.orientations.joinToString(",") { ORIENTATION_MAP[it]!! })
        )

        is FlashlightAction -> listOf(
            Extra(ActionEntity.EXTRA_LENS, LENS_MAP[data.lens]!!)
        )

        is SwitchKeyboardAction -> listOf(
            Extra(ActionEntity.EXTRA_IME_ID, data.imeId),
            Extra(ActionEntity.EXTRA_IME_NAME, data.savedImeName)
        )

        is VolumeAction ->
            when (data) {
                is VolumeAction.Stream -> listOf(
                    Extra(ActionEntity.EXTRA_STREAM_TYPE, VOLUME_STREAM_MAP[data.volumeStream]!!)
                )

                else -> emptyList()
            }
        is TapCoordinateAction -> sequence {
            if (!data.description.isNullOrBlank()) {
                yield(Extra(ActionEntity.EXTRA_COORDINATE_DESCRIPTION, data.description))
            }
        }.toList()

        is TextAction -> emptyList()
        is UrlAction -> emptyList()

        is SoundAction -> listOf(
            Extra(ActionEntity.EXTRA_SOUND_FILE_DESCRIPTION, data.soundDescription),
        )

        else -> emptyList()
    }

    private val ORIENTATION_MAP = mapOf(
        Orientation.ORIENTATION_0 to "rotation_0",
        Orientation.ORIENTATION_90 to "rotation_90",
        Orientation.ORIENTATION_180 to "rotation_180",
        Orientation.ORIENTATION_270 to "rotation_270",
    )

    private val RINGER_MODE_MAP = mapOf(
        RingerMode.NORMAL to "option_ringer_mode_normal",
        RingerMode.SILENT to "option_ringer_mode_silent",
        RingerMode.VIBRATE to "option_ringer_mode_vibrate"
    )

    private val LENS_MAP = mapOf(
        CameraLens.BACK to "option_lens_back",
        CameraLens.FRONT to "option_lens_front"
    )

    private val VOLUME_STREAM_MAP = mapOf(
        VolumeStream.ACCESSIBILITY to "option_stream_accessibility",
        VolumeStream.ALARM to "option_stream_alarm",
        VolumeStream.DTMF to "option_stream_dtmf",
        VolumeStream.MUSIC to "option_stream_music",
        VolumeStream.NOTIFICATION to "option_stream_notification",
        VolumeStream.RING to "option_stream_ring",
        VolumeStream.SYSTEM to "option_stream_system",
        VolumeStream.VOICE_CALL to "option_stream_voice_call"
    )

    private val DND_MODE_MAP = mapOf(
        DndMode.ALARMS to "do_not_disturb_alarms",
        DndMode.PRIORITY to "do_not_disturb_priority",
        DndMode.NONE to "do_not_disturb_none"
    )

    private val INTENT_TARGET_MAP = mapOf(
        IntentTarget.ACTIVITY to "ACTIVITY",
        IntentTarget.BROADCAST_RECEIVER to "BROADCAST_RECEIVER",
        IntentTarget.SERVICE to "SERVICE"
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

        ActionId.GO_BACK to "go_back",
        ActionId.GO_HOME to "go_home",
        ActionId.OPEN_RECENTS to "open_recents",
        ActionId.TOGGLE_SPLIT_SCREEN to "toggle_split_screen",
        ActionId.GO_LAST_APP to "go_last_app",
        ActionId.OPEN_MENU to "open_menu",

        ActionId.TOGGLE_FLASHLIGHT to "toggle_flashlight",
        ActionId.ENABLE_FLASHLIGHT to "enable_flashlight",
        ActionId.DISABLE_FLASHLIGHT to "disable_flashlight",

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
    )
}