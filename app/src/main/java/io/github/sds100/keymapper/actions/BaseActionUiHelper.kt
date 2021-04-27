package io.github.sds100.keymapper.actions

import android.view.KeyEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.mappings.Mapping
import io.github.sds100.keymapper.system.camera.CameraLensUtils
import io.github.sds100.keymapper.system.display.OrientationUtils
import io.github.sds100.keymapper.system.volume.DndModeUtils
import io.github.sds100.keymapper.system.volume.RingerModeUtils
import io.github.sds100.keymapper.system.volume.VolumeStreamUtils
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.system.intents.IntentTarget
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.handle
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 18/03/2021.
 */

abstract class BaseActionUiHelper<MAPPING: Mapping<A>,A: Action>(
    displayActionUseCase: DisplayActionUseCase,
    resourceProvider: ResourceProvider
) : ActionUiHelper<MAPPING, A>, ResourceProvider by resourceProvider,
    DisplayActionUseCase by displayActionUseCase {

    override fun getTitle(action: ActionData): String = when (action) {
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

                    getString(
                        R.string.description_keyevent_from_device,
                        arrayOf(metaStateString, keyCodeString, action.device.name)
                    )
                } else {
                    getString(
                        R.string.description_keyevent,
                        args = arrayOf(metaStateString, keyCodeString)
                    )
                }
            }
        }

        is SimpleSystemAction -> getString(SystemActionUtils.getTitle(action.id))

        is EnableDndMode ->{
            val dndModeString = getString(DndModeUtils.getLabel(action.dndMode))
            getString(
                R.string.action_enable_dnd_mode_formatted,
                dndModeString
            )
        }

        is ToggleDndMode ->{
            val dndModeString = getString(DndModeUtils.getLabel(action.dndMode))
            getString(
                R.string.action_toggle_dnd_mode_formatted,
                dndModeString
            )
        }

        is ChangeRingerModeSystemAction -> {
            val ringerModeString = getString(RingerModeUtils.getLabel(action.ringerMode))

            getString(R.string.action_change_ringer_mode_formatted, ringerModeString)
        }

        is VolumeSystemAction -> {
            val string = when (action) {
                is VolumeSystemAction.Stream -> {
                    val streamString = getString(
                        VolumeStreamUtils.getLabel(action.volumeStream)
                    )

                    when (action) {
                        is VolumeSystemAction.Stream.Decrease -> getString(
                            R.string.action_decrease_stream_formatted,
                            streamString
                        )

                        is VolumeSystemAction.Stream.Increase -> getString(
                            R.string.action_increase_stream_formatted,
                            streamString
                        )
                    }
                }

                is VolumeSystemAction.Down -> getString(R.string.action_volume_down)
                is VolumeSystemAction.Mute -> getString(R.string.action_volume_mute)
                is VolumeSystemAction.ToggleMute -> getString(R.string.action_toggle_mute)
                is VolumeSystemAction.UnMute -> getString(R.string.action_volume_unmute)
                is VolumeSystemAction.Up -> getString(R.string.action_volume_up)
            }

            if (action.showVolumeUi) {
                val midDot = getString(R.string.middot)
                "$string $midDot ${getString(R.string.flag_show_volume_dialog)}"
            } else {
                string
            }
        }

        is ControlMediaForAppSystemAction ->
            getAppName(action.packageName).handle(
                onSuccess = { appName ->
                    val resId = when (action) {
                        is ControlMediaForAppSystemAction.Play -> R.string.action_play_media_package_formatted
                        is ControlMediaForAppSystemAction.FastForward -> R.string.action_fast_forward_package_formatted
                        is ControlMediaForAppSystemAction.NextTrack -> R.string.action_next_track_package_formatted
                        is ControlMediaForAppSystemAction.Pause -> R.string.action_pause_media_package_formatted
                        is ControlMediaForAppSystemAction.PlayPause -> R.string.action_play_pause_media_package_formatted
                        is ControlMediaForAppSystemAction.PreviousTrack -> R.string.action_previous_track_package_formatted
                        is ControlMediaForAppSystemAction.Rewind -> R.string.action_rewind_package_formatted
                    }

                    getString(resId, appName)
                },
                onError = {
                    val resId = when (action) {
                        is ControlMediaForAppSystemAction.Play -> R.string.action_play_media_package
                        is ControlMediaForAppSystemAction.FastForward -> R.string.action_fast_forward_package
                        is ControlMediaForAppSystemAction.NextTrack -> R.string.action_next_track_package
                        is ControlMediaForAppSystemAction.Pause -> R.string.action_pause_media_package
                        is ControlMediaForAppSystemAction.PlayPause -> R.string.action_play_pause_media_package
                        is ControlMediaForAppSystemAction.PreviousTrack -> R.string.action_previous_track_package
                        is ControlMediaForAppSystemAction.Rewind -> R.string.action_rewind_package
                    }

                    getString(resId)
                }
            )

        is CycleRotationsSystemAction -> {
            val orientationStrings = action.orientations.map {
                getString(OrientationUtils.getLabel(it))
            }

            getString(
                R.string.action_cycle_rotations_formatted,
                orientationStrings.joinToString()
            )
        }

        is FlashlightSystemAction -> {
            val resId = when (action) {
                is FlashlightSystemAction.Toggle -> R.string.action_toggle_flashlight_formatted
                is FlashlightSystemAction.Enable -> R.string.action_enable_flashlight_formatted
                is FlashlightSystemAction.Disable -> R.string.action_disable_flashlight_formatted
            }

            val lensString = getString(CameraLensUtils.getLabel(action.lens))

            getString(resId, lensString)
        }

        is SwitchKeyboardSystemAction -> getInputMethodLabel(action.imeId).handle(
            onSuccess = { getString(R.string.action_switch_keyboard_formatted, it) },
            onError = { getString(R.string.action_switch_keyboard_formatted, action.savedImeName) }
        )

        is CorruptAction -> ""
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
    }

    override fun getIcon(action: ActionData): IconInfo? = when (action) {
        CorruptAction -> null

        is KeyEventAction -> null

        is OpenAppAction ->
            getAppIcon(action.packageName).handle(
                onSuccess = { IconInfo(it, TintType.NONE) },
                onError = { null }
            )

        is OpenAppShortcutAction -> {
            if (action.packageName.isNullOrBlank()) {
                null
            } else {
                getAppIcon(action.packageName).handle(
                    onSuccess = { IconInfo(it, TintType.NONE) },
                    onError = { null }
                )
            }
        }

        is SystemAction ->
            SystemActionUtils.getIcon(action.id)?.let {
                IconInfo(getDrawable(it), TintType.ON_SURFACE)
            }

        is IntentAction -> null

        is PhoneCallAction ->
            IconInfo(
                getDrawable(R.drawable.ic_outline_call_24),
                tintType = TintType.ON_SURFACE
            )

        is TapCoordinateAction -> IconInfo(
            getDrawable(R.drawable.ic_outline_touch_app_24),
            TintType.ON_SURFACE
        )

        is TextAction -> null
        is UrlAction -> null
    }
}

interface ActionUiHelper<MAPPING: Mapping<A>, A: Action> {
    fun getTitle(action: ActionData): String
    fun getOptionLabels(mapping: MAPPING, action: A): List<String>
    fun getIcon(action: ActionData): IconInfo?
    fun getError(action: ActionData): Error?
}