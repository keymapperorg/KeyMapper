package io.github.sds100.keymapper.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.util.ui.Icon
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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

    var actionListItems: List<ChooseActionListItem> by mutableStateOf(emptyList())

    var query: String by mutableStateOf("")
    private val queryFlow: Flow<String> = snapshotFlow { query }

    init {
        actionListItems = ActionId.values().map { id ->
            ChooseActionListItem(
                id,
                resourceProvider.getString(ActionUtils.getTitle(id)),
                ActionUtils.getNewIcon(id)
            )
        }

        queryFlow.onEach {

        }.launchIn(viewModelScope)
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
                    
                }

                ActionId.KEY_CODE -> {
                    configActionState = ConfigActionState.Screen.ChooseKeycode
                }

                ActionId.SWITCH_KEYBOARD -> {
                    configActionState = ConfigActionState.Dialog.ChooseKeyboard(useCase.getInputMethods())
                }
            }
        }
    }
}

data class ChooseActionListItem(val id: ActionId, val title: String, val icon: Icon)

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