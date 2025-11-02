package io.github.sds100.keymapper.base.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import javax.inject.Inject

data class CreateNotificationActionState(
    val title: String = "",
    val text: String = "",
    val timeoutEnabled: Boolean = false,
    /**
     * UI works with seconds for user-friendliness
     */
    val timeoutSeconds: Int = 30,
)

@HiltViewModel
class ConfigCreateNotificationViewModel @Inject constructor(
    private val navigationProvider: NavigationProvider,
    private val createActionDelegate: CreateActionDelegate,
) : ViewModel() {

    var state: CreateNotificationActionState by mutableStateOf(CreateNotificationActionState())
        private set

    fun loadAction(action: ActionData.CreateNotification) {
        state = state.copy(
            title = action.title,
            text = action.text,
            timeoutEnabled = action.timeoutMs != null,
            timeoutSeconds = (action.timeoutMs ?: 30000) / 1000,
        )
    }

    fun onTitleChanged(newTitle: String) {
        state = state.copy(title = newTitle)
    }

    fun onTextChanged(newText: String) {
        state = state.copy(text = newText)
    }

    fun onTimeoutEnabledChanged(enabled: Boolean) {
        state = state.copy(timeoutEnabled = enabled)
    }

    fun onTimeoutChanged(newTimeoutSeconds: Int) {
        state = state.copy(timeoutSeconds = newTimeoutSeconds)
    }

    fun onDoneClick() {
        if (state.title.isBlank() || state.text.isBlank()) {
            return
        }

        val timeoutMs = if (state.timeoutEnabled) {
            state.timeoutSeconds * 1000L
        } else {
            null
        }

        val action = ActionData.CreateNotification(
            title = state.title,
            text = state.text,
            timeoutMs = timeoutMs,
        )

        createActionDelegate.actionResult.value = action
        navigationProvider.navigate(io.github.sds100.keymapper.base.utils.navigation.NavDestination.Pop)
    }

    fun onCancelClick() {
        navigationProvider.navigate(io.github.sds100.keymapper.base.utils.navigation.NavDestination.Pop)
    }
}
