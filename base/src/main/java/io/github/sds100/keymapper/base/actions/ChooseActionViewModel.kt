package io.github.sds100.keymapper.base.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.containsQuery
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.DialogResponse
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemGroup
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemModel
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ChooseActionViewModel @Inject constructor(
    private val useCase: CreateActionUseCase,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    companion object {
        private val CATEGORY_ORDER = arrayOf(
            ActionCategory.INPUT,
            ActionCategory.APPS,
            ActionCategory.FLASHLIGHT,
            ActionCategory.NAVIGATION,
            ActionCategory.VOLUME,
            ActionCategory.DISPLAY,
            ActionCategory.MEDIA,
            ActionCategory.INTERFACE,
            ActionCategory.KEYBOARD,
            ActionCategory.CONNECTIVITY,
            ActionCategory.TELEPHONY,
            ActionCategory.NOTIFICATIONS,
            ActionCategory.SPECIAL,
        )
    }

    val createActionDelegate =
        CreateActionDelegate(viewModelScope, useCase, this, this, this)

    private val allGroupedListItems: List<SimpleListItemGroup> by lazy { buildListGroups() }

    val searchQuery = MutableStateFlow<String?>(null)

    val groups: StateFlow<State<List<SimpleListItemGroup>>> =
        searchQuery.map { query ->
            val groups = allGroupedListItems.mapNotNull { group ->

                val filteredItems = group.items.filter { it.title.containsQuery(query) }

                if (filteredItems.isEmpty()) {
                    return@mapNotNull null
                } else {
                    group.copy(items = filteredItems)
                }
            }

            State.Data(groups)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    init {
        viewModelScope.launch {
            createActionDelegate.actionResult.filterNotNull().collect { action ->
                popBackStackWithResult(Json.encodeToString(action))
            }
        }
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            val actionId = ActionId.valueOf(id)
            val approvedMessage = showMessageForAction(actionId)

            if (!approvedMessage) {
                return@launch
            }

            createActionDelegate.createAction(actionId)
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    private fun buildListGroups(): List<SimpleListItemGroup> = buildList {
        val listItems = buildListItems(ActionId.entries)

        for (category in CATEGORY_ORDER) {
            val header = getString(ActionUtils.getCategoryLabel(category))

            val group = SimpleListItemGroup(
                header,
                items = listItems.filter {
                    it.isEnabled &&
                        ActionUtils.getCategory(ActionId.valueOf(it.id)) == category
                },
            )
            add(group)
        }

        val unsupportedGroup = SimpleListItemGroup(
            header = getString(R.string.choose_action_group_unsupported),
            items = listItems.filter { !it.isEnabled },
        )
        add(unsupportedGroup)
    }

    private fun buildListItems(
        actionIds: List<ActionId>,
    ): List<SimpleListItemModel> = buildList {
        for (actionId in actionIds) {
            // See Issue #1593. This action should no longer exist because it is a relic
            // of the past when most apps had a 3-dot menu with a consistent content description
            // making it somewhat easy to identify. This action should still be usable
            // if a user already has a key map with it so just hide it from the list.
            if (actionId == ActionId.OPEN_MENU) {
                continue
            }

            val error = useCase.isSupported(actionId)
            val isSupported = error == null

            val title = getString(ActionUtils.getTitle(actionId))
            val icon = ActionUtils.getComposeIcon(actionId)

            val subtitle = when {
                error == SystemError.PermissionDenied(Permission.ROOT) -> getString(R.string.choose_action_warning_requires_root)
                error != null -> error.getFullMessage(this@ChooseActionViewModel)
                else -> null
            }

            add(
                SimpleListItemModel(
                    id = actionId.toString(),
                    title = title,
                    icon = ComposeIconInfo.Vector(icon),
                    subtitle = subtitle,
                    isSubtitleError = true,
                    isEnabled = isSupported,
                ),
            )
        }
    }

    /**
     * @return whether the user approved the message
     */
    private suspend fun showMessageForAction(id: ActionId): Boolean {
        // See issue #1379
        if (id == ActionId.APP) {
            val response = showDialog(
                "show_app_action_warning_dialog",
                DialogModel.Alert(
                    message = getString(R.string.action_open_app_dialog_message),
                    title = getString(R.string.action_open_app_dialog_title),
                    positiveButtonText = getString(R.string.action_open_app_dialog_read_more_button),
                    negativeButtonText = getString(R.string.action_open_app_dialog_ignore_button),
                ),
            )

            if (response == DialogResponse.POSITIVE) {
                showDialog(
                    "app_action_permission_info",
                    DialogModel.OpenUrl(getString(R.string.url_action_guide)),
                )
                return false
            } else {
                return response != null
            }
        }

        val messageToShow: Int? = when (id) {
            ActionId.FAST_FORWARD_PACKAGE,
            ActionId.FAST_FORWARD,
                -> R.string.action_fast_forward_message

            ActionId.REWIND_PACKAGE,
            ActionId.REWIND,
                -> R.string.action_rewind_message

            ActionId.TOGGLE_KEYBOARD,
            ActionId.SHOW_KEYBOARD,
            ActionId.HIDE_KEYBOARD,
                -> R.string.action_toggle_keyboard_message

            ActionId.SECURE_LOCK_DEVICE -> R.string.action_secure_lock_device_message

            else -> null
        }

        if (messageToShow != null) {
            val response = showDialog(
                "show_action_message",
                DialogModel.Ok(message = getString(messageToShow)),
            )

            return response != null
        }

        return true
    }
}
