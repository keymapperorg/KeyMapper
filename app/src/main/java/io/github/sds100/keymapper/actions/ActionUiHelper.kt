package io.github.sds100.keymapper.actions

import android.os.Build
import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.pinchscreen.PinchScreenType
import io.github.sds100.keymapper.common.result.handle
import io.github.sds100.keymapper.keymaps.KeyMap
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.display.OrientationUtils
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.intents.IntentTarget
import io.github.sds100.keymapper.system.volume.DndModeUtils
import io.github.sds100.keymapper.system.volume.RingerModeUtils
import io.github.sds100.keymapper.system.volume.VolumeStreamUtils
import io.github.sds100.keymapper.util.toPercentString
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import splitties.bitflags.hasFlag

class ActionUiHelper(
    displayActionUseCase: DisplayActionUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    DisplayActionUseCase by displayActionUseCase {

    fun getTitle(action: ActionData, showDeviceDescriptors: Boolean): String = when (action) {
        is ActionData.App ->
            getAppName(action.packageName).handle(
                onSuccess = { getString(R.string.description_open_app, it) },
                onError = { getString(R.string.description_open_app, action.packageName) },
            )

        is ActionData.AppShortcut -> action.shortcutTitle

        is ActionData.InputKeyEvent -> {
            val keyCodeString = if (action.keyCode > KeyEvent.getMaxKeyCode()) {
                "Key Code ${action.keyCode}"
            } else {
                KeyEvent.keyCodeToString(action.keyCode)
            }

            // only a key code can be inputted through the shell
            if (action.useShell) {
                getString(R.string.description_keyevent_through_shell, keyCodeString)
            } else {
                val metaStateString = buildString {
                    InputEventUtils.MODIFIER_LABELS.entries.forEach {
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
                            name,
                        )
                    } else {
                        name
                    }

                    getString(
                        R.string.description_keyevent_from_device,
                        arrayOf(metaStateString, keyCodeString, nameToShow),
                    )
                } else {
                    getString(
                        R.string.description_keyevent,
                        args = arrayOf(metaStateString, keyCodeString),
                    )
                }
            }
        }

        is ActionData.DoNotDisturb.Enable -> {
            val dndModeString = getString(DndModeUtils.getLabel(action.dndMode))
            getString(
                R.string.action_enable_dnd_mode_formatted,
                dndModeString,
            )
        }

        is ActionData.DoNotDisturb.Toggle -> {
            val dndModeString = getString(DndModeUtils.getLabel(action.dndMode))
            getString(
                R.string.action_toggle_dnd_mode_formatted,
                dndModeString,
            )
        }

        ActionData.DoNotDisturb.Disable -> getString(R.string.action_disable_dnd_mode)

        is ActionData.Volume.SetRingerMode -> {
            val ringerModeString = getString(RingerModeUtils.getLabel(action.ringerMode))

            getString(R.string.action_change_ringer_mode_formatted, ringerModeString)
        }

        is ActionData.Volume -> {
            var hasShowVolumeUiFlag = false
            val string: String

            when (action) {
                is ActionData.Volume.Stream -> {
                    val streamString = getString(
                        VolumeStreamUtils.getLabel(action.volumeStream),
                    )

                    if (action.showVolumeUi) {
                        hasShowVolumeUiFlag = true
                    }

                    string = when (action) {
                        is ActionData.Volume.Stream.Decrease -> getString(
                            R.string.action_decrease_stream_formatted,
                            streamString,
                        )

                        is ActionData.Volume.Stream.Increase -> getString(
                            R.string.action_increase_stream_formatted,
                            streamString,
                        )
                    }
                }

                is ActionData.Volume.Down -> {
                    if (action.showVolumeUi) {
                        hasShowVolumeUiFlag = true
                    }

                    string = getString(R.string.action_volume_down)
                }

                is ActionData.Volume.Mute -> {
                    if (action.showVolumeUi) {
                        hasShowVolumeUiFlag = true
                    }

                    string = getString(R.string.action_volume_mute)
                }

                is ActionData.Volume.ToggleMute -> {
                    if (action.showVolumeUi) {
                        hasShowVolumeUiFlag = true
                    }

                    string = getString(R.string.action_toggle_mute)
                }

                is ActionData.Volume.UnMute -> {
                    if (action.showVolumeUi) {
                        hasShowVolumeUiFlag = true
                    }

                    string = getString(R.string.action_volume_unmute)
                }

                is ActionData.Volume.Up -> {
                    if (action.showVolumeUi) {
                        hasShowVolumeUiFlag = true
                    }

                    string = getString(R.string.action_volume_up)
                }

                ActionData.Volume.CycleRingerMode -> {
                    string = getString(R.string.action_cycle_ringer_mode)
                }

                ActionData.Volume.CycleVibrateRing -> {
                    string = getString(R.string.action_cycle_vibrate_ring)
                }

                is ActionData.Volume.SetRingerMode -> {
                    val ringerModeString =
                        getString(RingerModeUtils.getLabel(action.ringerMode))

                    string = getString(
                        R.string.action_change_ringer_mode_formatted,
                        ringerModeString,
                    )
                }

                ActionData.Volume.ShowDialog -> {
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

        is ActionData.ControlMediaForApp ->
            getAppName(action.packageName).handle(
                onSuccess = { appName ->
                    val resId = when (action) {
                        is ActionData.ControlMediaForApp.Play -> R.string.action_play_media_package_formatted
                        is ActionData.ControlMediaForApp.FastForward -> R.string.action_fast_forward_package_formatted
                        is ActionData.ControlMediaForApp.NextTrack -> R.string.action_next_track_package_formatted
                        is ActionData.ControlMediaForApp.Pause -> R.string.action_pause_media_package_formatted
                        is ActionData.ControlMediaForApp.PlayPause -> R.string.action_play_pause_media_package_formatted
                        is ActionData.ControlMediaForApp.PreviousTrack -> R.string.action_previous_track_package_formatted
                        is ActionData.ControlMediaForApp.Rewind -> R.string.action_rewind_package_formatted
                        is ActionData.ControlMediaForApp.Stop -> R.string.action_stop_media_package_formatted
                        is ActionData.ControlMediaForApp.StepForward -> R.string.action_step_forward_media_package_formatted
                        is ActionData.ControlMediaForApp.StepBackward -> R.string.action_step_backward_media_package_formatted
                    }

                    getString(resId, appName)
                },
                onError = {
                    val resId = when (action) {
                        is ActionData.ControlMediaForApp.Play -> R.string.action_play_media_package
                        is ActionData.ControlMediaForApp.FastForward -> R.string.action_fast_forward_package
                        is ActionData.ControlMediaForApp.NextTrack -> R.string.action_next_track_package
                        is ActionData.ControlMediaForApp.Pause -> R.string.action_pause_media_package
                        is ActionData.ControlMediaForApp.PlayPause -> R.string.action_play_pause_media_package
                        is ActionData.ControlMediaForApp.PreviousTrack -> R.string.action_previous_track_package
                        is ActionData.ControlMediaForApp.Rewind -> R.string.action_rewind_package
                        is ActionData.ControlMediaForApp.Stop -> R.string.action_stop_media_package
                        is ActionData.ControlMediaForApp.StepForward -> R.string.action_step_forward_media_package
                        is ActionData.ControlMediaForApp.StepBackward -> R.string.action_step_backward_media_package
                    }

                    getString(resId)
                },
            )

        is ActionData.Flashlight -> {
            when (action) {
                is ActionData.Flashlight.Toggle -> {
                    if (action.strengthPercent == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        if (action.lens == CameraLens.FRONT) {
                            getString(R.string.action_toggle_front_flashlight_formatted)
                        } else {
                            getString(R.string.action_toggle_flashlight_formatted)
                        }
                    } else {
                        if (action.lens == CameraLens.FRONT) {
                            getString(
                                R.string.action_toggle_front_flashlight_with_strength,
                                action.strengthPercent.toPercentString(),

                            )
                        } else {
                            getString(
                                R.string.action_toggle_flashlight_with_strength,
                                action.strengthPercent.toPercentString(),
                            )
                        }
                    }
                }

                is ActionData.Flashlight.Enable -> {
                    if (action.strengthPercent == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        if (action.lens == CameraLens.FRONT) {
                            getString(R.string.action_enable_front_flashlight_formatted)
                        } else {
                            getString(R.string.action_enable_flashlight_formatted)
                        }
                    } else {
                        if (action.lens == CameraLens.FRONT) {
                            getString(
                                R.string.action_enable_front_flashlight_with_strength,
                                action.strengthPercent.toPercentString(),
                            )
                        } else {
                            getString(
                                R.string.action_enable_flashlight_with_strength,
                                action.strengthPercent.toPercentString(),
                            )
                        }
                    }
                }

                is ActionData.Flashlight.Disable -> {
                    if (action.lens == CameraLens.FRONT) {
                        getString(R.string.action_disable_front_flashlight_formatted)
                    } else {
                        getString(R.string.action_disable_flashlight_formatted)
                    }
                }

                is ActionData.Flashlight.ChangeStrength -> {
                    if (action.percent > 0) {
                        if (action.lens == CameraLens.FRONT) {
                            getString(
                                R.string.action_front_flashlight_increase_strength_formatted,
                                action.percent.toPercentString(),
                            )
                        } else {
                            getString(
                                R.string.action_flashlight_increase_strength_formatted,
                                action.percent.toPercentString(),
                            )
                        }
                    } else {
                        if (action.lens == CameraLens.FRONT) {
                            getString(
                                R.string.action_front_flashlight_decrease_strength_formatted,
                                action.percent.toPercentString(),
                            )
                        } else {
                            getString(
                                R.string.action_flashlight_decrease_strength_formatted,
                                action.percent.toPercentString(),
                            )
                        }
                    }
                }
            }
        }

        is ActionData.SwitchKeyboard -> getInputMethodLabel(action.imeId).handle(
            onSuccess = { getString(R.string.action_switch_keyboard_formatted, it) },
            onError = {
                getString(
                    R.string.action_switch_keyboard_formatted,
                    action.savedImeName,
                )
            },
        )

        is ActionData.Intent -> {
            val resId = when (action.target) {
                IntentTarget.ACTIVITY -> R.string.action_title_intent_start_activity
                IntentTarget.BROADCAST_RECEIVER -> R.string.action_title_intent_send_broadcast
                IntentTarget.SERVICE -> R.string.action_title_intent_start_service
            }

            getString(resId, action.description)
        }

        is ActionData.PhoneCall -> getString(R.string.description_phone_call, action.number)

        is ActionData.TapScreen -> if (action.description.isNullOrBlank()) {
            getString(
                R.string.description_tap_coordinate_default,
                arrayOf(action.x, action.y),
            )
        } else {
            getString(
                R.string.description_tap_coordinate_with_description,
                action.description,
            )
        }

        is ActionData.SwipeScreen -> if (action.description.isNullOrBlank()) {
            getString(
                R.string.description_swipe_coordinate_default,
                arrayOf(
                    action.fingerCount,
                    action.xStart,
                    action.yStart,
                    action.xEnd,
                    action.yEnd,
                    action.duration,
                ),
            )
        } else {
            getString(
                R.string.description_swipe_coordinate_with_description,
                arrayOf(
                    action.fingerCount,
                    action.xStart,
                    action.yStart,
                    action.xEnd,
                    action.yEnd,
                    action.duration,
                    action.description,
                ),
            )
        }

        is ActionData.PinchScreen -> if (action.description.isNullOrBlank()) {
            val pinchTypeDisplayName = if (action.pinchType == PinchScreenType.PINCH_IN) {
                getString(R.string.hint_coordinate_type_pinch_in)
            } else {
                getString(R.string.hint_coordinate_type_pinch_out)
            }

            getString(
                R.string.description_pinch_coordinate_default,
                arrayOf(
                    pinchTypeDisplayName,
                    action.fingerCount,
                    action.x,
                    action.y,
                    action.distance,
                    action.duration,
                ),
            )
        } else {
            val pinchTypeDisplayName = if (action.pinchType == PinchScreenType.PINCH_IN) {
                getString(R.string.hint_coordinate_type_pinch_in)
            } else {
                getString(R.string.hint_coordinate_type_pinch_out)
            }

            getString(
                R.string.description_pinch_coordinate_with_description,
                arrayOf(
                    pinchTypeDisplayName,
                    action.fingerCount,
                    action.x,
                    action.y,
                    action.distance,
                    action.duration,
                    action.description,
                ),
            )
        }

        is ActionData.Text -> getString(R.string.description_text_block, action.text)
        is ActionData.Url -> getString(R.string.description_url, action.url)
        is ActionData.Sound.SoundFile -> getString(
            R.string.description_sound,
            action.soundDescription,
        )

        is ActionData.Sound.Ringtone -> {
            getRingtoneLabel(action.uri).handle(
                onSuccess = { getString(R.string.description_sound, it) },
                onError = { getString(R.string.description_sound_unknown) },
            )
        }

        ActionData.AirplaneMode.Disable -> getString(R.string.action_disable_airplane_mode)
        ActionData.AirplaneMode.Enable -> getString(R.string.action_enable_airplane_mode)
        ActionData.AirplaneMode.Toggle -> getString(R.string.action_toggle_airplane_mode)

        ActionData.Bluetooth.Disable -> getString(R.string.action_disable_bluetooth)
        ActionData.Bluetooth.Enable -> getString(R.string.action_enable_bluetooth)
        ActionData.Bluetooth.Toggle -> getString(R.string.action_toggle_bluetooth)

        ActionData.Brightness.Decrease -> getString(R.string.action_decrease_brightness)
        ActionData.Brightness.DisableAuto -> getString(R.string.action_disable_auto_brightness)
        ActionData.Brightness.EnableAuto -> getString(R.string.action_enable_auto_brightness)
        ActionData.Brightness.Increase -> getString(R.string.action_increase_brightness)
        ActionData.Brightness.ToggleAuto -> getString(R.string.action_toggle_auto_brightness)

        ActionData.ConsumeKeyEvent -> getString(R.string.action_consume_keyevent)

        ActionData.ControlMedia.FastForward -> getString(R.string.action_fast_forward)
        ActionData.ControlMedia.NextTrack -> getString(R.string.action_next_track)
        ActionData.ControlMedia.Pause -> getString(R.string.action_pause_media)
        ActionData.ControlMedia.Play -> getString(R.string.action_play_media)
        ActionData.ControlMedia.PlayPause -> getString(R.string.action_play_pause_media)
        ActionData.ControlMedia.PreviousTrack -> getString(R.string.action_previous_track)
        ActionData.ControlMedia.Rewind -> getString(R.string.action_rewind)
        ActionData.ControlMedia.Stop -> getString(R.string.action_stop_media)
        ActionData.ControlMedia.StepForward -> getString(R.string.action_step_forward_media)
        ActionData.ControlMedia.StepBackward -> getString(R.string.action_step_backward_media)

        ActionData.CopyText -> getString(R.string.action_text_copy)
        ActionData.CutText -> getString(R.string.action_text_cut)
        ActionData.PasteText -> getString(R.string.action_text_paste)

        ActionData.DeviceAssistant -> getString(R.string.action_open_device_assistant)

        ActionData.GoBack -> getString(R.string.action_go_back)
        ActionData.GoHome -> getString(R.string.action_go_home)
        ActionData.GoLastApp -> getString(R.string.action_go_last_app)
        ActionData.OpenMenu -> getString(R.string.action_open_menu)
        ActionData.OpenRecents -> getString(R.string.action_open_recents)

        ActionData.HideKeyboard -> getString(R.string.action_hide_keyboard)
        ActionData.LockDevice -> getString(R.string.action_lock_device)

        ActionData.MobileData.Disable -> getString(R.string.action_disable_mobile_data)
        ActionData.MobileData.Enable -> getString(R.string.action_enable_mobile_data)
        ActionData.MobileData.Toggle -> getString(R.string.action_toggle_mobile_data)

        ActionData.MoveCursorToEnd -> getString(R.string.action_move_to_end_of_text)

        ActionData.Nfc.Disable -> getString(R.string.action_nfc_disable)
        ActionData.Nfc.Enable -> getString(R.string.action_nfc_enable)
        ActionData.Nfc.Toggle -> getString(R.string.action_nfc_toggle)

        ActionData.OpenCamera -> getString(R.string.action_open_camera)
        ActionData.OpenSettings -> getString(R.string.action_open_settings)

        is ActionData.Rotation.CycleRotations -> {
            val orientationStrings = action.orientations.map {
                getString(OrientationUtils.getLabel(it))
            }

            getString(
                R.string.action_cycle_rotations_formatted,
                orientationStrings.joinToString(),
            )
        }

        ActionData.Rotation.DisableAuto -> getString(R.string.action_disable_auto_rotate)
        ActionData.Rotation.EnableAuto -> getString(R.string.action_enable_auto_rotate)
        ActionData.Rotation.Landscape -> getString(R.string.action_landscape_mode)
        ActionData.Rotation.Portrait -> getString(R.string.action_portrait_mode)
        ActionData.Rotation.SwitchOrientation -> getString(R.string.action_switch_orientation)
        ActionData.Rotation.ToggleAuto -> getString(R.string.action_toggle_auto_rotate)

        ActionData.ScreenOnOff -> getString(R.string.action_power_on_off_device)
        ActionData.Screenshot -> getString(R.string.action_screenshot)
        ActionData.SecureLock -> getString(R.string.action_secure_lock_device)
        ActionData.SelectWordAtCursor -> getString(R.string.action_select_word_at_cursor)
        ActionData.ShowKeyboard -> getString(R.string.action_show_keyboard)
        ActionData.ShowKeyboardPicker -> getString(R.string.action_show_keyboard_picker)
        ActionData.ShowPowerMenu -> getString(R.string.action_show_power_menu)

        ActionData.StatusBar.Collapse -> getString(R.string.action_collapse_status_bar)
        ActionData.StatusBar.ExpandNotifications -> getString(R.string.action_expand_notification_drawer)
        ActionData.StatusBar.ExpandQuickSettings -> getString(R.string.action_expand_quick_settings)
        ActionData.StatusBar.ToggleNotifications -> getString(R.string.action_toggle_notification_drawer)
        ActionData.StatusBar.ToggleQuickSettings -> getString(R.string.action_toggle_quick_settings)

        ActionData.ToggleKeyboard -> getString(R.string.action_toggle_keyboard)
        ActionData.ToggleSplitScreen -> getString(R.string.action_toggle_split_screen)
        ActionData.VoiceAssistant -> getString(R.string.action_open_assistant)

        ActionData.Wifi.Disable -> getString(R.string.action_disable_wifi)
        ActionData.Wifi.Enable -> getString(R.string.action_enable_wifi)
        ActionData.Wifi.Toggle -> getString(R.string.action_toggle_wifi)
        ActionData.DismissAllNotifications -> getString(R.string.action_dismiss_all_notifications)
        ActionData.DismissLastNotification -> getString(R.string.action_dismiss_most_recent_notification)

        ActionData.AnswerCall -> getString(R.string.action_answer_call)
        ActionData.EndCall -> getString(R.string.action_end_call)

        ActionData.DeviceControls -> getString(R.string.action_device_controls)
        is ActionData.HttpRequest -> action.description

        is ActionData.InteractUiElement -> action.description
    }

    fun getIcon(action: ActionData): ComposeIconInfo = when (action) {
        is ActionData.App ->
            getAppIcon(action.packageName).handle(
                onSuccess = { ComposeIconInfo.Drawable(it) },
                onError = { ComposeIconInfo.Vector(Icons.Outlined.Android) },
            )

        is ActionData.AppShortcut -> {
            if (action.packageName.isNullOrBlank()) {
                ComposeIconInfo.Vector(Icons.Outlined.Android)
            } else {
                getAppIcon(action.packageName).handle(
                    onSuccess = { ComposeIconInfo.Drawable(it) },
                    onError = { ComposeIconInfo.Vector(Icons.Outlined.Android) },
                )
            }
        }

        else -> ComposeIconInfo.Vector(ActionUtils.getComposeIcon(action.id))
    }

    fun getDrawableIcon(action: ActionData): IconInfo? = when (action) {
        is ActionData.InputKeyEvent -> null

        is ActionData.App ->
            getAppIcon(action.packageName).handle(
                onSuccess = { IconInfo(it, TintType.None) },
                onError = { null },
            )

        is ActionData.AppShortcut -> {
            if (action.packageName.isNullOrBlank()) {
                null
            } else {
                getAppIcon(action.packageName).handle(
                    onSuccess = { IconInfo(it, TintType.None) },
                    onError = { null },
                )
            }
        }

        is ActionData.Intent -> null

        is ActionData.PhoneCall ->
            IconInfo(
                getDrawable(R.drawable.ic_outline_call_24),
                tintType = TintType.OnSurface,
            )

        is ActionData.TapScreen -> IconInfo(
            getDrawable(R.drawable.ic_outline_touch_app_24),
            TintType.OnSurface,
        )

        is ActionData.Text -> null
        is ActionData.Url -> null
        is ActionData.Sound -> IconInfo(
            getDrawable(R.drawable.ic_outline_volume_up_24),
            TintType.OnSurface,
        )

        else -> ActionUtils.getDrawableIcon(action.id)?.let { iconRes ->
            IconInfo(
                getDrawable(iconRes),
                TintType.OnSurface,
            )
        }
    }

    fun getOptionLabels(keyMap: KeyMap, action: Action) = buildList {
        if (keyMap.isRepeatingActionsAllowed() && action.repeat) {
            val repeatDescription = buildString {
                append(getString(R.string.flag_repeat_build_description_start))

                val repeatLimit = when {
                    action.repeatLimit != null -> action.repeatLimit
                    action.repeatMode == RepeatMode.LIMIT_REACHED -> 1 // and is null
                    else -> null
                }

                if (repeatLimit != null) {
                    append(" ")
                    append(getString(R.string.flag_repeat_build_description_limit, repeatLimit))
                }

                if (action.repeatRate != null) {
                    append(" ")
                    append(
                        getString(
                            R.string.flag_repeat_build_description_repeat_rate,
                            action.repeatRate,
                        ),
                    )
                }

                if (action.repeatDelay != null) {
                    append(" ")
                    append(
                        getString(
                            R.string.flag_repeat_build_description_repeat_delay,
                            action.repeatDelay,
                        ),
                    )
                }

                append(" ")

                when (action.repeatMode) {
                    RepeatMode.TRIGGER_RELEASED -> {
                        append(getString(R.string.flag_repeat_build_description_until_released))
                    }

                    RepeatMode.TRIGGER_PRESSED_AGAIN -> {
                        append(getString(R.string.flag_repeat_build_description_until_pressed_again))
                    }

                    else -> Unit
                }
            }

            add(repeatDescription)
        }

        if (keyMap.isHoldingDownActionAllowed(action) &&
            action.holdDown &&
            !action.stopHoldDownWhenTriggerPressedAgain
        ) {
            add(getString(R.string.flag_hold_down))
        }

        if (keyMap.isHoldingDownActionAllowed(action) &&
            action.holdDown &&
            action.stopHoldDownWhenTriggerPressedAgain
        ) {
            add(getString(R.string.flag_hold_down_until_pressed_again))
        }
    }
}
