package io.github.sds100.keymapper.actions

import android.text.InputType
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.camera.CameraLensUtils
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.display.OrientationUtils
import io.github.sds100.keymapper.system.intents.ConfigIntentResult
import io.github.sds100.keymapper.system.volume.*
import io.github.sds100.keymapper.util.ui.*

/**
 * Created by sds100 on 26/07/2021.
 */

class CreateActionViewModelImpl(
    private val useCase: CreateActionUseCase,
    resourceProvider: ResourceProvider
) : CreateActionViewModel,
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    override suspend fun editAction(oldData: ActionData): ActionData? {
        if (!oldData.isEditable()) {
            throw IllegalArgumentException("This action ${oldData.javaClass.name} can't be edited!")
        }

        return configAction(oldData.id, oldData)
    }

    override suspend fun createAction(id: ActionId): ActionData? {
        return configAction(id)
    }

    private suspend fun configAction(actionId: ActionId, oldData: ActionData? = null): ActionData? {
        when (actionId) {
            ActionId.SWITCH_KEYBOARD -> {
                val inputMethods = useCase.getInputMethods()
                val items = inputMethods.map {
                    it.id to it.label
                }

                val imeId =
                    showPopup("choose_ime", PopupUi.SingleChoice(items)) ?: return null
                val imeName = inputMethods.single { it.id == imeId }.label

                return SwitchKeyboardAction(imeId, imeName)
            }

            ActionId.PLAY_PAUSE_MEDIA_PACKAGE,
            ActionId.PLAY_MEDIA_PACKAGE,
            ActionId.PAUSE_MEDIA_PACKAGE,
            ActionId.NEXT_TRACK_PACKAGE,
            ActionId.PREVIOUS_TRACK_PACKAGE,
            ActionId.FAST_FORWARD_PACKAGE,
            ActionId.REWIND_PACKAGE,
            -> {
                val packageName =
                    navigate("choose_app_for_media_action", NavDestination.ChooseApp)
                        ?: return null

                val action = when (actionId) {
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
                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.VOLUME_UP,
            ActionId.VOLUME_DOWN,
            ActionId.VOLUME_MUTE,
            ActionId.VOLUME_UNMUTE,
            ActionId.VOLUME_TOGGLE_MUTE -> {

                val showVolumeUiId = 0
                val isVolumeUiChecked =
                    when (oldData) {
                        is VolumeAction.Up -> oldData.showVolumeUi
                        is VolumeAction.Down -> oldData.showVolumeUi
                        is VolumeAction.Mute -> oldData.showVolumeUi
                        is VolumeAction.UnMute -> oldData.showVolumeUi
                        is VolumeAction.ToggleMute -> oldData.showVolumeUi
                        else -> false
                    }

                val dialogItems = listOf(
                    MultiChoiceItem(
                        showVolumeUiId,
                        getString(R.string.flag_show_volume_dialog),
                        isVolumeUiChecked
                    )
                )

                val showVolumeUiDialog = PopupUi.MultiChoice(items = dialogItems)

                val chosenFlags = showPopup("show_volume_ui", showVolumeUiDialog) ?: return null

                val showVolumeUi = chosenFlags.contains(showVolumeUiId)

                val action = when (actionId) {
                    ActionId.VOLUME_UP -> VolumeAction.Up(showVolumeUi)
                    ActionId.VOLUME_DOWN -> VolumeAction.Down(showVolumeUi)
                    ActionId.VOLUME_MUTE -> VolumeAction.Mute(showVolumeUi)
                    ActionId.VOLUME_UNMUTE -> VolumeAction.UnMute(showVolumeUi)
                    ActionId.VOLUME_TOGGLE_MUTE -> VolumeAction.ToggleMute(
                        showVolumeUi
                    )
                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.VOLUME_INCREASE_STREAM,
            ActionId.VOLUME_DECREASE_STREAM -> {

                val showVolumeUiId = 0
                val isVolumeUiChecked = if (oldData is VolumeAction.Stream) {
                    oldData.showVolumeUi
                } else {
                    false
                }

                val dialogItems = listOf(
                    MultiChoiceItem(
                        showVolumeUiId,
                        getString(R.string.flag_show_volume_dialog),
                        isVolumeUiChecked
                    )
                )

                val showVolumeUiDialog = PopupUi.MultiChoice(items = dialogItems)

                val chosenFlags =
                    showPopup("show_volume_ui", showVolumeUiDialog) ?: return null

                val showVolumeUi = chosenFlags.contains(showVolumeUiId)

                val items = VolumeStream.values()
                    .map { it to getString(VolumeStreamUtils.getLabel(it)) }

                val stream = showPopup("pick_volume_stream", PopupUi.SingleChoice(items))
                    ?: return null

                val action = when (actionId) {
                    ActionId.VOLUME_INCREASE_STREAM ->
                        VolumeAction.Stream.Increase(showVolumeUi = showVolumeUi, stream)

                    ActionId.VOLUME_DECREASE_STREAM ->
                        VolumeAction.Stream.Decrease(showVolumeUi = showVolumeUi, stream)

                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.CHANGE_RINGER_MODE -> {
                val items = RingerMode.values()
                    .map { it to getString(RingerModeUtils.getLabel(it)) }

                val ringerMode =
                    showPopup("pick_ringer_mode", PopupUi.SingleChoice(items))
                        ?: return null

                return VolumeAction.SetRingerMode(ringerMode)
            }

            //don't need to show options for disabling do not disturb
            ActionId.TOGGLE_DND_MODE,
            ActionId.ENABLE_DND_MODE -> {
                val items = DndMode.values()
                    .map { it to getString(DndModeUtils.getLabel(it)) }

                val dndMode =
                    showPopup("pick_dnd_mode", PopupUi.SingleChoice(items))
                        ?: return null

                val action = when (actionId) {
                    ActionId.TOGGLE_DND_MODE -> DndModeAction.Toggle(dndMode)

                    ActionId.ENABLE_DND_MODE -> DndModeAction.Enable(dndMode)

                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.CYCLE_ROTATIONS -> {
                val items = Orientation.values().map { orientation ->
                    val isChecked = if (oldData is RotationAction.CycleRotations) {
                        oldData.orientations.contains(orientation)
                    } else {
                        false
                    }

                    MultiChoiceItem(
                        orientation, getString(OrientationUtils.getLabel(orientation)),
                        isChecked
                    )
                }

                val orientations =
                    showPopup("pick_orientations", PopupUi.MultiChoice(items)) ?: return null

                return RotationAction.CycleRotations(orientations)
            }

            ActionId.TOGGLE_FLASHLIGHT,
            ActionId.ENABLE_FLASHLIGHT,
            ActionId.DISABLE_FLASHLIGHT -> {
                val items = CameraLens.values().map {
                    it to getString(CameraLensUtils.getLabel(it))
                }

                val lens = showPopup("pick_lens", PopupUi.SingleChoice(items))
                    ?: return null

                val action = when (actionId) {
                    ActionId.TOGGLE_FLASHLIGHT -> FlashlightAction.Toggle(lens)
                    ActionId.ENABLE_FLASHLIGHT -> FlashlightAction.Enable(lens)
                    ActionId.DISABLE_FLASHLIGHT -> FlashlightAction.Disable(lens)
                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.APP -> {
                val packageName =
                    navigate("choose_app_for_app_action", NavDestination.ChooseApp)
                        ?: return null

                return OpenAppAction(packageName)
            }

            ActionId.APP_SHORTCUT -> {
                val appShortcutResult =
                    navigate("choose_app_shortcut", NavDestination.ChooseAppShortcut)
                        ?: return null

                return OpenAppShortcutAction(
                    appShortcutResult.packageName,
                    appShortcutResult.shortcutName,
                    appShortcutResult.uri
                )
            }

            ActionId.KEY_CODE -> {
                val keyCode =
                    navigate("choose_key_code", NavDestination.ChooseKeyCode)
                        ?: return null

                return KeyEventAction(keyCode = keyCode)
            }

            ActionId.KEY_EVENT -> {
                val keyEventAction = if (oldData is KeyEventAction) {
                    navigate("config_key_event", NavDestination.ConfigKeyEventAction(oldData))
                } else {
                    navigate("config_key_event", NavDestination.ConfigKeyEventAction())
                }

                return keyEventAction
            }

            ActionId.TAP_SCREEN -> {
                val oldResult = if (oldData is TapCoordinateAction) {
                    PickCoordinateResult(
                        oldData.x,
                        oldData.y,
                        oldData.description ?: ""
                    )
                } else {
                    null
                }

                val result = navigate(
                    "pick_display_coordinate_for_action",
                    NavDestination.PickCoordinate(oldResult)
                ) ?: return null

                val description = if (result.description.isEmpty()) {
                    null
                } else {
                    result.description
                }

                return TapCoordinateAction(
                    result.x,
                    result.y,
                    description
                )
            }

            ActionId.TEXT -> {
                val oldText = if (oldData is TextAction) {
                    oldData.text
                } else {
                    ""
                }

                val text = showPopup(
                    "create_text_action", PopupUi.Text(
                        hint = getString(R.string.hint_create_text_action),
                        allowEmpty = false,
                        text = oldText
                    )
                ) ?: return null

                return TextAction(text)
            }

            ActionId.URL -> {
                val oldUrl = if (oldData is UrlAction) {
                    oldData.url
                } else {
                    ""
                }

                val text = showPopup(
                    "create_url_action", PopupUi.Text(
                        hint = getString(R.string.hint_create_url_action),
                        allowEmpty = false,
                        inputType = InputType.TYPE_TEXT_VARIATION_URI,
                        text = oldUrl
                    )
                ) ?: return null

                return UrlAction(text)
            }

            ActionId.INTENT -> {
                val oldIntent = if (oldData is IntentAction) {
                    ConfigIntentResult(
                        oldData.uri,
                        oldData.target,
                        oldData.description
                    )
                } else {
                    null
                }

                val result = navigate(
                    "config_intent",
                    NavDestination.ConfigIntent(oldIntent)
                ) ?: return null

                return IntentAction(
                    description = result.description,
                    target = result.target,
                    uri = result.uri
                )
            }

            ActionId.PHONE_CALL -> {
                val oldText = if (oldData is PhoneCallAction) {
                    oldData.number
                } else {
                    ""
                }

                val text = showPopup(
                    "create_phone_call_action", PopupUi.Text(
                        hint = getString(R.string.hint_create_phone_call_action),
                        allowEmpty = false,
                        inputType = InputType.TYPE_CLASS_PHONE,
                        text = oldText,
                    )
                ) ?: return null

                return PhoneCallAction(text)
            }

            ActionId.SOUND -> {
                val result = navigate(
                    "choose_sound_file",
                    NavDestination.ChooseSound
                ) ?: return null

                return SoundAction(
                    soundUid = result.soundUid,
                    soundDescription = result.description
                )
            }

            ActionId.TOGGLE_WIFI -> return WifiAction.Toggle
            ActionId.ENABLE_WIFI -> return WifiAction.Enable
            ActionId.DISABLE_WIFI -> return WifiAction.Disable

            ActionId.TOGGLE_BLUETOOTH -> return BluetoothAction.Toggle
            ActionId.ENABLE_BLUETOOTH -> return BluetoothAction.Enable
            ActionId.DISABLE_BLUETOOTH -> return BluetoothAction.Disable

            ActionId.TOGGLE_MOBILE_DATA -> return MobileDataAction.Toggle
            ActionId.ENABLE_MOBILE_DATA -> return MobileDataAction.Enable
            ActionId.DISABLE_MOBILE_DATA -> return MobileDataAction.Disable

            ActionId.TOGGLE_AUTO_BRIGHTNESS -> return BrightnessAction.ToggleAuto
            ActionId.DISABLE_AUTO_BRIGHTNESS -> return BrightnessAction.DisableAuto
            ActionId.ENABLE_AUTO_BRIGHTNESS -> return BrightnessAction.EnableAuto
            ActionId.INCREASE_BRIGHTNESS -> return BrightnessAction.Increase
            ActionId.DECREASE_BRIGHTNESS -> return BrightnessAction.Decrease

            ActionId.TOGGLE_AUTO_ROTATE -> return RotationAction.ToggleAuto
            ActionId.ENABLE_AUTO_ROTATE -> return RotationAction.EnableAuto
            ActionId.DISABLE_AUTO_ROTATE -> return RotationAction.DisableAuto
            ActionId.PORTRAIT_MODE -> return RotationAction.Portrait
            ActionId.LANDSCAPE_MODE -> return RotationAction.Landscape
            ActionId.SWITCH_ORIENTATION -> return RotationAction.SwitchOrientation

            ActionId.VOLUME_SHOW_DIALOG -> return VolumeAction.ShowDialog
            ActionId.CYCLE_RINGER_MODE -> return VolumeAction.CycleRingerMode
            ActionId.CYCLE_VIBRATE_RING -> return VolumeAction.CycleVibrateRing

            ActionId.EXPAND_NOTIFICATION_DRAWER -> return StatusBarAction.ExpandNotifications
            ActionId.TOGGLE_NOTIFICATION_DRAWER -> return StatusBarAction.ToggleNotifications
            ActionId.EXPAND_QUICK_SETTINGS -> return StatusBarAction.ExpandQuickSettings
            ActionId.TOGGLE_QUICK_SETTINGS -> return StatusBarAction.ToggleQuickSettings
            ActionId.COLLAPSE_STATUS_BAR -> return StatusBarAction.Collapse

            ActionId.PAUSE_MEDIA -> return ControlMediaAction.Pause
            ActionId.PLAY_MEDIA -> return ControlMediaAction.Play
            ActionId.PLAY_PAUSE_MEDIA -> return ControlMediaAction.PlayPause
            ActionId.NEXT_TRACK -> return ControlMediaAction.NextTrack
            ActionId.PREVIOUS_TRACK -> return ControlMediaAction.PreviousTrack
            ActionId.FAST_FORWARD -> return ControlMediaAction.FastForward
            ActionId.REWIND -> return ControlMediaAction.Rewind

            ActionId.GO_BACK -> return GoBackAction
            ActionId.GO_HOME -> return GoHomeAction
            ActionId.OPEN_RECENTS -> return OpenRecentsAction
            ActionId.TOGGLE_SPLIT_SCREEN -> return ToggleSplitScreenAction
            ActionId.GO_LAST_APP -> return GoLastAppAction
            ActionId.OPEN_MENU -> return OpenMenuAction

            ActionId.ENABLE_NFC -> return NfcAction.Enable
            ActionId.DISABLE_NFC -> return NfcAction.Disable
            ActionId.TOGGLE_NFC -> return NfcAction.Toggle

            ActionId.MOVE_CURSOR_TO_END -> return MoveCursorToEndAction
            ActionId.TOGGLE_KEYBOARD -> return ToggleKeyboardAction
            ActionId.SHOW_KEYBOARD -> return ShowKeyboardAction
            ActionId.HIDE_KEYBOARD -> return HideKeyboardAction
            ActionId.SHOW_KEYBOARD_PICKER -> return ShowKeyboardPickerAction
            ActionId.TEXT_CUT -> return CutTextAction
            ActionId.TEXT_COPY -> return CopyTextAction
            ActionId.TEXT_PASTE -> return PasteTextAction
            ActionId.SELECT_WORD_AT_CURSOR -> return SelectWordAtCursorAction

            ActionId.TOGGLE_AIRPLANE_MODE -> return AirplaneModeAction.Toggle
            ActionId.ENABLE_AIRPLANE_MODE -> return AirplaneModeAction.Enable
            ActionId.DISABLE_AIRPLANE_MODE -> return AirplaneModeAction.Disable

            ActionId.SCREENSHOT -> return ScreenshotAction
            ActionId.OPEN_VOICE_ASSISTANT -> return VoiceAssistantAction
            ActionId.OPEN_DEVICE_ASSISTANT -> return DeviceAssistantAction

            ActionId.OPEN_CAMERA -> return OpenCameraAction
            ActionId.LOCK_DEVICE -> return LockDeviceAction
            ActionId.POWER_ON_OFF_DEVICE -> return ScreenOnOffAction
            ActionId.SECURE_LOCK_DEVICE -> return SecureLockAction
            ActionId.CONSUME_KEY_EVENT -> return ConsumeKeyEventAction
            ActionId.OPEN_SETTINGS -> return OpenSettingsAction
            ActionId.SHOW_POWER_MENU -> return ShowPowerMenuAction
            ActionId.DISABLE_DND_MODE -> return DndModeAction.Disable
            ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> return DismissLastNotificationAction
            ActionId.DISMISS_ALL_NOTIFICATIONS -> return DismissAllNotificationsAction
        }
    }
}

interface CreateActionViewModel : PopupViewModel, NavigationViewModel {
    suspend fun editAction(oldData: ActionData): ActionData?
    suspend fun createAction(id: ActionId): ActionData?
}