package io.github.sds100.keymapper.actions

import android.view.KeyEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.mappings.Mapping
import io.github.sds100.keymapper.system.camera.CameraLensUtils
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.display.OrientationUtils
import io.github.sds100.keymapper.system.intents.IntentTarget
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.system.volume.DndModeUtils
import io.github.sds100.keymapper.system.volume.RingerModeUtils
import io.github.sds100.keymapper.system.volume.VolumeStreamUtils
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.handle
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.TintType
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 18/03/2021.
 */

abstract class BaseActionUiHelper<MAPPING : Mapping<A>, A : Action>(
    displayActionUseCase: DisplayActionUseCase,
    resourceProvider: ResourceProvider
) : ActionUiHelper<MAPPING, A>,
    ResourceProvider by resourceProvider,
    DisplayActionUseCase by displayActionUseCase {

    override fun getTitle(action: ActionData, showDeviceDescriptors: Boolean): String =
        when (action) {
            is OpenAppAction ->
                getAppName(action.packageName).handle(
                    onSuccess = { getString(R.string.description_open_app, it) },
                    onError = { getString(R.string.description_open_app, action.packageName) }
                )

            is OpenAppShortcutAction -> action.shortcutTitle

            is KeyEventAction -> {
                val keyCodeString = if (action.keyCode > KeyEvent.getMaxKeyCode()) {
                    "Key Code ${action.keyCode}"
                } else {
                    KeyEvent.keyCodeToString(action.keyCode)
                }

                //only a key code can be inputted through the shell
                if (action.useShell) {
                    getString(R.string.description_keyevent_through_shell, keyCodeString)

                } else {

                    val metaStateString = buildString {

                        KeyEventUtils.MODIFIER_LABELS.entries.forEach {
                            val modifier = it.key
                            val labelRes = it.value

                            if (action.metaState.hasFlag(modifier)) {
                                append("${getString(labelRes)} + ")
                            }
                        }
                    }

                    if (action.device != null) {

                        val name = if (action.device.name.isBlank()) {
                            getString(R.string.unknown_device_name)
                        } else {
                            action.device.name
                        }

                        val nameToShow = if (showDeviceDescriptors) {
                            InputDeviceUtils.appendDeviceDescriptorToName(
                                action.device.descriptor,
                                name
                            )
                        } else {
                            name
                        }

                        getString(
                            R.string.description_keyevent_from_device,
                            arrayOf(metaStateString, keyCodeString, nameToShow)
                        )
                    } else {
                        getString(
                            R.string.description_keyevent,
                            args = arrayOf(metaStateString, keyCodeString)
                        )
                    }
                }
            }

            is DndModeAction.Enable -> {
                val dndModeString = getString(DndModeUtils.getLabel(action.dndMode))
                getString(
                    R.string.action_enable_dnd_mode_formatted,
                    dndModeString
                )
            }

            is DndModeAction.Toggle -> {
                val dndModeString = getString(DndModeUtils.getLabel(action.dndMode))
                getString(
                    R.string.action_toggle_dnd_mode_formatted,
                    dndModeString
                )
            }

            DndModeAction.Disable -> getString(R.string.action_disable_dnd_mode)

            is VolumeAction.SetRingerMode -> {
                val ringerModeString = getString(RingerModeUtils.getLabel(action.ringerMode))

                getString(R.string.action_change_ringer_mode_formatted, ringerModeString)
            }

            is VolumeAction -> {
                var hasShowVolumeUiFlag = false
                val string: String

                when (action) {
                    is VolumeAction.Stream -> {
                        val streamString = getString(
                            VolumeStreamUtils.getLabel(action.volumeStream)
                        )

                        if (action.showVolumeUi) {
                            hasShowVolumeUiFlag = true
                        }

                        string = when (action) {
                            is VolumeAction.Stream.Decrease -> getString(
                                R.string.action_decrease_stream_formatted,
                                streamString
                            )

                            is VolumeAction.Stream.Increase -> getString(
                                R.string.action_increase_stream_formatted,
                                streamString
                            )
                        }
                    }

                    is VolumeAction.Down -> {

                        if (action.showVolumeUi) {
                            hasShowVolumeUiFlag = true
                        }

                        string = getString(R.string.action_volume_down)
                    }

                    is VolumeAction.Mute -> {

                        if (action.showVolumeUi) {
                            hasShowVolumeUiFlag = true
                        }

                        string = getString(R.string.action_volume_mute)
                    }

                    is VolumeAction.ToggleMute -> {

                        if (action.showVolumeUi) {
                            hasShowVolumeUiFlag = true
                        }

                        string = getString(R.string.action_toggle_mute)
                    }

                    is VolumeAction.UnMute -> {

                        if (action.showVolumeUi) {
                            hasShowVolumeUiFlag = true
                        }

                        string = getString(R.string.action_volume_unmute)
                    }

                    is VolumeAction.Up -> {

                        if (action.showVolumeUi) {
                            hasShowVolumeUiFlag = true
                        }

                        string = getString(R.string.action_volume_up)
                    }

                    VolumeAction.CycleRingerMode -> {
                        string = getString(R.string.action_cycle_ringer_mode)
                    }

                    VolumeAction.CycleVibrateRing -> {
                        string = getString(R.string.action_cycle_vibrate_ring)
                    }

                    is VolumeAction.SetRingerMode -> {
                        val ringerModeString =
                            getString(RingerModeUtils.getLabel(action.ringerMode))

                        string = getString(
                            R.string.action_change_ringer_mode_formatted,
                            ringerModeString
                        )
                    }

                    VolumeAction.ShowDialog -> {
                        string = getString(R.string.action_volume_show_dialog)
                    }
                }

                if (hasShowVolumeUiFlag) {
                    val midDot = getString(R.string.middot)
                    "$string $midDot ${getString(R.string.flag_show_volume_dialog)}"
                } else {
                    string
                }
            }

            is ControlMediaForAppAction ->
                getAppName(action.packageName).handle(
                    onSuccess = { appName ->
                        val resId = when (action) {
                            is ControlMediaForAppAction.Play -> R.string.action_play_media_package_formatted
                            is ControlMediaForAppAction.FastForward -> R.string.action_fast_forward_package_formatted
                            is ControlMediaForAppAction.NextTrack -> R.string.action_next_track_package_formatted
                            is ControlMediaForAppAction.Pause -> R.string.action_pause_media_package_formatted
                            is ControlMediaForAppAction.PlayPause -> R.string.action_play_pause_media_package_formatted
                            is ControlMediaForAppAction.PreviousTrack -> R.string.action_previous_track_package_formatted
                            is ControlMediaForAppAction.Rewind -> R.string.action_rewind_package_formatted
                        }

                        getString(resId, appName)
                    },
                    onError = {
                        val resId = when (action) {
                            is ControlMediaForAppAction.Play -> R.string.action_play_media_package
                            is ControlMediaForAppAction.FastForward -> R.string.action_fast_forward_package
                            is ControlMediaForAppAction.NextTrack -> R.string.action_next_track_package
                            is ControlMediaForAppAction.Pause -> R.string.action_pause_media_package
                            is ControlMediaForAppAction.PlayPause -> R.string.action_play_pause_media_package
                            is ControlMediaForAppAction.PreviousTrack -> R.string.action_previous_track_package
                            is ControlMediaForAppAction.Rewind -> R.string.action_rewind_package
                        }

                        getString(resId)
                    }
                )

            is FlashlightAction -> {
                val resId = when (action) {
                    is FlashlightAction.Toggle -> R.string.action_toggle_flashlight_formatted
                    is FlashlightAction.Enable -> R.string.action_enable_flashlight_formatted
                    is FlashlightAction.Disable -> R.string.action_disable_flashlight_formatted
                }

                val lensString = getString(CameraLensUtils.getLabel(action.lens))

                getString(resId, lensString)
            }

            is SwitchKeyboardAction -> getInputMethodLabel(action.imeId).handle(
                onSuccess = { getString(R.string.action_switch_keyboard_formatted, it) },
                onError = {
                    getString(
                        R.string.action_switch_keyboard_formatted,
                        action.savedImeName
                    )
                }
            )

            is IntentAction -> {
                val resId = when (action.target) {
                    IntentTarget.ACTIVITY -> R.string.action_title_intent_start_activity
                    IntentTarget.BROADCAST_RECEIVER -> R.string.action_title_intent_send_broadcast
                    IntentTarget.SERVICE -> R.string.action_title_intent_start_service
                }

                getString(resId, action.description)
            }

            is PhoneCallAction -> getString(R.string.description_phone_call, action.number)

            is TapCoordinateAction -> if (action.description.isNullOrBlank()) {
                getString(
                    R.string.description_tap_coordinate_default,
                    arrayOf(action.x, action.y)
                )
            } else {
                getString(
                    R.string.description_tap_coordinate_with_description,
                    arrayOf(action.x, action.y, action.description)
                )
            }

            is TextAction -> getString(R.string.description_text_block, action.text)
            is UrlAction -> getString(R.string.description_url, action.url)
            is SoundAction -> getString(R.string.description_sound, action.soundDescription)

            AirplaneModeAction.Disable -> getString(R.string.action_disable_airplane_mode)
            AirplaneModeAction.Enable -> getString(R.string.action_enable_airplane_mode)
            AirplaneModeAction.Toggle -> getString(R.string.action_toggle_airplane_mode)

            BluetoothAction.Disable -> getString(R.string.action_disable_bluetooth)
            BluetoothAction.Enable -> getString(R.string.action_enable_bluetooth)
            BluetoothAction.Toggle -> getString(R.string.action_toggle_bluetooth)

            BrightnessAction.Decrease -> getString(R.string.action_decrease_brightness)
            BrightnessAction.DisableAuto -> getString(R.string.action_disable_auto_brightness)
            BrightnessAction.EnableAuto -> getString(R.string.action_enable_auto_brightness)
            BrightnessAction.Increase -> getString(R.string.action_increase_brightness)
            BrightnessAction.ToggleAuto -> getString(R.string.action_toggle_auto_brightness)

            ConsumeKeyEventAction -> getString(R.string.action_consume_keyevent)

            ControlMediaAction.FastForward -> getString(R.string.action_fast_forward)
            ControlMediaAction.NextTrack -> getString(R.string.action_next_track)
            ControlMediaAction.Pause -> getString(R.string.action_pause_media)
            ControlMediaAction.Play -> getString(R.string.action_play_media)
            ControlMediaAction.PlayPause -> getString(R.string.action_play_pause_media)
            ControlMediaAction.PreviousTrack -> getString(R.string.action_previous_track)
            ControlMediaAction.Rewind -> getString(R.string.action_rewind)

            CopyTextAction -> getString(R.string.action_text_copy)
            CutTextAction -> getString(R.string.action_text_cut)
            PasteTextAction -> getString(R.string.action_text_paste)

            DeviceAssistantAction -> getString(R.string.action_open_device_assistant)

            GoBackAction -> getString(R.string.action_go_back)
            GoHomeAction -> getString(R.string.action_go_home)
            GoLastAppAction -> getString(R.string.action_go_last_app)
            OpenMenuAction -> getString(R.string.action_open_menu)
            OpenRecentsAction -> getString(R.string.action_open_recents)

            HideKeyboardAction -> getString(R.string.action_hide_keyboard)
            LockDeviceAction -> getString(R.string.action_lock_device)

            MobileDataAction.Disable -> getString(R.string.action_disable_mobile_data)
            MobileDataAction.Enable -> getString(R.string.action_enable_mobile_data)
            MobileDataAction.Toggle -> getString(R.string.action_toggle_mobile_data)

            MoveCursorToEndAction -> getString(R.string.action_move_to_end_of_text)

            NfcAction.Disable -> getString(R.string.action_nfc_disable)
            NfcAction.Enable -> getString(R.string.action_nfc_enable)
            NfcAction.Toggle -> getString(R.string.action_nfc_toggle)

            OpenCameraAction -> getString(R.string.action_open_camera)
            OpenSettingsAction -> getString(R.string.action_open_settings)

            is RotationAction.CycleRotations -> {
                val orientationStrings = action.orientations.map {
                    getString(OrientationUtils.getLabel(it))
                }

                getString(
                    R.string.action_cycle_rotations_formatted,
                    orientationStrings.joinToString()
                )
            }
            RotationAction.DisableAuto -> getString(R.string.action_disable_auto_rotate)
            RotationAction.EnableAuto -> getString(R.string.action_enable_auto_rotate)
            RotationAction.Landscape -> getString(R.string.action_landscape_mode)
            RotationAction.Portrait -> getString(R.string.action_portrait_mode)
            RotationAction.SwitchOrientation -> getString(R.string.action_switch_orientation)
            RotationAction.ToggleAuto -> getString(R.string.action_toggle_auto_rotate)

            ScreenOnOffAction -> getString(R.string.action_power_on_off_device)
            ScreenshotAction -> getString(R.string.action_screenshot)
            SecureLockAction -> getString(R.string.action_secure_lock_device)
            SelectWordAtCursorAction -> getString(R.string.action_select_word_at_cursor)
            ShowKeyboardAction -> getString(R.string.action_show_keyboard)
            ShowKeyboardPickerAction -> getString(R.string.action_show_keyboard_picker)
            ShowPowerMenuAction -> getString(R.string.action_show_power_menu)

            StatusBarAction.Collapse -> getString(R.string.action_collapse_status_bar)
            StatusBarAction.ExpandNotifications -> getString(R.string.action_expand_notification_drawer)
            StatusBarAction.ExpandQuickSettings -> getString(R.string.action_expand_quick_settings)
            StatusBarAction.ToggleNotifications -> getString(R.string.action_toggle_notification_drawer)
            StatusBarAction.ToggleQuickSettings -> getString(R.string.action_toggle_quick_settings)

            ToggleKeyboardAction -> getString(R.string.action_toggle_keyboard)
            ToggleSplitScreenAction -> getString(R.string.action_toggle_split_screen)
            VoiceAssistantAction -> getString(R.string.action_open_assistant)

            WifiAction.Disable -> getString(R.string.action_disable_wifi)
            WifiAction.Enable -> getString(R.string.action_enable_wifi)
            WifiAction.Toggle -> getString(R.string.action_toggle_wifi)
            DismissAllNotificationsAction -> getString(R.string.action_dismiss_all_notifications)
            DismissLastNotificationAction -> getString(R.string.action_dismiss_most_recent_notification)
        }

    override fun getIcon(action: ActionData): IconInfo? = when (action) {
        is KeyEventAction -> null

        is OpenAppAction ->
            getAppIcon(action.packageName).handle(
                onSuccess = { IconInfo(it, TintType.None) },
                onError = { null }
            )

        is OpenAppShortcutAction -> {
            if (action.packageName.isNullOrBlank()) {
                null
            } else {
                getAppIcon(action.packageName).handle(
                    onSuccess = { IconInfo(it, TintType.None) },
                    onError = { null }
                )
            }
        }

        is IntentAction -> null

        is PhoneCallAction ->
            IconInfo(
                getDrawable(R.drawable.ic_outline_call_24),
                tintType = TintType.OnSurface
            )

        is TapCoordinateAction -> IconInfo(
            getDrawable(R.drawable.ic_outline_touch_app_24),
            TintType.OnSurface
        )

        is TextAction -> null
        is UrlAction -> null
        is SoundAction -> IconInfo(
            getDrawable(R.drawable.ic_outline_volume_up_24),
            TintType.OnSurface
        )

        else -> ActionUtils.getIcon(action.id)?.let { iconRes ->
            IconInfo(
                getDrawable(iconRes),
                TintType.OnSurface
            )
        }
    }
}

interface ActionUiHelper<MAPPING : Mapping<A>, A : Action> {
    fun getTitle(action: ActionData, showDeviceDescriptors: Boolean): String
    fun getOptionLabels(mapping: MAPPING, action: A): List<String>
    fun getIcon(action: ActionData): IconInfo?
    fun getError(action: ActionData): Error?
}