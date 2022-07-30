package io.github.sds100.keymapper.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun onChooseInputMethod(imeId: String, imeName: String) {
        configActionState = ConfigActionState.Finished(ActionData.SwitchKeyboard(imeId, imeName))
    }

    fun onChooseKeyCode(keyCode: Int) {
        configActionState = ConfigActionState.Finished(ActionData.InputKeyEvent(keyCode = keyCode))
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

                ActionId.SWITCH_KEYBOARD -> TODO()

                ActionId.APP -> TODO()
                ActionId.APP_SHORTCUT -> TODO()
                ActionId.KEY_EVENT -> TODO()
                ActionId.TAP_SCREEN -> TODO()
                ActionId.TEXT -> TODO()
                ActionId.URL -> TODO()
                ActionId.INTENT -> TODO()
                ActionId.PHONE_CALL -> TODO()
                ActionId.SOUND -> TODO()
                ActionId.TOGGLE_BLUETOOTH -> TODO()
                ActionId.ENABLE_BLUETOOTH -> TODO()
                ActionId.DISABLE_BLUETOOTH -> TODO()
                ActionId.TOGGLE_MOBILE_DATA -> TODO()
                ActionId.ENABLE_MOBILE_DATA -> TODO()
                ActionId.DISABLE_MOBILE_DATA -> TODO()
                ActionId.TOGGLE_AUTO_BRIGHTNESS -> TODO()
                ActionId.DISABLE_AUTO_BRIGHTNESS -> TODO()
                ActionId.ENABLE_AUTO_BRIGHTNESS -> TODO()
                ActionId.INCREASE_BRIGHTNESS -> TODO()
                ActionId.DECREASE_BRIGHTNESS -> TODO()
                ActionId.TOGGLE_AUTO_ROTATE -> TODO()
                ActionId.ENABLE_AUTO_ROTATE -> TODO()
                ActionId.DISABLE_AUTO_ROTATE -> TODO()
                ActionId.PORTRAIT_MODE -> TODO()
                ActionId.LANDSCAPE_MODE -> TODO()
                ActionId.SWITCH_ORIENTATION -> TODO()
                ActionId.CYCLE_ROTATIONS -> TODO()
                ActionId.VOLUME_UP -> TODO()
                ActionId.VOLUME_DOWN -> TODO()
                ActionId.VOLUME_SHOW_DIALOG -> TODO()
                ActionId.CYCLE_RINGER_MODE -> TODO()
                ActionId.CHANGE_RINGER_MODE -> TODO()
                ActionId.CYCLE_VIBRATE_RING -> TODO()
                ActionId.TOGGLE_DND_MODE -> TODO()
                ActionId.ENABLE_DND_MODE -> TODO()
                ActionId.DISABLE_DND_MODE -> TODO()
                ActionId.VOLUME_UNMUTE -> TODO()
                ActionId.VOLUME_MUTE -> TODO()
                ActionId.VOLUME_TOGGLE_MUTE -> TODO()
                ActionId.EXPAND_NOTIFICATION_DRAWER -> TODO()
                ActionId.TOGGLE_NOTIFICATION_DRAWER -> TODO()
                ActionId.EXPAND_QUICK_SETTINGS -> TODO()
                ActionId.TOGGLE_QUICK_SETTINGS -> TODO()
                ActionId.COLLAPSE_STATUS_BAR -> TODO()
                ActionId.PAUSE_MEDIA -> TODO()
                ActionId.PAUSE_MEDIA_PACKAGE -> TODO()
                ActionId.PLAY_MEDIA -> TODO()
                ActionId.PLAY_MEDIA_PACKAGE -> TODO()
                ActionId.PLAY_PAUSE_MEDIA -> TODO()
                ActionId.PLAY_PAUSE_MEDIA_PACKAGE -> TODO()
                ActionId.NEXT_TRACK -> TODO()
                ActionId.NEXT_TRACK_PACKAGE -> TODO()
                ActionId.PREVIOUS_TRACK -> TODO()
                ActionId.PREVIOUS_TRACK_PACKAGE -> TODO()
                ActionId.FAST_FORWARD -> TODO()
                ActionId.FAST_FORWARD_PACKAGE -> TODO()
                ActionId.REWIND -> TODO()
                ActionId.REWIND_PACKAGE -> TODO()
                ActionId.GO_BACK -> TODO()
                ActionId.GO_HOME -> TODO()
                ActionId.OPEN_RECENTS -> TODO()
                ActionId.TOGGLE_SPLIT_SCREEN -> TODO()
                ActionId.GO_LAST_APP -> TODO()
                ActionId.OPEN_MENU -> TODO()
                ActionId.TOGGLE_FLASHLIGHT -> TODO()
                ActionId.ENABLE_FLASHLIGHT -> TODO()
                ActionId.DISABLE_FLASHLIGHT -> TODO()
                ActionId.ENABLE_NFC -> TODO()
                ActionId.DISABLE_NFC -> TODO()
                ActionId.TOGGLE_NFC -> TODO()
                ActionId.MOVE_CURSOR_TO_END -> TODO()
                ActionId.TOGGLE_KEYBOARD -> TODO()
                ActionId.SHOW_KEYBOARD -> TODO()
                ActionId.HIDE_KEYBOARD -> TODO()
                ActionId.SHOW_KEYBOARD_PICKER -> TODO()
                ActionId.TEXT_CUT -> TODO()
                ActionId.TEXT_COPY -> TODO()
                ActionId.TEXT_PASTE -> TODO()
                ActionId.SELECT_WORD_AT_CURSOR -> TODO()
                ActionId.TOGGLE_AIRPLANE_MODE -> TODO()
                ActionId.ENABLE_AIRPLANE_MODE -> TODO()
                ActionId.DISABLE_AIRPLANE_MODE -> TODO()
                ActionId.SCREENSHOT -> TODO()
                ActionId.OPEN_VOICE_ASSISTANT -> TODO()
                ActionId.OPEN_DEVICE_ASSISTANT -> TODO()
                ActionId.OPEN_CAMERA -> TODO()
                ActionId.LOCK_DEVICE -> TODO()
                ActionId.POWER_ON_OFF_DEVICE -> TODO()
                ActionId.SECURE_LOCK_DEVICE -> TODO()
                ActionId.CONSUME_KEY_EVENT -> TODO()
                ActionId.OPEN_SETTINGS -> TODO()
                ActionId.SHOW_POWER_MENU -> TODO()
                ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> TODO()
                ActionId.DISMISS_ALL_NOTIFICATIONS -> TODO()
                ActionId.ANSWER_PHONE_CALL -> TODO()
                ActionId.END_PHONE_CALL -> TODO()
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
    sealed class Dialog(val actionId: ActionId) : ConfigActionState() {
        data class ChooseKeyboard(val inputMethods: List<ImeInfo>) : Dialog(ActionId.SWITCH_KEYBOARD)
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
    }

    data class Finished(val action: ActionData) : ConfigActionState()
}