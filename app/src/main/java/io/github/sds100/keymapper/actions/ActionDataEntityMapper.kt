package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.volume.DndMode
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.intents.IntentTarget
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
        return when (entity.type) {
            ActionEntity.Type.APP -> OpenAppAction(packageName = entity.data)

            ActionEntity.Type.APP_SHORTCUT -> {
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

            ActionEntity.Type.KEY_EVENT -> {
                val metaState =
                    entity.extras.getData(ActionEntity.EXTRA_KEY_EVENT_META_STATE).valueOrNull()
                        ?.toInt()
                        ?: 0

                val deviceDescriptor =
                    entity.extras.getData(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR)
                        .valueOrNull()

                val useShell =
                    entity.extras.getData(ActionEntity.EXTRA_KEY_EVENT_USE_SHELL).then {
                        (it == "true").success()
                    }.valueOrNull() ?: false

                //TODO issue #612 get device name name
                val device = if (deviceDescriptor != null) {
                    KeyEventAction.Device(deviceDescriptor, "stub name")
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

            ActionEntity.Type.TEXT_BLOCK -> TextAction(text = entity.data)
            ActionEntity.Type.URL -> UrlAction(url = entity.data)
            ActionEntity.Type.TAP_COORDINATE -> {
                val x = entity.data.split(',')[0].toInt()
                val y = entity.data.split(',')[0].toInt()
                val description = entity.extras.getData(ActionEntity.EXTRA_COORDINATE_DESCRIPTION)
                    .valueOrNull()

                TapCoordinateAction(x = x, y = y, description = description)
            }

            ActionEntity.Type.INTENT -> {
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

            ActionEntity.Type.PHONE_CALL -> PhoneCallAction(number = entity.data)

            ActionEntity.Type.SYSTEM_ACTION -> {
                val id = SYSTEM_ACTION_ID_MAP.getKey(entity.data) ?: return null

                when (id) {
                    SystemActionId.VOLUME_INCREASE_STREAM,
                    SystemActionId.VOLUME_DECREASE_STREAM -> {
                        val stream = entity.extras.getData(ActionEntity.EXTRA_STREAM_TYPE).then {
                            VOLUME_STREAM_MAP.getKey(it)!!.success()
                        }.valueOrNull() ?: return null

                        val showVolumeUi =
                            entity.flags.hasFlag(ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI)

                        when (id) {
                            SystemActionId.VOLUME_INCREASE_STREAM ->
                                VolumeSystemAction.Stream.Increase(showVolumeUi, stream)

                            SystemActionId.VOLUME_DECREASE_STREAM ->
                                VolumeSystemAction.Stream.Decrease(showVolumeUi, stream)

                            else -> throw Exception("don't know how to create system action for $id")
                        }
                    }

                    SystemActionId.VOLUME_UP,
                    SystemActionId.VOLUME_DOWN,
                    SystemActionId.VOLUME_TOGGLE_MUTE,
                    SystemActionId.VOLUME_UNMUTE,
                    SystemActionId.VOLUME_MUTE -> {
                        val showVolumeUi =
                            entity.flags.hasFlag(ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI)

                        when (id) {
                            SystemActionId.VOLUME_UP -> VolumeSystemAction.Up(showVolumeUi)
                            SystemActionId.VOLUME_DOWN -> VolumeSystemAction.Down(showVolumeUi)
                            SystemActionId.VOLUME_TOGGLE_MUTE -> VolumeSystemAction.ToggleMute(
                                showVolumeUi
                            )
                            SystemActionId.VOLUME_UNMUTE -> VolumeSystemAction.UnMute(showVolumeUi)
                            SystemActionId.VOLUME_MUTE -> VolumeSystemAction.Mute(showVolumeUi)

                            else -> throw Exception("don't know how to create system action for $id")
                        }
                    }

                    SystemActionId.TOGGLE_FLASHLIGHT,
                    SystemActionId.ENABLE_FLASHLIGHT,
                    SystemActionId.DISABLE_FLASHLIGHT -> {
                        val lens = entity.extras.getData(ActionEntity.EXTRA_LENS).then {
                            LENS_MAP.getKey(it)!!.success()
                        }.valueOrNull() ?: return null

                        when (id) {
                            SystemActionId.TOGGLE_FLASHLIGHT ->
                                FlashlightSystemAction.Toggle(lens)

                            SystemActionId.ENABLE_FLASHLIGHT ->
                                FlashlightSystemAction.Enable(lens)

                            SystemActionId.DISABLE_FLASHLIGHT ->
                                FlashlightSystemAction.Disable(lens)

                            else -> throw Exception("don't know how to create system action for $id")
                        }
                    }

                    SystemActionId.TOGGLE_DND_MODE,
                    SystemActionId.ENABLE_DND_MODE -> {
                        val dndMode = entity.extras.getData(ActionEntity.EXTRA_DND_MODE).then {
                            DND_MODE_MAP.getKey(it)!!.success()
                        }.valueOrNull() ?: return null

                        when (id) {
                            SystemActionId.TOGGLE_DND_MODE ->
                                ToggleDndMode(dndMode)

                            SystemActionId.ENABLE_DND_MODE ->
                                EnableDndMode(dndMode)

                            else -> throw Exception("don't know how to create system action for $id")
                        }
                    }

                    SystemActionId.DISABLE_DND_MODE ->{
                        return SimpleSystemAction(SystemActionId.DISABLE_DND_MODE)
                    }

                    SystemActionId.PAUSE_MEDIA_PACKAGE,
                    SystemActionId.PLAY_MEDIA_PACKAGE,
                    SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE,
                    SystemActionId.NEXT_TRACK_PACKAGE,
                    SystemActionId.PREVIOUS_TRACK_PACKAGE,
                    SystemActionId.FAST_FORWARD_PACKAGE,
                    SystemActionId.REWIND_PACKAGE -> {
                        val packageName =
                            entity.extras.getData(ActionEntity.EXTRA_PACKAGE_NAME).valueOrNull()
                                ?: return null

                        when (id) {
                            SystemActionId.PAUSE_MEDIA_PACKAGE ->
                                ControlMediaForAppSystemAction.Pause(packageName)
                            SystemActionId.PLAY_MEDIA_PACKAGE ->
                                ControlMediaForAppSystemAction.Play(packageName)
                            SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE ->
                                ControlMediaForAppSystemAction.PlayPause(packageName)
                            SystemActionId.NEXT_TRACK_PACKAGE ->
                                ControlMediaForAppSystemAction.NextTrack(packageName)
                            SystemActionId.PREVIOUS_TRACK_PACKAGE ->
                                ControlMediaForAppSystemAction.PreviousTrack(packageName)
                            SystemActionId.FAST_FORWARD_PACKAGE ->
                                ControlMediaForAppSystemAction.FastForward(packageName)
                            SystemActionId.REWIND_PACKAGE ->
                                ControlMediaForAppSystemAction.Rewind(packageName)

                            else -> throw Exception("don't know how to create system action for $id")
                        }
                    }

                    SystemActionId.CHANGE_RINGER_MODE -> {
                        val ringerMode =
                            entity.extras.getData(ActionEntity.EXTRA_RINGER_MODE).then {
                                RINGER_MODE_MAP.getKey(it)!!.success()
                            }.valueOrNull() ?: return null

                        ChangeRingerModeSystemAction(ringerMode)
                    }

                    SystemActionId.SWITCH_KEYBOARD -> {
                        val imeId = entity.extras.getData(ActionEntity.EXTRA_IME_ID)
                            .valueOrNull() ?: return null

                        val imeName = entity.extras.getData(ActionEntity.EXTRA_IME_NAME)
                            .valueOrNull() ?: return null

                        SwitchKeyboardSystemAction(imeId, imeName)
                    }

                    SystemActionId.CYCLE_ROTATIONS -> {
                        val orientations =
                            entity.extras.getData(ActionEntity.EXTRA_ORIENTATIONS)
                                .then { extraValue ->
                                    extraValue
                                        .split(",")
                                        .map { ORIENTATION_MAP.getKey(it)!! }
                                        .success()
                                }.valueOrNull() ?: return null

                        CycleRotationsSystemAction(orientations)
                    }

                    else -> SimpleSystemAction(id)
                }
            }
        }
    }

    fun toEntity(data: ActionData): ActionEntity? {
        val type = when (data) {
            CorruptAction -> return null
            is IntentAction -> ActionEntity.Type.INTENT
            is KeyEventAction -> ActionEntity.Type.KEY_EVENT
            is OpenAppAction -> ActionEntity.Type.APP
            is OpenAppShortcutAction -> ActionEntity.Type.APP_SHORTCUT
            is PhoneCallAction -> ActionEntity.Type.PHONE_CALL
            is SystemAction -> ActionEntity.Type.SYSTEM_ACTION
            is TapCoordinateAction -> ActionEntity.Type.TAP_COORDINATE
            is TextAction -> ActionEntity.Type.TEXT_BLOCK
            is UrlAction -> ActionEntity.Type.URL
        }

        return ActionEntity(
            type = type,
            data = getDataString(data),
            extras = getExtras(data),
            flags = getFlags(data)
        )
    }

    private fun getFlags(data: ActionData): Int = when (data) {
        is VolumeSystemAction ->
            if (data.showVolumeUi) {
                ActionEntity.ACTION_FLAG_SHOW_VOLUME_UI
            } else {
                0
            }

        else -> 0
    }

    private fun getDataString(data: ActionData): String = when (data) {
        CorruptAction -> ""
        is IntentAction -> data.uri
        is KeyEventAction -> data.keyCode.toString()
        is OpenAppAction -> data.packageName
        is OpenAppShortcutAction -> data.uri
        is PhoneCallAction -> data.number
        is SystemAction -> SYSTEM_ACTION_ID_MAP[data.id]!!
        is TapCoordinateAction -> "${data.x},${data.y}"
        is TextAction -> data.text
        is UrlAction -> data.url
    }

    private fun getExtras(data: ActionData): List<Extra> = when (data) {
        CorruptAction -> emptyList()
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
            }
            //TODO save device name
        }.toList()

        is OpenAppAction -> emptyList()
        is OpenAppShortcutAction -> sequence {
            yield(Extra(ActionEntity.EXTRA_SHORTCUT_TITLE, data.shortcutTitle))
            data.packageName?.let { yield(Extra(ActionEntity.EXTRA_PACKAGE_NAME, it)) }
        }.toList()

        is PhoneCallAction -> emptyList()

        is EnableDndMode -> listOf(
            Extra(ActionEntity.EXTRA_DND_MODE, DND_MODE_MAP[data.dndMode]!!)
        )

        is ToggleDndMode -> listOf(
            Extra(ActionEntity.EXTRA_DND_MODE, DND_MODE_MAP[data.dndMode]!!)
        )

        is ChangeRingerModeSystemAction -> listOf(
            Extra(ActionEntity.EXTRA_RINGER_MODE, RINGER_MODE_MAP[data.ringerMode]!!)
        )

        is ControlMediaForAppSystemAction -> listOf(
            Extra(ActionEntity.EXTRA_PACKAGE_NAME, data.packageName)
        )

        is CycleRotationsSystemAction -> listOf(
            Extra(
                ActionEntity.EXTRA_ORIENTATIONS,
                data.orientations.joinToString(",") { ORIENTATION_MAP[it]!! })
        )

        is FlashlightSystemAction -> listOf(
            Extra(ActionEntity.EXTRA_LENS, LENS_MAP[data.lens]!!)
        )

        is SimpleSystemAction -> emptyList()

        is SwitchKeyboardSystemAction -> listOf(
            Extra(ActionEntity.EXTRA_IME_ID, data.imeId),
            Extra(ActionEntity.EXTRA_IME_NAME, data.savedImeName)
        )

        is VolumeSystemAction ->
            when (data) {
                is VolumeSystemAction.Stream -> listOf(
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
        SystemActionId.TOGGLE_WIFI to "toggle_wifi",
        SystemActionId.ENABLE_WIFI to "enable_wifi",
        SystemActionId.DISABLE_WIFI to "disable_wifi",

        SystemActionId.TOGGLE_BLUETOOTH to "toggle_bluetooth",
        SystemActionId.ENABLE_BLUETOOTH to "enable_bluetooth",
        SystemActionId.DISABLE_BLUETOOTH to "disable_bluetooth",

        SystemActionId.TOGGLE_MOBILE_DATA to "toggle_mobile_data",
        SystemActionId.ENABLE_MOBILE_DATA to "enable_mobile_data",
        SystemActionId.DISABLE_MOBILE_DATA to "disable_mobile_data",

        SystemActionId.TOGGLE_AUTO_BRIGHTNESS to "toggle_auto_brightness",
        SystemActionId.DISABLE_AUTO_BRIGHTNESS to "disable_auto_brightness",
        SystemActionId.ENABLE_AUTO_BRIGHTNESS to "enable_auto_brightness",
        SystemActionId.INCREASE_BRIGHTNESS to "increase_brightness",
        SystemActionId.DECREASE_BRIGHTNESS to "decrease_brightness",

        SystemActionId.TOGGLE_AUTO_ROTATE to "toggle_auto_rotate",
        SystemActionId.ENABLE_AUTO_ROTATE to "enable_auto_rotate",
        SystemActionId.DISABLE_AUTO_ROTATE to "disable_auto_rotate",
        SystemActionId.PORTRAIT_MODE to "portrait_mode",
        SystemActionId.LANDSCAPE_MODE to "landscape_mode",
        SystemActionId.SWITCH_ORIENTATION to "switch_orientation",
        SystemActionId.CYCLE_ROTATIONS to "cycle_rotations",

        SystemActionId.VOLUME_UP to "volume_up",
        SystemActionId.VOLUME_DOWN to "volume_down",
        SystemActionId.VOLUME_SHOW_DIALOG to "volume_show_dialog",
        SystemActionId.VOLUME_DECREASE_STREAM to "volume_decrease_stream",
        SystemActionId.VOLUME_INCREASE_STREAM to "volume_increase_stream",
        SystemActionId.CYCLE_RINGER_MODE to "ringer_mode_cycle",
        SystemActionId.CHANGE_RINGER_MODE to "ringer_mode_change",
        SystemActionId.CYCLE_VIBRATE_RING to "ringer_mode_cycle_vibrate_ring",
        SystemActionId.TOGGLE_DND_MODE to "toggle_do_not_disturb_mode",
        SystemActionId.ENABLE_DND_MODE to "set_do_not_disturb_mode",
        SystemActionId.DISABLE_DND_MODE to "disable_do_not_disturb_mode",
        SystemActionId.VOLUME_UNMUTE to "volume_unmute",
        SystemActionId.VOLUME_MUTE to "volume_mute",
        SystemActionId.VOLUME_TOGGLE_MUTE to "volume_toggle_mute",

        SystemActionId.EXPAND_NOTIFICATION_DRAWER to "expand_notification_drawer",
        SystemActionId.TOGGLE_NOTIFICATION_DRAWER to "toggle_notification_drawer",
        SystemActionId.EXPAND_QUICK_SETTINGS to "expand_quick_settings",
        SystemActionId.TOGGLE_QUICK_SETTINGS to "toggle_quick_settings_drawer",
        SystemActionId.COLLAPSE_STATUS_BAR to "collapse_status_bar",

        SystemActionId.PAUSE_MEDIA to "pause_media",
        SystemActionId.PAUSE_MEDIA_PACKAGE to "pause_media_package",
        SystemActionId.PLAY_MEDIA to "play_media",
        SystemActionId.PLAY_MEDIA_PACKAGE to "play_media_package",
        SystemActionId.PLAY_PAUSE_MEDIA to "play_pause_media",
        SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE to "play_pause_media_package",
        SystemActionId.NEXT_TRACK to "next_track",
        SystemActionId.NEXT_TRACK_PACKAGE to "next_track_package",
        SystemActionId.PREVIOUS_TRACK to "previous_track",
        SystemActionId.PREVIOUS_TRACK_PACKAGE to "previous_track_package",
        SystemActionId.FAST_FORWARD to "fast_forward",
        SystemActionId.FAST_FORWARD_PACKAGE to "fast_forward_package",
        SystemActionId.REWIND to "rewind",
        SystemActionId.REWIND_PACKAGE to "rewind_package",

        SystemActionId.GO_BACK to "go_back",
        SystemActionId.GO_HOME to "go_home",
        SystemActionId.OPEN_RECENTS to "open_recents",
        SystemActionId.TOGGLE_SPLIT_SCREEN to "toggle_split_screen",
        SystemActionId.GO_LAST_APP to "go_last_app",
        SystemActionId.OPEN_MENU to "open_menu",

        SystemActionId.TOGGLE_FLASHLIGHT to "toggle_flashlight",
        SystemActionId.ENABLE_FLASHLIGHT to "enable_flashlight",
        SystemActionId.DISABLE_FLASHLIGHT to "disable_flashlight",

        SystemActionId.ENABLE_NFC to "nfc_enable",
        SystemActionId.DISABLE_NFC to "nfc_disable",
        SystemActionId.TOGGLE_NFC to "nfc_toggle",

        SystemActionId.MOVE_CURSOR_TO_END to "move_cursor_to_end",
        SystemActionId.TOGGLE_KEYBOARD to "toggle_keyboard",
        SystemActionId.SHOW_KEYBOARD to "show_keyboard",
        SystemActionId.HIDE_KEYBOARD to "hide_keyboard",
        SystemActionId.SHOW_KEYBOARD_PICKER to "show_keyboard_picker",
        SystemActionId.TEXT_CUT to "text_cut",
        SystemActionId.TEXT_COPY to "text_copy",
        SystemActionId.TEXT_PASTE to "text_paste",
        SystemActionId.SELECT_WORD_AT_CURSOR to "select_word_at_cursor",

        SystemActionId.SWITCH_KEYBOARD to "switch_keyboard",

        SystemActionId.TOGGLE_AIRPLANE_MODE to "toggle_airplane_mode",
        SystemActionId.ENABLE_AIRPLANE_MODE to "enable_airplane_mode",
        SystemActionId.DISABLE_AIRPLANE_MODE to "disable_airplane_mode",

        SystemActionId.SCREENSHOT to "screenshot",
        SystemActionId.OPEN_VOICE_ASSISTANT to "open_assistant",
        SystemActionId.OPEN_DEVICE_ASSISTANT to "open_device_assistant",
        SystemActionId.OPEN_CAMERA to "open_camera",
        SystemActionId.LOCK_DEVICE to "lock_device",
        SystemActionId.POWER_ON_OFF_DEVICE to "power_on_off_device",
        SystemActionId.SECURE_LOCK_DEVICE to "secure_lock_device",
        SystemActionId.CONSUME_KEY_EVENT to "consume_key_event",
        SystemActionId.OPEN_SETTINGS to "open_settings",
        SystemActionId.SHOW_POWER_MENU to "show_power_menu",
    )
}