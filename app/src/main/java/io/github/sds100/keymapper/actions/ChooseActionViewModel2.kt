package io.github.sds100.keymapper.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.system.apps.ChooseAppShortcutResult
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
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
        configActionState = ConfigActionState.Screen.Navigated
    }

    fun onChooseInputMethod(imeInfo: ImeInfo) {
        configActionState = ConfigActionState.Finished(ActionData.SwitchKeyboard(imeInfo.id, imeInfo.label))
    }

    fun onChooseKeyCode(keyCode: Int) {
        configActionState = ConfigActionState.Finished(ActionData.InputKeyEvent(keyCode = keyCode))
    }

    fun onChooseApp(packageName: String) {
        configActionState = ConfigActionState.Finished(ActionData.App(packageName))
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
                    configActionState = ConfigActionState.Screen.ChooseApp
                }

                ActionId.APP_SHORTCUT -> {
                    configActionState = ConfigActionState.Screen.ChooseAppShortcut
                }
                ActionId.KEY_EVENT -> TODO()
                ActionId.TAP_SCREEN -> {
                    configActionState = ConfigActionState.Screen.CreateTapScreenAction
                }
                ActionId.TEXT -> {
                    configActionState = ConfigActionState.Dialog.TextAction
                }
                ActionId.URL -> {
                    configActionState = ConfigActionState.Dialog.UrlAction
                }
                ActionId.INTENT -> TODO()
                ActionId.PHONE_CALL -> TODO()
                ActionId.SOUND -> TODO()
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
                ActionId.CYCLE_ROTATIONS -> TODO()
                ActionId.VOLUME_UP -> TODO()
                ActionId.VOLUME_DOWN -> TODO()
                ActionId.VOLUME_SHOW_DIALOG -> {
                    configActionState = ConfigActionState.Finished(ActionData.ShowVolumeDialog)
                }
                ActionId.CYCLE_RINGER_MODE -> {
                    configActionState = ConfigActionState.Finished(ActionData.CycleRingerMode)
                }
                ActionId.CHANGE_RINGER_MODE -> TODO()
                ActionId.CYCLE_VIBRATE_RING -> {
                    configActionState = ConfigActionState.Finished(ActionData.CycleVibrateRing)
                }
                ActionId.TOGGLE_DND_MODE -> TODO()
                ActionId.ENABLE_DND_MODE -> TODO()
                ActionId.DISABLE_DND_MODE -> TODO()
                ActionId.VOLUME_UNMUTE -> TODO()
                ActionId.VOLUME_MUTE -> TODO()
                ActionId.VOLUME_TOGGLE_MUTE -> TODO()
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
                ActionId.PAUSE_MEDIA_PACKAGE -> TODO()
                ActionId.PLAY_MEDIA -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.Play)
                }
                ActionId.PLAY_MEDIA_PACKAGE -> TODO()
                ActionId.PLAY_PAUSE_MEDIA -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.PlayPause)
                }
                ActionId.PLAY_PAUSE_MEDIA_PACKAGE -> TODO()
                ActionId.NEXT_TRACK -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.NextTrack)
                }
                ActionId.NEXT_TRACK_PACKAGE -> TODO()
                ActionId.PREVIOUS_TRACK -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.PreviousTrack)
                }
                ActionId.PREVIOUS_TRACK_PACKAGE -> TODO()
                ActionId.FAST_FORWARD -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.FastForward)
                }
                ActionId.FAST_FORWARD_PACKAGE -> TODO()
                ActionId.REWIND -> {
                    configActionState = ConfigActionState.Finished(ActionData.ControlMedia.Rewind)
                }
                ActionId.REWIND_PACKAGE -> TODO()
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
                ActionId.TOGGLE_FLASHLIGHT -> TODO()
                ActionId.ENABLE_FLASHLIGHT -> TODO()
                ActionId.DISABLE_FLASHLIGHT -> TODO()
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

        object TextAction : Dialog()

        object UrlAction : Dialog()

        object Hidden : Dialog()
    }

    /**
     * The user is configuring an action on another screen.
     */
    sealed class Screen : ConfigActionState() {
        /**
         * The user has navigated away to the other screen.
         */
        object Navigated : Screen()

        /**
         * The user should be taken to the screen to choose a key code.
         */
        object ChooseKeycode : Screen()

        object ChooseApp : Screen()
        object ChooseAppShortcut : Screen()
        object CreateTapScreenAction : Screen()
    }

    data class Finished(val action: ActionData) : ConfigActionState()
}