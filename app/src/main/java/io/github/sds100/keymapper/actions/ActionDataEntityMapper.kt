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
        val actionId: ActionId = when (entity.type) {
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
                SYSTEM_ACTION_ID_MAP[entity.data] ?: return null
            }
        }

        when (actionId) {
            ActionId.APP -> return ActionData.App(packageName = entity.data)

            ActionId.APP_SHORTCUT -> {
                val packageName =
                    entity.extras.getData(ActionEntity.EXTRA_PACKAGE_NAME).valueOrNull()

                val shortcutTitle =
                    entity.extras.getData(ActionEntity.EXTRA_SHORTCUT_TITLE).valueOrNull()
                        ?: return null

                return ActionData.AppShortcut(
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
                    ActionData.InputKeyEvent.Device(deviceDescriptor, deviceName)
                } else {
                    null
                }

                return ActionData.InputKeyEvent(
                    keyCode = entity.data.toInt(),
                    metaState = metaState,
                    useShell = useShell,
                    device = device
                )
            }

            ActionId.TEXT -> return ActionData.Text(text = entity.data)
            ActionId.URL -> return ActionData.Url(url = entity.data)
            ActionId.TAP_SCREEN -> {
                val x = entity.data.split(',')[0].toInt()
                val y = entity.data.split(',')[1].toInt()
                val description = entity.extras.getData(ActionEntity.EXTRA_COORDINATE_DESCRIPTION)
                    .valueOrNull()

                return ActionData.TapScreen(x = x, y = y, description = description)
            }

            ActionId.INTENT -> {
                val target = entity.extras.getData(ActionEntity.EXTRA_INTENT_TARGET).then {
                    INTENT_TARGET_MAP.getKey(it).success()
                }.valueOrNull() ?: return null

                val description =
                    entity.extras.getData(ActionEntity.EXTRA_INTENT_DESCRIPTION).valueOrNull()
                        ?: return null

                return ActionData.Intent(
                    target = target,
                    description = description,
                    uri = entity.data
                )
            }

            ActionId.PHONE_CALL -> return ActionData.PhoneCall(number = entity.data)

            ActionId.SOUND -> {
                val soundFileDescription =
                    entity.extras.getData(ActionEntity.EXTRA_SOUND_FILE_DESCRIPTION)
                        .valueOrNull() ?: return null

                return ActionData.Sound(soundUid = entity.data, soundDescription = soundFileDescription)
            }

            ActionId.VOLUME_UP,
            ActionId.VOLUME_DOWN,
            ActionId.VOLUME_TOGGLE_MUTE,
            ActionId.VOLUME_UNMUTE,
            ActionId.VOLUME_MUTE -> {
                val stream: VolumeStream =
                    entity.extras.getData(ActionEntity.EXTRA_STREAM_TYPE).then {
                        VOLUME_STREAM_MAP.getKey(it)!!.success()
                    }.valueOrNull() ?: VolumeStream.DEFAULT

                val showVolumeUi =
                    entity.flags.hasFlag(ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI)

                return when (actionId) {
                    ActionId.VOLUME_UP -> ActionData.Volume.Up(showVolumeUi, stream)
                    ActionId.VOLUME_DOWN -> ActionData.Volume.Down(showVolumeUi, stream)
                    ActionId.VOLUME_TOGGLE_MUTE -> ActionData.Volume.ToggleMute(showVolumeUi, stream)
                    ActionId.VOLUME_UNMUTE -> ActionData.Volume.UnMute(showVolumeUi, stream)
                    ActionId.VOLUME_MUTE -> ActionData.Volume.Mute(showVolumeUi, stream)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.TOGGLE_FLASHLIGHT,
            ActionId.ENABLE_FLASHLIGHT,
            ActionId.DISABLE_FLASHLIGHT -> {
                val lens = entity.extras.getData(ActionEntity.EXTRA_LENS).then {
                    LENS_MAP.getKey(it)!!.success()
                }.valueOrNull() ?: return null

                return when (actionId) {
                    ActionId.TOGGLE_FLASHLIGHT ->
                        ActionData.Flashlight.Toggle(lens)

                    ActionId.ENABLE_FLASHLIGHT ->
                        ActionData.Flashlight.Enable(lens)

                    ActionId.DISABLE_FLASHLIGHT ->
                        ActionData.Flashlight.Disable(lens)

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.TOGGLE_DND_MODE,
            ActionId.ENABLE_DND_MODE -> {
                val dndMode = entity.extras.getData(ActionEntity.EXTRA_DND_MODE).then {
                    DND_MODE_MAP.getKey(it)!!.success()
                }.valueOrNull() ?: return null

                return when (actionId) {
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
            ActionId.REWIND_PACKAGE -> {
                val packageName =
                    entity.extras.getData(ActionEntity.EXTRA_PACKAGE_NAME).valueOrNull()
                        ?: return null

                return when (actionId) {
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

                    else -> throw Exception("don't know how to create system action for $actionId")
                }
            }

            ActionId.CHANGE_RINGER_MODE -> {
                val ringerMode =
                    entity.extras.getData(ActionEntity.EXTRA_RINGER_MODE).then {
                        RINGER_MODE_MAP.getKey(it)!!.success()
                    }.valueOrNull() ?: return null

                return ActionData.SetRingerMode(ringerMode)
            }

            ActionId.SWITCH_KEYBOARD -> {
                val imeId = entity.extras.getData(ActionEntity.EXTRA_IME_ID)
                    .valueOrNull() ?: return null

                val imeName = entity.extras.getData(ActionEntity.EXTRA_IME_NAME)
                    .valueOrNull() ?: return null

                return ActionData.SwitchKeyboard(imeId, imeName)
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

                return ActionData.Rotation.CycleRotations(orientations)
            }

            ActionId.TOGGLE_WIFI -> return ActionData.Wifi.Toggle
            ActionId.ENABLE_WIFI -> return ActionData.Wifi.Enable
            ActionId.DISABLE_WIFI -> return ActionData.Wifi.Disable

            ActionId.TOGGLE_BLUETOOTH -> return ActionData.Bluetooth.Toggle
            ActionId.ENABLE_BLUETOOTH -> return ActionData.Bluetooth.Enable
            ActionId.DISABLE_BLUETOOTH -> return ActionData.Bluetooth.Disable

            ActionId.TOGGLE_MOBILE_DATA -> return ActionData.MobileData.Toggle
            ActionId.ENABLE_MOBILE_DATA -> return ActionData.MobileData.Enable
            ActionId.DISABLE_MOBILE_DATA -> return ActionData.MobileData.Disable

            ActionId.TOGGLE_AUTO_BRIGHTNESS -> return ActionData.Brightness.ToggleAuto
            ActionId.DISABLE_AUTO_BRIGHTNESS -> return ActionData.Brightness.DisableAuto
            ActionId.ENABLE_AUTO_BRIGHTNESS -> return ActionData.Brightness.EnableAuto
            ActionId.INCREASE_BRIGHTNESS -> return ActionData.Brightness.Increase
            ActionId.DECREASE_BRIGHTNESS -> return ActionData.Brightness.Decrease

            ActionId.TOGGLE_AUTO_ROTATE -> return ActionData.Rotation.ToggleAuto
            ActionId.ENABLE_AUTO_ROTATE -> return ActionData.Rotation.EnableAuto
            ActionId.DISABLE_AUTO_ROTATE -> return ActionData.Rotation.DisableAuto
            ActionId.PORTRAIT_MODE -> return ActionData.Rotation.Portrait
            ActionId.LANDSCAPE_MODE -> return ActionData.Rotation.Landscape
            ActionId.SWITCH_ORIENTATION -> return ActionData.Rotation.SwitchOrientation

            ActionId.VOLUME_SHOW_DIALOG -> return ActionData.ShowVolumeDialog
            ActionId.CYCLE_RINGER_MODE -> return ActionData.CycleRingerMode
            ActionId.CYCLE_VIBRATE_RING -> return ActionData.CycleVibrateRing

            ActionId.EXPAND_NOTIFICATION_DRAWER -> return ActionData.StatusBar.ExpandNotifications
            ActionId.TOGGLE_NOTIFICATION_DRAWER -> return ActionData.StatusBar.ToggleNotifications
            ActionId.EXPAND_QUICK_SETTINGS -> return ActionData.StatusBar.ExpandQuickSettings
            ActionId.TOGGLE_QUICK_SETTINGS -> return ActionData.StatusBar.ToggleQuickSettings
            ActionId.COLLAPSE_STATUS_BAR -> return ActionData.StatusBar.Collapse

            ActionId.PAUSE_MEDIA -> return ActionData.ControlMedia.Pause
            ActionId.PLAY_MEDIA -> return ActionData.ControlMedia.Play
            ActionId.PLAY_PAUSE_MEDIA -> return ActionData.ControlMedia.PlayPause
            ActionId.NEXT_TRACK -> return ActionData.ControlMedia.NextTrack
            ActionId.PREVIOUS_TRACK -> return ActionData.ControlMedia.PreviousTrack
            ActionId.FAST_FORWARD -> return ActionData.ControlMedia.FastForward
            ActionId.REWIND -> return ActionData.ControlMedia.Rewind

            ActionId.GO_BACK -> return ActionData.GoBack
            ActionId.GO_HOME -> return ActionData.GoHome
            ActionId.OPEN_RECENTS -> return ActionData.OpenRecents
            ActionId.TOGGLE_SPLIT_SCREEN -> return ActionData.ToggleSplitScreen
            ActionId.GO_LAST_APP -> return ActionData.GoLastApp
            ActionId.OPEN_MENU -> return ActionData.OpenMenu

            ActionId.ENABLE_NFC -> return ActionData.Nfc.Enable
            ActionId.DISABLE_NFC -> return ActionData.Nfc.Disable
            ActionId.TOGGLE_NFC -> return ActionData.Nfc.Toggle

            ActionId.MOVE_CURSOR_TO_END -> return ActionData.MoveCursorToEnd
            ActionId.TOGGLE_KEYBOARD -> return ActionData.ToggleKeyboard
            ActionId.SHOW_KEYBOARD -> return ActionData.ShowKeyboard
            ActionId.HIDE_KEYBOARD -> return ActionData.HideKeyboard
            ActionId.SHOW_KEYBOARD_PICKER -> return ActionData.ShowKeyboardPicker
            ActionId.TEXT_CUT -> return ActionData.CutText
            ActionId.TEXT_COPY -> return ActionData.CopyText
            ActionId.TEXT_PASTE -> return ActionData.PasteText
            ActionId.SELECT_WORD_AT_CURSOR -> return ActionData.SelectWordAtCursor

            ActionId.TOGGLE_AIRPLANE_MODE -> return ActionData.AirplaneMode.Toggle
            ActionId.ENABLE_AIRPLANE_MODE -> return ActionData.AirplaneMode.Enable
            ActionId.DISABLE_AIRPLANE_MODE -> return ActionData.AirplaneMode.Disable

            ActionId.SCREENSHOT -> return ActionData.Screenshot
            ActionId.OPEN_VOICE_ASSISTANT -> return ActionData.VoiceAssistant
            ActionId.OPEN_DEVICE_ASSISTANT -> return ActionData.DeviceAssistant

            ActionId.OPEN_CAMERA -> return ActionData.OpenCamera
            ActionId.LOCK_DEVICE -> return ActionData.LockDevice
            ActionId.POWER_ON_OFF_DEVICE -> return ActionData.ScreenOnOff
            ActionId.SECURE_LOCK_DEVICE -> return ActionData.SecureLock
            ActionId.CONSUME_KEY_EVENT -> return ActionData.ConsumeKeyEvent
            ActionId.OPEN_SETTINGS -> return ActionData.OpenSettings
            ActionId.SHOW_POWER_MENU -> return ActionData.ShowPowerMenu
            ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> return ActionData.DismissLastNotification
            ActionId.DISMISS_ALL_NOTIFICATIONS -> return ActionData.DismissAllNotifications
            ActionId.ANSWER_PHONE_CALL -> return ActionData.AnswerCall
            ActionId.END_PHONE_CALL -> return ActionData.EndCall
        }
    }

    fun toEntity(data: ActionData): ActionEntity {
        val type = when (data) {
            is ActionData.Intent -> ActionEntity.Type.INTENT
            is ActionData.InputKeyEvent -> ActionEntity.Type.KEY_EVENT
            is ActionData.App -> ActionEntity.Type.APP
            is ActionData.AppShortcut -> ActionEntity.Type.APP_SHORTCUT
            is ActionData.PhoneCall -> ActionEntity.Type.PHONE_CALL
            is ActionData.TapScreen -> ActionEntity.Type.TAP_COORDINATE
            is ActionData.Text -> ActionEntity.Type.TEXT_BLOCK
            is ActionData.Url -> ActionEntity.Type.URL
            is ActionData.Sound -> ActionEntity.Type.SOUND
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
            is ActionData.Volume -> data.showVolumeUi
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
        is ActionData.Text -> data.text
        is ActionData.Url -> data.url
        is ActionData.Sound -> data.soundUid
        else -> SYSTEM_ACTION_ID_MAP.getKey(data.id)!!
    }

    private fun getExtras(data: ActionData): List<Extra> = when (data) {
        is ActionData.Intent ->
            listOf(
                Extra(ActionEntity.EXTRA_INTENT_DESCRIPTION, data.description),
                Extra(ActionEntity.EXTRA_INTENT_TARGET, INTENT_TARGET_MAP[data.target]!!)
            )

        is ActionData.InputKeyEvent -> sequence {
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

        is ActionData.App -> emptyList()
        is ActionData.AppShortcut -> sequence {
            yield(Extra(ActionEntity.EXTRA_SHORTCUT_TITLE, data.shortcutTitle))
            data.packageName?.let { yield(Extra(ActionEntity.EXTRA_PACKAGE_NAME, it)) }
        }.toList()

        is ActionData.PhoneCall -> emptyList()

        is ActionData.DoNotDisturb.Enable -> listOf(
            Extra(ActionEntity.EXTRA_DND_MODE, DND_MODE_MAP[data.dndMode]!!)
        )

        is ActionData.DoNotDisturb.Toggle -> listOf(
            Extra(ActionEntity.EXTRA_DND_MODE, DND_MODE_MAP[data.dndMode]!!)
        )

        is ActionData.SetRingerMode -> listOf(
            Extra(ActionEntity.EXTRA_RINGER_MODE, RINGER_MODE_MAP[data.ringerMode]!!)
        )

        is ActionData.ControlMediaForApp -> listOf(
            Extra(ActionEntity.EXTRA_PACKAGE_NAME, data.packageName)
        )

        is ActionData.Rotation.CycleRotations -> listOf(
            Extra(
                ActionEntity.EXTRA_ORIENTATIONS,
                data.orientations.joinToString(",") { ORIENTATION_MAP[it]!! })
        )

        is ActionData.Flashlight -> listOf(
            Extra(ActionEntity.EXTRA_LENS, LENS_MAP[data.lens]!!)
        )

        is ActionData.SwitchKeyboard -> listOf(
            Extra(ActionEntity.EXTRA_IME_ID, data.imeId),
            Extra(ActionEntity.EXTRA_IME_NAME, data.savedImeName)
        )

        is ActionData.Volume -> {
            //only save stream if it isn't the default
            if (data.volumeStream != VolumeStream.DEFAULT) {
                listOf(Extra(ActionEntity.EXTRA_STREAM_TYPE, VOLUME_STREAM_MAP[data.volumeStream]!!))
            } else {
                emptyList()
            }
        }

        is ActionData.TapScreen -> sequence {
            if (!data.description.isNullOrBlank()) {
                yield(Extra(ActionEntity.EXTRA_COORDINATE_DESCRIPTION, data.description))
            }
        }.toList()

        is ActionData.Text -> emptyList()
        is ActionData.Url -> emptyList()

        is ActionData.Sound -> listOf(
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
    private val SYSTEM_ACTION_ID_MAP: Map<String, ActionId> = mapOf(
        "toggle_wifi" to ActionId.TOGGLE_WIFI,
        "enable_wifi" to ActionId.ENABLE_WIFI,
        "disable_wifi" to ActionId.DISABLE_WIFI,

        "toggle_bluetooth" to ActionId.TOGGLE_BLUETOOTH,
        "enable_bluetooth" to ActionId.ENABLE_BLUETOOTH,
        "disable_bluetooth" to ActionId.DISABLE_BLUETOOTH,

        "toggle_mobile_data" to ActionId.TOGGLE_MOBILE_DATA,
        "enable_mobile_data" to ActionId.ENABLE_MOBILE_DATA,
        "disable_mobile_data" to ActionId.DISABLE_MOBILE_DATA,

        "toggle_auto_brightness" to ActionId.TOGGLE_AUTO_BRIGHTNESS,
        "disable_auto_brightness" to ActionId.DISABLE_AUTO_BRIGHTNESS,
        "enable_auto_brightness" to ActionId.ENABLE_AUTO_BRIGHTNESS,
        "increase_brightness" to ActionId.INCREASE_BRIGHTNESS,
        "decrease_brightness" to ActionId.DECREASE_BRIGHTNESS,

        "toggle_auto_rotate" to ActionId.TOGGLE_AUTO_ROTATE,
        "enable_auto_rotate" to ActionId.ENABLE_AUTO_ROTATE,
        "disable_auto_rotate" to ActionId.DISABLE_AUTO_ROTATE,
        "portrait_mode" to ActionId.PORTRAIT_MODE,
        "landscape_mode" to ActionId.LANDSCAPE_MODE,
        "switch_orientation" to ActionId.SWITCH_ORIENTATION,
        "cycle_rotations" to ActionId.CYCLE_ROTATIONS,

        "volume_up" to ActionId.VOLUME_UP,
        "volume_decrease_stream" to ActionId.VOLUME_UP,

        "volume_down" to ActionId.VOLUME_DOWN,
        "volume_increase_stream" to ActionId.VOLUME_DOWN,

        "volume_show_dialog" to ActionId.VOLUME_SHOW_DIALOG,
        "ringer_mode_cycle" to ActionId.CYCLE_RINGER_MODE,
        "ringer_mode_change" to ActionId.CHANGE_RINGER_MODE,
        "ringer_mode_cycle_vibrate_ring" to ActionId.CYCLE_VIBRATE_RING,
        "toggle_do_not_disturb_mode" to ActionId.TOGGLE_DND_MODE,
        "set_do_not_disturb_mode" to ActionId.ENABLE_DND_MODE,
        "disable_do_not_disturb_mode" to ActionId.DISABLE_DND_MODE,
        "volume_unmute" to ActionId.VOLUME_UNMUTE,
        "volume_mute" to ActionId.VOLUME_MUTE,
        "volume_toggle_mute" to ActionId.VOLUME_TOGGLE_MUTE,

        "expand_notification_drawer" to ActionId.EXPAND_NOTIFICATION_DRAWER,
        "toggle_notification_drawer" to ActionId.TOGGLE_NOTIFICATION_DRAWER,
        "expand_quick_settings" to ActionId.EXPAND_QUICK_SETTINGS,
        "toggle_quick_settings_drawer" to ActionId.TOGGLE_QUICK_SETTINGS,
        "collapse_status_bar" to ActionId.COLLAPSE_STATUS_BAR,

        "pause_media" to ActionId.PAUSE_MEDIA,
        "pause_media_package" to ActionId.PAUSE_MEDIA_PACKAGE,
        "play_media" to ActionId.PLAY_MEDIA,
        "play_media_package" to ActionId.PLAY_MEDIA_PACKAGE,
        "play_pause_media" to ActionId.PLAY_PAUSE_MEDIA,
        "play_pause_media_package" to ActionId.PLAY_PAUSE_MEDIA_PACKAGE,
        "next_track" to ActionId.NEXT_TRACK,
        "next_track_package" to ActionId.NEXT_TRACK_PACKAGE,
        "previous_track" to ActionId.PREVIOUS_TRACK,
        "previous_track_package" to ActionId.PREVIOUS_TRACK_PACKAGE,
        "fast_forward" to ActionId.FAST_FORWARD,
        "fast_forward_package" to ActionId.FAST_FORWARD_PACKAGE,
        "rewind" to ActionId.REWIND,
        "rewind_package" to ActionId.REWIND_PACKAGE,

        "go_back" to ActionId.GO_BACK,
        "go_home" to ActionId.GO_HOME,
        "open_recents" to ActionId.OPEN_RECENTS,
        "toggle_split_screen" to ActionId.TOGGLE_SPLIT_SCREEN,
        "go_last_app" to ActionId.GO_LAST_APP,
        "open_menu" to ActionId.OPEN_MENU,

        "toggle_flashlight" to ActionId.TOGGLE_FLASHLIGHT,
        "enable_flashlight" to ActionId.ENABLE_FLASHLIGHT,
        "disable_flashlight" to ActionId.DISABLE_FLASHLIGHT,

        "nfc_enable" to ActionId.ENABLE_NFC,
        "nfc_disable" to ActionId.DISABLE_NFC,
        "nfc_toggle" to ActionId.TOGGLE_NFC,

        "move_cursor_to_end" to ActionId.MOVE_CURSOR_TO_END,
        "toggle_keyboard" to ActionId.TOGGLE_KEYBOARD,
        "show_keyboard" to ActionId.SHOW_KEYBOARD,
        "hide_keyboard" to ActionId.HIDE_KEYBOARD,
        "show_keyboard_picker" to ActionId.SHOW_KEYBOARD_PICKER,
        "text_cut" to ActionId.TEXT_CUT,
        "text_copy" to ActionId.TEXT_COPY,
        "text_paste" to ActionId.TEXT_PASTE,
        "select_word_at_cursor" to ActionId.SELECT_WORD_AT_CURSOR,

        "switch_keyboard" to ActionId.SWITCH_KEYBOARD,

        "toggle_airplane_mode" to ActionId.TOGGLE_AIRPLANE_MODE,
        "enable_airplane_mode" to ActionId.ENABLE_AIRPLANE_MODE,
        "disable_airplane_mode" to ActionId.DISABLE_AIRPLANE_MODE,

        "screenshot" to ActionId.SCREENSHOT,
        "open_assistant" to ActionId.OPEN_VOICE_ASSISTANT,
        "open_device_assistant" to ActionId.OPEN_DEVICE_ASSISTANT,
        "open_camera" to ActionId.OPEN_CAMERA,
        "lock_device" to ActionId.LOCK_DEVICE,
        "power_on_off_device" to ActionId.POWER_ON_OFF_DEVICE,
        "secure_lock_device" to ActionId.SECURE_LOCK_DEVICE,
        "consume_key_event" to ActionId.CONSUME_KEY_EVENT,
        "open_settings" to ActionId.OPEN_SETTINGS,
        "show_power_menu" to ActionId.SHOW_POWER_MENU,

        "dismiss_most_recent_notification" to ActionId.DISMISS_MOST_RECENT_NOTIFICATION,
        "dismiss_all_notifications" to ActionId.DISMISS_ALL_NOTIFICATIONS,

        "answer_phone_call" to ActionId.ANSWER_PHONE_CALL,
        "end_phone_call" to ActionId.END_PHONE_CALL
    )
}