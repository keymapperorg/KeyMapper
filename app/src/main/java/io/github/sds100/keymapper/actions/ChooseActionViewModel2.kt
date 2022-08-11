package io.github.sds100.keymapper.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.actions.sound.ChooseSoundResult
import io.github.sds100.keymapper.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.system.apps.ChooseAppShortcutResult
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.volume.DndMode
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.util.containsQuery
import io.github.sds100.keymapper.util.ui.KMIcon
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SearchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Created by sds100 on 12/07/2022.
 */
@HiltViewModel
class ChooseActionViewModel2 @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val useCase: CreateActionUseCase
) : ViewModel() {
    companion object {
        private val CATEGORY_ORDER = arrayOf(
            ActionCategory.INPUT,
            ActionCategory.APPS,
            ActionCategory.NAVIGATION,
            ActionCategory.VOLUME,
            ActionCategory.DISPLAY,
            ActionCategory.MEDIA,
            ActionCategory.INTERFACE,
            ActionCategory.CONTENT,
            ActionCategory.KEYBOARD,
            ActionCategory.CONNECTIVITY,
            ActionCategory.TELEPHONY,
            ActionCategory.CAMERA_SOUND,
            ActionCategory.NOTIFICATIONS
        )
    }

    var configActionState: ConfigActionState by mutableStateOf(ConfigActionState.NotStarted)
        private set

    private val allActionGroups: List<ChooseActionListGroup> = createActionGroups()
    private val allActionListItems: List<ChooseActionListItem> = convertGroupsIntoListItems(allActionGroups)

    var actionListItems: List<ChooseActionListItem> by mutableStateOf(allActionListItems)
        private set

    private val _searchState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            searchState.collectLatest { searchState ->
                val newActionListItems = when (searchState) {
                    SearchState.Idle -> allActionListItems
                    is SearchState.Searching -> {
                        val filteredGroups = filterGroups(searchState.query)
                        convertGroupsIntoListItems(filteredGroups)
                    }
                }

                withContext(Dispatchers.Main) {
                    actionListItems = newActionListItems
                }
            }
        }
    }

    fun setSearchState(state: SearchState) {
        _searchState.value = state
    }

    fun dismissConfiguringAction() {
        configActionState = ConfigActionState.NotStarted
    }

    fun onNavigateToConfigScreen() {
        (configActionState as? ConfigActionState.Screen)?.also { screen ->
            configActionState = ConfigActionState.Screen.Navigated(screen)
        }
    }

    fun onChooseInputMethod(imeInfo: ImeInfo) {
        configActionState = ConfigActionState.Finished(ActionData.SwitchKeyboard(imeInfo.id, imeInfo.label))
    }

    fun onChooseKeyCode(keyCode: Int) {
        configActionState = ConfigActionState.Finished(ActionData.InputKeyEvent(keyCode = keyCode))
    }

    fun onChooseApp(packageName: String) {
        val navigatedScreen = (configActionState as? ConfigActionState.Screen.Navigated)?.screen ?: return
        val chooseAppConfig = (navigatedScreen as? ConfigActionState.Screen.ChooseApp) ?: return

        val action = when (chooseAppConfig) {
            ConfigActionState.Screen.ChooseApp.ForAppAction ->
                ActionData.App(packageName)

            ConfigActionState.Screen.ChooseApp.ForNextTrackAction ->
                ActionData.ControlMediaForApp.NextTrack(packageName)

            ConfigActionState.Screen.ChooseApp.ForPauseMediaAction ->
                ActionData.ControlMediaForApp.Pause(packageName)

            ConfigActionState.Screen.ChooseApp.ForPlayMediaAction ->
                ActionData.ControlMediaForApp.Play(packageName)

            ConfigActionState.Screen.ChooseApp.ForPlayPauseMediaAction ->
                ActionData.ControlMediaForApp.PlayPause(packageName)

            ConfigActionState.Screen.ChooseApp.ForPreviousTrackAction ->
                ActionData.ControlMediaForApp.PreviousTrack(packageName)

            ConfigActionState.Screen.ChooseApp.ForFastForwardAction ->
                ActionData.ControlMediaForApp.FastForward(packageName)

            ConfigActionState.Screen.ChooseApp.ForRewindAction ->
                ActionData.ControlMediaForApp.Rewind(packageName)
        }

        configActionState = ConfigActionState.Finished(action)
    }

    fun onChooseAppShortcut(result: ChooseAppShortcutResult) {
        val action = ActionData.AppShortcut(result.packageName, result.shortcutName, result.uri)
        configActionState = ConfigActionState.Finished(action)
    }

    fun onCreateTapScreenAction(result: PickCoordinateResult) {
        val description = result.description.takeIf { it.isNotEmpty() }
        val action = ActionData.TapScreen(result.x, result.y, description)
        configActionState = ConfigActionState.Finished(action)
    }

    fun onCreateTextAction(text: String) {
        configActionState = ConfigActionState.Finished(ActionData.Text(text))
    }

    fun onCreateUrlAction(text: String) {
        configActionState = ConfigActionState.Finished(ActionData.Url(text))
    }

    fun onCreatePhoneCallAction(text: String) {
        configActionState = ConfigActionState.Finished(ActionData.PhoneCall(text))
    }

    fun onChooseSound(result: ChooseSoundResult) {
        configActionState = ConfigActionState.Finished(ActionData.Sound(result.soundUid, result.name))
    }

    fun onConfigCycleRotations(orientations: List<Orientation>) {
        configActionState = ConfigActionState.Finished(ActionData.Rotation.CycleRotations(orientations))
    }

    fun onChooseRingerMode(mode: RingerMode) {
        configActionState = ConfigActionState.Finished(ActionData.SetRingerMode(mode))
    }

    fun onConfigVolumeAction(showVolumeDialog: Boolean, stream: VolumeStream) {
        val volumeConfig = (configActionState as? ConfigActionState.Dialog.Volume) ?: return
        val action = when (volumeConfig) {
            is ConfigActionState.Dialog.Volume.Up -> ActionData.Volume.Up(showVolumeDialog, stream)
            is ConfigActionState.Dialog.Volume.Down -> ActionData.Volume.Down(showVolumeDialog, stream)
            is ConfigActionState.Dialog.Volume.Mute -> ActionData.Volume.Mute(showVolumeDialog, stream)
            is ConfigActionState.Dialog.Volume.Unmute -> ActionData.Volume.UnMute(showVolumeDialog, stream)
            is ConfigActionState.Dialog.Volume.ToggleMute -> ActionData.Volume.ToggleMute(showVolumeDialog, stream)
        }

        configActionState = ConfigActionState.Finished(action)
    }

    fun onChooseDoNotDisturbMode(mode: DndMode) {
        val dndConfig = (configActionState as? ConfigActionState.Dialog.DoNotDisturb) ?: return

        val action = when (dndConfig) {
            ConfigActionState.Dialog.DoNotDisturb.Enable -> ActionData.DoNotDisturb.Enable(mode)
            ConfigActionState.Dialog.DoNotDisturb.Toggle -> ActionData.DoNotDisturb.Toggle(mode)
        }

        configActionState = ConfigActionState.Finished(action)
    }

    fun onChooseFlashlight(flashlight: CameraLens) {
        val flashlightConfig = (configActionState as? ConfigActionState.Dialog.Flashlight) ?: return

        val action = when (flashlightConfig) {
            ConfigActionState.Dialog.Flashlight.Enable -> ActionData.Flashlight.Enable(flashlight)
            ConfigActionState.Dialog.Flashlight.Disable -> ActionData.Flashlight.Disable(flashlight)
            ConfigActionState.Dialog.Flashlight.Toggle -> ActionData.Flashlight.Toggle(flashlight)
        }

        configActionState = ConfigActionState.Finished(action)
    }

    fun onActionClick(id: ActionId) {
        viewModelScope.launch {

            when (id) {
                ActionId.TOGGLE_WIFI -> {
                    configActionState = ConfigActionState.Finished(ActionData.Wifi.Toggle)
                }

                ActionId.ENABLE_WIFI -> {
                    configActionState = ConfigActionState.Finished(ActionData.Wifi.Enable)
                }

                ActionId.DISABLE_WIFI -> {
                    configActionState = ConfigActionState.Finished(ActionData.Wifi.Disable)
                }

                ActionId.KEY_CODE -> {
                    configActionState = ConfigActionState.Screen.ChooseKeycode
                }

                ActionId.SWITCH_KEYBOARD -> {
                    configActionState = ConfigActionState.Dialog.ChooseKeyboard(useCase.getInputMethods())
                }

                ActionId.APP -> {
                    configActionState = ConfigActionState.Screen.ChooseApp.ForAppAction
                }

                ActionId.APP_SHORTCUT -> {
                    configActionState = ConfigActionState.Screen.ChooseAppShortcut
                }
                ActionId.KEY_EVENT -> TODO()
                ActionId.TAP_SCREEN -> {
                    configActionState = ConfigActionState.Screen.CreateTapScreenAction
                }
                ActionId.TEXT -> {
                    configActionState = ConfigActionState.Dialog.Text
                }
                ActionId.URL -> {
                    configActionState = ConfigActionState.Dialog.Url
                }
                ActionId.INTENT -> TODO()
                ActionId.PHONE_CALL -> {
                    configActionState = ConfigActionState.Dialog.PhoneCall
                }
                ActionId.SOUND -> {
                    configActionState = ConfigActionState.Screen.ChooseSound
                }
                ActionId.TOGGLE_BLUETOOTH -> {
                    configActionState = ConfigActionState.Finished(ActionData.Bluetooth.Toggle)
                }
                ActionId.ENABLE_BLUETOOTH -> {
                    configActionState = ConfigActionState.Finished(ActionData.Bluetooth.Enable)
                }
                ActionId.DISABLE_BLUETOOTH -> {
                    configActionState = ConfigActionState.Finished(ActionData.Bluetooth.Disable)
                }
                ActionId.TOGGLE_MOBILE_DATA -> {
                    configActionState = ConfigActionState.Finished(ActionData.MobileData.Toggle)
                }
                ActionId.ENABLE_MOBILE_DATA -> {
                    configActionState = ConfigActionState.Finished(ActionData.MobileData.Enable)
                }
                ActionId.DISABLE_MOBILE_DATA -> {
                    configActionState = ConfigActionState.Finished(ActionData.MobileData.Disable)
                }
                ActionId.TOGGLE_AUTO_BRIGHTNESS -> {
                    configActionState = ConfigActionState.Finished(ActionData.Brightness.ToggleAuto)
                }
                ActionId.DISABLE_AUTO_BRIGHTNESS -> {
                    configActionState = ConfigActionState.Finished(ActionData.Brightness.DisableAuto)
                }
                ActionId.ENABLE_AUTO_BRIGHTNESS -> {
                    configActionState = ConfigActionState.Finished(ActionData.Brightness.EnableAuto)
                }
                ActionId.INCREASE_BRIGHTNESS -> {
                    configActionState = ConfigActionState.Finished(ActionData.Brightness.Increase)
                }
                ActionId.DECREASE_BRIGHTNESS -> {
                    configActionState = ConfigActionState.Finished(ActionData.Brightness.Decrease)
                }
                ActionId.TOGGLE_AUTO_ROTATE -> {
                    configActionState = ConfigActionState.Finished(ActionData.Rotation.ToggleAuto)
                }
                ActionId.ENABLE_AUTO_ROTATE -> {
                    configActionState = ConfigActionState.Finished(ActionData.Rotation.EnableAuto)
                }
                ActionId.DISABLE_AUTO_ROTATE -> {
                    configActionState = ConfigActionState.Finished(ActionData.Rotation.DisableAuto)
                }
                ActionId.PORTRAIT_MODE -> {
                    configActionState = ConfigActionState.Finished(ActionData.Rotation.Portrait)
                }
                ActionId.LANDSCAPE_MODE -> {
                    configActionState = ConfigActionState.Finished(ActionData.Rotation.Landscape)
                }
                ActionId.SWITCH_ORIENTATION -> {
                    configActionState = ConfigActionState.Finished(ActionData.Rotation.SwitchOrientation)
                }
                ActionId.CYCLE_ROTATIONS -> {
                    configActionState = ConfigActionState.Dialog.CycleRotations
                }
                ActionId.VOLUME_UP -> {
                    configActionState = ConfigActionState.Dialog.Volume.Up
                }
                ActionId.VOLUME_DOWN -> {
                    configActionState = ConfigActionState.Dialog.Volume.Down
                }
                ActionId.VOLUME_UNMUTE -> {
                    configActionState = ConfigActionState.Dialog.Volume.Unmute
                }
                ActionId.VOLUME_MUTE -> {
                    configActionState = ConfigActionState.Dialog.Volume.Mute
                }
                ActionId.VOLUME_TOGGLE_MUTE -> {
                    configActionState = ConfigActionState.Dialog.Volume.ToggleMute
                }
                ActionId.VOLUME_SHOW_DIALOG -> {
                    configActionState = ConfigActionState.Finished(ActionData.ShowVolumeDialog)
                }
                ActionId.CYCLE_RINGER_MODE -> {
                    configActionState = ConfigActionState.Finished(ActionData.CycleRingerMode)
                }
                ActionId.CHANGE_RINGER_MODE -> {
                    configActionState = ConfigActionState.Dialog.RingerMode
                }
                ActionId.CYCLE_VIBRATE_RING -> {
                    configActionState = ConfigActionState.Finished(ActionData.CycleVibrateRing)
                }
                ActionId.TOGGLE_DND_MODE -> {
                    configActionState = ConfigActionState.Dialog.DoNotDisturb.Toggle
                }
                ActionId.ENABLE_DND_MODE -> {
                    configActionState = ConfigActionState.Dialog.DoNotDisturb.Enable
                }
                ActionId.DISABLE_DND_MODE -> {
                    configActionState = ConfigActionState.Finished(ActionData.DoNotDisturb.Disable)
                }
                ActionId.EXPAND_NOTIFICATION_DRAWER -> {
                    configActionState = ConfigActionState.Finished(ActionData.StatusBar.ExpandNotifications)
                }
                ActionId.TOGGLE_NOTIFICATION_DRAWER -> {
                    configActionState = ConfigActionState.Finished(ActionData.StatusBar.ToggleNotifications)
                }
                ActionId.EXPAND_QUICK_SETTINGS -> {
                    configActionState = ConfigActionState.Finished(ActionData.StatusBar.ExpandQuickSettings)
                }
                ActionId.TOGGLE_QUICK_SETTINGS -> {
                    configActionState = ConfigActionState.Finished(ActionData.StatusBar.ToggleQuickSettings)
                }
                ActionId.COLLAPSE_STATUS_BAR -> {
                    configActionState = ConfigActionState.Finished(ActionData.StatusBar.Collapse)
                }
                ActionId.PAUSE_MEDIA -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.Pause)
                }
                ActionId.PAUSE_MEDIA_PACKAGE -> {
                    configActionState = ConfigActionState.Screen.ChooseApp.ForPauseMediaAction
                }
                ActionId.PLAY_MEDIA -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.Play)
                }
                ActionId.PLAY_MEDIA_PACKAGE -> {
                    configActionState = ConfigActionState.Screen.ChooseApp.ForPlayMediaAction
                }
                ActionId.PLAY_PAUSE_MEDIA -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.PlayPause)
                }
                ActionId.PLAY_PAUSE_MEDIA_PACKAGE -> {
                    configActionState = ConfigActionState.Screen.ChooseApp.ForPlayPauseMediaAction
                }
                ActionId.NEXT_TRACK -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.NextTrack)
                }
                ActionId.NEXT_TRACK_PACKAGE -> {
                    configActionState = ConfigActionState.Screen.ChooseApp.ForNextTrackAction
                }
                ActionId.PREVIOUS_TRACK -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.PreviousTrack)
                }
                ActionId.PREVIOUS_TRACK_PACKAGE -> {
                    configActionState = ConfigActionState.Screen.ChooseApp.ForPreviousTrackAction
                }
                ActionId.FAST_FORWARD -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.FastForward)
                }
                ActionId.FAST_FORWARD_PACKAGE -> {
                    configActionState = ConfigActionState.Screen.ChooseApp.ForFastForwardAction
                }
                ActionId.REWIND -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.Rewind)
                }
                ActionId.REWIND_PACKAGE -> {
                    configActionState = ConfigActionState.Screen.ChooseApp.ForRewindAction
                }
                ActionId.GO_BACK -> {
                    configActionState = ConfigActionState.Finished(ActionData.GoBack)
                }
                ActionId.GO_HOME -> {
                    configActionState = ConfigActionState.Finished(ActionData.GoHome)
                }
                ActionId.OPEN_RECENTS -> {
                    configActionState = ConfigActionState.Finished(ActionData.OpenRecents)
                }
                ActionId.TOGGLE_SPLIT_SCREEN -> {
                    configActionState = ConfigActionState.Finished(ActionData.ToggleSplitScreen)
                }
                ActionId.GO_LAST_APP -> {
                    configActionState = ConfigActionState.Finished(ActionData.GoLastApp)
                }
                ActionId.OPEN_MENU -> {
                    configActionState = ConfigActionState.Finished(ActionData.OpenMenu)
                }
                ActionId.TOGGLE_FLASHLIGHT -> {
                    configActionState = ConfigActionState.Dialog.Flashlight.Toggle
                }
                ActionId.ENABLE_FLASHLIGHT -> {
                    configActionState = ConfigActionState.Dialog.Flashlight.Enable
                }
                ActionId.DISABLE_FLASHLIGHT -> {
                    configActionState = ConfigActionState.Dialog.Flashlight.Disable
                }
                ActionId.ENABLE_NFC -> {
                    configActionState = ConfigActionState.Finished(ActionData.Nfc.Enable)
                }
                ActionId.DISABLE_NFC -> {
                    configActionState = ConfigActionState.Finished(ActionData.Nfc.Disable)
                }
                ActionId.TOGGLE_NFC -> {
                    configActionState = ConfigActionState.Finished(ActionData.Nfc.Toggle)
                }
                ActionId.MOVE_CURSOR_TO_END -> {
                    configActionState = ConfigActionState.Finished(ActionData.MoveCursorToEnd)
                }
                ActionId.TOGGLE_KEYBOARD -> {
                    configActionState = ConfigActionState.Finished(ActionData.ToggleKeyboard)
                }
                ActionId.SHOW_KEYBOARD -> {
                    configActionState = ConfigActionState.Finished(ActionData.ShowKeyboard)
                }
                ActionId.HIDE_KEYBOARD -> {
                    configActionState = ConfigActionState.Finished(ActionData.HideKeyboard)
                }
                ActionId.SHOW_KEYBOARD_PICKER -> {
                    configActionState = ConfigActionState.Finished(ActionData.ShowKeyboardPicker)
                }
                ActionId.TEXT_CUT -> {
                    configActionState = ConfigActionState.Finished(ActionData.CutText)
                }
                ActionId.TEXT_COPY -> {
                    configActionState = ConfigActionState.Finished(ActionData.CopyText)
                }
                ActionId.TEXT_PASTE -> {
                    configActionState = ConfigActionState.Finished(ActionData.PasteText)
                }
                ActionId.SELECT_WORD_AT_CURSOR -> {
                    configActionState = ConfigActionState.Finished(ActionData.SelectWordAtCursor)
                }
                ActionId.TOGGLE_AIRPLANE_MODE -> {
                    configActionState = ConfigActionState.Finished(ActionData.AirplaneMode.Toggle)
                }
                ActionId.ENABLE_AIRPLANE_MODE -> {
                    configActionState = ConfigActionState.Finished(ActionData.AirplaneMode.Enable)
                }
                ActionId.DISABLE_AIRPLANE_MODE -> {
                    configActionState = ConfigActionState.Finished(ActionData.AirplaneMode.Disable)
                }
                ActionId.SCREENSHOT -> {
                    configActionState = ConfigActionState.Finished(ActionData.Screenshot)
                }
                ActionId.OPEN_VOICE_ASSISTANT -> {
                    configActionState = ConfigActionState.Finished(ActionData.VoiceAssistant)
                }
                ActionId.OPEN_DEVICE_ASSISTANT -> {
                    configActionState = ConfigActionState.Finished(ActionData.DeviceAssistant)
                }
                ActionId.OPEN_CAMERA -> {
                    configActionState = ConfigActionState.Finished(ActionData.OpenCamera)
                }
                ActionId.LOCK_DEVICE -> {
                    configActionState = ConfigActionState.Finished(ActionData.LockDevice)
                }
                ActionId.POWER_ON_OFF_DEVICE -> {
                    configActionState = ConfigActionState.Finished(ActionData.ScreenOnOff)
                }
                ActionId.SECURE_LOCK_DEVICE -> {
                    configActionState = ConfigActionState.Finished(ActionData.SecureLock)
                }
                ActionId.CONSUME_KEY_EVENT -> {
                    configActionState = ConfigActionState.Finished(ActionData.ConsumeKeyEvent)
                }
                ActionId.OPEN_SETTINGS -> {
                    configActionState = ConfigActionState.Finished(ActionData.OpenSettings)
                }
                ActionId.SHOW_POWER_MENU -> {
                    configActionState = ConfigActionState.Finished(ActionData.ShowPowerMenu)
                }
                ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> {
                    configActionState = ConfigActionState.Finished(ActionData.DismissLastNotification)
                }
                ActionId.DISMISS_ALL_NOTIFICATIONS -> {
                    configActionState = ConfigActionState.Finished(ActionData.DismissAllNotifications)
                }
                ActionId.ANSWER_PHONE_CALL -> {
                    configActionState = ConfigActionState.Finished(ActionData.AnswerCall)
                }
                ActionId.END_PHONE_CALL -> {
                    configActionState = ConfigActionState.Finished(ActionData.EndCall)
                }
            }
        }
    }

    private fun filterGroups(query: String): List<ChooseActionListGroup> {
        val filteredGroups = mutableListOf<ChooseActionListGroup>()

        for (group in allActionGroups) {
            val filteredActions = group.actions.filter { it.title.containsQuery(query) }
            if (filteredActions.isNotEmpty()) {
                filteredGroups.add(ChooseActionListGroup(group.header, filteredActions))
            }
        }

        return filteredGroups
    }

    private fun convertGroupsIntoListItems(groups: List<ChooseActionListGroup>): List<ChooseActionListItem> {
        return sequence {
            groups.forEach { group ->
                yield(group.header)
                yieldAll(group.actions)
            }
        }.toList()
    }

    private fun createActionGroups(): List<ChooseActionListGroup> {
        val groups = mutableListOf<ChooseActionListGroup>()

        for (category in CATEGORY_ORDER) {
            val actionIds = ActionId.values().filter { ActionUtils.getCategory(it) == category }

            val headerLabelRes = ActionUtils.getCategoryLabel(category)
            val headerListItem = ChooseActionListItem.Header(resourceProvider.getString(headerLabelRes))

            val actionListItems = actionIds.map { actionId ->
                ChooseActionListItem.Action(
                    actionId,
                    resourceProvider.getString(ActionUtils.getTitle(actionId)),
                    ActionUtils.getNewIcon(actionId)
                )
            }

            val group = ChooseActionListGroup(headerListItem, actionListItems)
            groups.add(group)
        }

        return groups
    }

}

