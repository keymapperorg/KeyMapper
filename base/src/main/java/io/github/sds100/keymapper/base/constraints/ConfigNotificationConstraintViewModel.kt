package io.github.sds100.keymapper.base.constraints

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.handle
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class ConfigNotificationConstraintViewModel @Inject constructor(
    private val displayConstraintUseCase: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    NavigationProvider by navigationProvider {

    var selectedField: NotificationField by mutableStateOf(NotificationField.TITLE)
        private set

    var matchMode: TextMatchMode by mutableStateOf(TextMatchMode.CONTAINS)
        private set

    var value: String by mutableStateOf("")
        private set

    var isEditing: Boolean by mutableStateOf(false)
        private set

    val isValid: Boolean
        get() = value.isNotEmpty()

    /**
     * The name of the chosen app, or null when no app has been chosen yet.
     */
    val selectedAppName: String?
        get() = if (selectedField == NotificationField.PACKAGE && value.isNotEmpty()) {
            displayConstraintUseCase.getAppName(value).handle(
                onSuccess = { it },
                onError = { value },
            )
        } else {
            null
        }

    fun onSelectField(field: NotificationField) {
        selectedField = field

        // An app is picked from a list so it can only ever be matched exactly. Going back to a text
        // field starts from the default rather than keeping whatever was used last.
        matchMode = TextMatchMode.CONTAINS

        value = ""
    }

    fun onSelectMatchMode(matchMode: TextMatchMode) {
        this.matchMode = matchMode
    }

    fun onValueChange(value: String) {
        this.value = value
    }

    fun onChooseAppClick() {
        viewModelScope.launch {
            val packageName = navigate(
                "choose_app_for_notification_constraint",
                NavDestination.ChooseApp(allowHiddenApps = true),
            ) ?: return@launch

            value = packageName
        }
    }

    fun onDoneClick() {
        val constraint = buildConstraintOrNull() ?: return

        viewModelScope.launch {
            popBackStackWithResult(Json.encodeToString(constraint))
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    private fun buildConstraintOrNull(): ConstraintData.NotificationPosted? {
        if (value.isEmpty()) {
            return null
        }

        return when (selectedField) {
            NotificationField.PACKAGE ->
                ConstraintData.NotificationPosted.FromApp(packageName = value)

            NotificationField.TITLE -> ConstraintData.NotificationPosted.Title(
                text = value,
                matchMode = matchMode,
            )

            NotificationField.TEXT -> ConstraintData.NotificationPosted.Text(
                text = value,
                matchMode = matchMode,
            )
        }
    }
}
