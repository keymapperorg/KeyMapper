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

    var configActionState: ConfigActionState? by mutableStateOf(null)
        private set

    var listItems: List<ChooseActionListItem> by mutableStateOf(emptyList())

    var query: String by mutableStateOf("")
    private val queryFlow: Flow<String> = snapshotFlow { query }

    init {
        listItems = ActionId.values().map { id ->
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
        configActionState = null
    }

    fun onChooseInputMethod(inputMethod: String) {

    }

    fun onActionClick(id: ActionId) {
        viewModelScope.launch {
            configActionState = ConfigActionState.ChooseKeyboard(useCase.getInputMethods())

            when (id) {
                ActionId.SWITCH_KEYBOARD -> {
                }
            }
        }
    }
}

data class ChooseActionListItem(val id: ActionId, val title: String, val icon: Icon)

sealed class ConfigActionState(val id: ActionId) {
    data class ChooseKeyboard(val inputMethods: List<ImeInfo>) : ConfigActionState(ActionId.SWITCH_KEYBOARD)
}