data class ChooseActionListGroup(
    val header: ChooseActionListItem.Header,
    val actions: List<ChooseActionListItem.Action>
)

sealed class ChooseActionListItem {
    data class Header(val header: String) : ChooseActionListItem()
    data class Action(val id: ActionId, val title: String, val icon: KMIcon) : ChooseActionListItem()
}

/**
 * Represents the current state of configuring an action.
 */
sealed class ConfigActionState {
    /**
     * The user is not currently configuring an action.
     */
    object NotStarted : ConfigActionState()

    /**
     * The user is configuring an action with a dialog.
     */
    sealed class Dialog : ConfigActionState() {
        data class ChooseKeyboard(val inputMethods: List<ImeInfo>) : Dialog()
        object Text : Dialog()
        object Url : Dialog()
        object PhoneCall : Dialog()
        object CycleRotations : Dialog()

        sealed class Volume : Dialog() {
            object Up : Volume()
            object Down : Volume()
            object Mute : Volume()
            object Unmute : Volume()
            object ToggleMute : Volume()
        }

        object RingerMode : Dialog()

        sealed class DoNotDisturb : Dialog() {
            object Enable : DoNotDisturb()
            object Toggle : DoNotDisturb()
        }

        sealed class Flashlight : Dialog() {
            object Enable : Flashlight()
            object Disable : Flashlight()
            object Toggle : Flashlight()
        }

        object Hidden : Dialog()
    }

    /**
     * The user is configuring an action on another screen.
     */
    sealed class Screen : ConfigActionState() {
        /**
         * The user has navigated away to the other screen.
         */
        data class Navigated(val screen: Screen) : Screen()

        /**
         * The user should be taken to the screen to choose a key code.
         */
        object ChooseKeycode : Screen()

        sealed class ChooseApp : Screen() {
            object ForAppAction : ChooseApp()
            object ForPauseMediaAction : ChooseApp()
            object ForPlayMediaAction : ChooseApp()
            object ForPlayPauseMediaAction : ChooseApp()
            object ForNextTrackAction : ChooseApp()
            object ForPreviousTrackAction : ChooseApp()
            object ForFastForwardAction : ChooseApp()
            object ForRewindAction : ChooseApp()
        }

        object ChooseAppShortcut : Screen()
        object CreateTapScreenAction : Screen()
        object ChooseSound : Screen()
    }

    data class Finished(val action: ActionData) : ConfigActionState()
}