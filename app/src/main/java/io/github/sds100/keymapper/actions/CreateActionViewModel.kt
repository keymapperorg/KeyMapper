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
import javax.inject.Inject

/**
 * Created by sds100 on 26/07/2021.
 */

class CreateActionViewModelImpl @Inject constructor(
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

                val dialog = PopupUi.SingleChoice(title = getString(R.string.dialog_title_choose_keyboard), items)
                val imeId = showPopup("choose_ime", dialog) ?: return null

                val imeName = inputMethods.single { it.id == imeId }.label

                return ActionData.SwitchKeyboard(imeId, imeName)
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
                    navigate("choose_app_for_media_action", NavDestination.ChooseApp(allowHiddenApps = true))
                        ?: return null

                val action = when (actionId) {
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
                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.VOLUME_UP,
            ActionId.VOLUME_DOWN,
            ActionId.VOLUME_MUTE,
            ActionId.VOLUME_UNMUTE,
            ActionId.VOLUME_TOGGLE_MUTE -> {
                val isShowVolumeUiChecked = if (oldData is ActionData.Volume) {
                    oldData.showVolumeUi
                } else {
                    false
                }
                val showVolumeUi = chooseWhetherToShowVolumeUi(isShowVolumeUiChecked)
                val volumeStream = chooseVolumeStream()

                val action = when (actionId) {
                    ActionId.VOLUME_UP -> ActionData.Volume.Up(showVolumeUi, volumeStream)
                    ActionId.VOLUME_DOWN -> ActionData.Volume.Down(showVolumeUi, volumeStream)
                    ActionId.VOLUME_MUTE -> ActionData.Volume.Mute(showVolumeUi, volumeStream)
                    ActionId.VOLUME_UNMUTE -> ActionData.Volume.UnMute(showVolumeUi, volumeStream)
                    ActionId.VOLUME_TOGGLE_MUTE -> ActionData.Volume.ToggleMute(showVolumeUi, volumeStream)
                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.CHANGE_RINGER_MODE -> {
                val items = RingerMode.values()
                    .map { it to getString(RingerModeUtils.getLabel(it)) }

                val dialog = PopupUi.SingleChoice(title = getString(R.string.dialog_title_choose_ringer_mode), items)
                val ringerMode = showPopup("pick_ringer_mode", dialog) ?: return null

                return ActionData.SetRingerMode(ringerMode)
            }

            //don't need to show options for disabling do not disturb
            ActionId.TOGGLE_DND_MODE,
            ActionId.ENABLE_DND_MODE -> {
                val items = DndMode.values()
                    .map { it to getString(DndModeUtils.getLabel(it)) }

                val dialog = PopupUi.SingleChoice(title = getString(R.string.dialog_title_choose_dnd_mode), items)
                val dndMode = showPopup("pick_dnd_mode", dialog) ?: return null

                val action = when (actionId) {
                    ActionId.TOGGLE_DND_MODE -> ActionData.DoNotDisturb.Toggle(dndMode)

                    ActionId.ENABLE_DND_MODE -> ActionData.DoNotDisturb.Enable(dndMode)

                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.CYCLE_ROTATIONS -> {
                val items = Orientation.values().map { orientation ->
                    val isChecked = if (oldData is ActionData.Rotation.CycleRotations) {
                        oldData.orientations.contains(orientation)
                    } else {
                        false
                    }

                    MultiChoiceItem(
                        orientation, getString(OrientationUtils.getLabel(orientation)),
                        isChecked
                    )
                }

                val dialog = PopupUi.MultiChoice(
                    title = getString(R.string.choose_action_cycle_orientations_dialog_title),
                    items
                )
                val orientations = showPopup("pick_orientations", dialog) ?: return null

                return ActionData.Rotation.CycleRotations(orientations)
            }

            ActionId.TOGGLE_FLASHLIGHT,
            ActionId.ENABLE_FLASHLIGHT,
            ActionId.DISABLE_FLASHLIGHT -> {
                val items = CameraLens.values().map {
                    it to getString(CameraLensUtils.getLabel(it))
                }

                val dialog = PopupUi.SingleChoice(getString(R.string.dialog_title_choose_camera_flash), items)
                val lens = showPopup("pick_lens", dialog) ?: return null

                val action = when (actionId) {
                    ActionId.TOGGLE_FLASHLIGHT -> ActionData.Flashlight.Toggle(lens)
                    ActionId.ENABLE_FLASHLIGHT -> ActionData.Flashlight.Enable(lens)
                    ActionId.DISABLE_FLASHLIGHT -> ActionData.Flashlight.Disable(lens)
                    else -> throw Exception("don't know how to create action for $actionId")
                }

                return action
            }

            ActionId.APP -> {
                val packageName =
                    navigate("choose_app_for_app_action", NavDestination.ChooseApp(allowHiddenApps = false))
                        ?: return null

                return ActionData.App(packageName)
            }

            ActionId.APP_SHORTCUT -> {
                val appShortcutResult =
                    navigate("choose_app_shortcut", NavDestination.ChooseAppShortcut)
                        ?: return null

                return ActionData.AppShortcut(
                    appShortcutResult.packageName,
                    appShortcutResult.shortcutName,
                    appShortcutResult.uri
                )
            }

            ActionId.KEY_CODE -> {
                val keyCode =
                    navigate("choose_key_code", NavDestination.ChooseKeyCode)
                        ?: return null

                return ActionData.InputKeyEvent(keyCode = keyCode)
            }

            ActionId.KEY_EVENT -> {
                val keyEventAction = if (oldData is ActionData.InputKeyEvent) {
                    navigate("config_key_event", NavDestination.ConfigKeyEventAction(oldData))
                } else {
                    navigate("config_key_event", NavDestination.ConfigKeyEventAction())
                }

                return keyEventAction
            }

            ActionId.TAP_SCREEN -> {
                val oldResult = if (oldData is ActionData.TapScreen) {
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

                return ActionData.TapScreen(
                    result.x,
                    result.y,
                    description
                )
            }

            ActionId.TEXT -> {
                val oldText = if (oldData is ActionData.Text) {
                    oldData.text
                } else {
                    ""
                }

                val text = showPopup(
                    "create_text_action", PopupUi.Text(
                        hint = getString(R.string.hint_create_text_action),
                        allowEmpty = false,
                        allowBlank = true, // allow blank strings in case the user wants to input blank chars
                        text = oldText
                    )
                ) ?: return null

                return ActionData.Text(text)
            }

            ActionId.URL -> {
                val oldUrl = if (oldData is ActionData.Url) {
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

                return ActionData.Url(text)
            }

            ActionId.INTENT -> {
                val oldIntent = if (oldData is ActionData.Intent) {
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

                return ActionData.Intent(
                    description = result.description,
                    target = result.target,
                    uri = result.uri
                )
            }

            ActionId.PHONE_CALL -> {
                val oldText = if (oldData is ActionData.PhoneCall) {
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

                return ActionData.PhoneCall(text)
            }

            ActionId.SOUND -> {
                val result = navigate(
                    "choose_sound_file",
                    NavDestination.ChooseSound
                ) ?: return null

                return ActionData.Sound(
                    soundUid = result.soundUid,
                    soundDescription = result.name
                )
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
            ActionId.DISABLE_DND_MODE -> return ActionData.DoNotDisturb.Disable
            ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> return ActionData.DismissLastNotification
            ActionId.DISMISS_ALL_NOTIFICATIONS -> return ActionData.DismissAllNotifications
            ActionId.ANSWER_PHONE_CALL -> return ActionData.AnswerCall
            ActionId.END_PHONE_CALL -> return ActionData.EndCall
        }
    }

    private suspend fun chooseWhetherToShowVolumeUi(isChecked: Boolean): Boolean {
        val showVolumeUiId = 0

        val showVolumeUiDialogItem = MultiChoiceItem(
            showVolumeUiId,
            getString(R.string.flag_show_volume_dialog),
            isChecked
        )

        val showVolumeUiDialog: PopupUi.MultiChoice<Int> = PopupUi.MultiChoice(
            title = getString(R.string.dialog_title_choose_to_show_volume_ui),
            items = listOf(showVolumeUiDialogItem)
        )

        val chosenFlags = showPopup("show_volume_ui", showVolumeUiDialog) ?: emptyList()

        return chosenFlags.contains(showVolumeUiId)
    }

    private suspend fun chooseVolumeStream(): VolumeStream {
        val items = VolumeStream.values()
            .map { it to getString(VolumeStreamUtils.getLabel(it)) }

        val dialog = PopupUi.SingleChoice(
            title = getString(R.string.dialog_title_pick_stream),
            items
        )

        return showPopup("pick_volume_stream", dialog) ?: VolumeStream.DEFAULT
    }
}

interface CreateActionViewModel : PopupViewModel, NavigationViewModel {
    suspend fun editAction(oldData: ActionData): ActionData?
    suspend fun createAction(id: ActionId): ActionData?
}