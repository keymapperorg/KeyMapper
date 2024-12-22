package io.github.sds100.keymapper.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.containsQuery
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SectionHeaderListItem
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 22/07/2021.
 */
class ChooseActionViewModel(
    private val useCase: CreateActionUseCase,
    resourceProvider: ResourceProvider,
    private val isActionSupported: IsActionSupportedUseCase,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    CreateActionViewModel by CreateActionViewModelImpl(useCase, resourceProvider) {

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
            ActionCategory.NOTIFICATIONS,
        )
    }

    private val allGroupedListItems: Map<SectionHeaderListItem, List<DefaultSimpleListItem>> by lazy { createGroupedListItems() }

    private val _returnAction = MutableSharedFlow<ActionData>()
    val returnAction = _returnAction.asSharedFlow()

    val searchQuery = MutableStateFlow<String?>(null)

    val listItems: StateFlow<State<List<ListItem>>> =
        searchQuery
            .map { query ->
                allGroupedListItems
                    .mapNotNull { (header, children) ->
                        val filteredChildren = children.filter { it.title.containsQuery(query) }

                        if (filteredChildren.isEmpty()) {
                            return@mapNotNull null
                        } else {
                            header to filteredChildren
                        }
                    }
                    .flatMap { (sectionHeader, children) -> listOf(sectionHeader).plus(children) }
                    .let { State.Data(it) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            val actionId = ActionId.valueOf(id)
            val approvedMessage = showMessageForAction(actionId)

            if (!approvedMessage) {
                showMessageForAction(actionId)
            }

            createAction(actionId)?.let { action ->
                _returnAction.emit(action)
            }
        }
    }

    private fun createGroupedListItems(): Map<SectionHeaderListItem, List<DefaultSimpleListItem>> {
        val groupedActionIds = mutableMapOf<ActionCategory, List<ActionId>>()

        CATEGORY_ORDER.forEach { category ->
            val actionIds = ActionId.values().filter { ActionUtils.getCategory(it) == category }
            groupedActionIds[category] = actionIds
        }

        return groupedActionIds
            .map { (category, children) -> createCategoryListItems(category, children) }
            .toMap()
    }

    private fun createCategoryListItems(
        category: ActionCategory,
        actionIds: List<ActionId>,
    ): Pair<SectionHeaderListItem, List<DefaultSimpleListItem>> {
        val childrenListItems = mutableListOf<DefaultSimpleListItem>()

        for (actionId in actionIds) {
            val error = isActionSupported.invoke(actionId)
            val isSupported = error == null

            val title = getString(ActionUtils.getTitle(actionId))

            val icon = ActionUtils.getIcon(actionId)?.let {
                IconInfo(
                    getDrawable(it),
                    TintType.OnSurface,
                )
            }

            val requiresRoot = ActionUtils.getRequiredPermissions(actionId)
                .contains(Permission.ROOT)

            val subtitle = when {
                error != null -> error.getFullMessage(this)
                requiresRoot -> getString(R.string.choose_action_warning_requires_root)
                else -> null
            }

            childrenListItems.add(
                DefaultSimpleListItem(
                    id = actionId.toString(),
                    title = title,
                    subtitle = subtitle,
                    subtitleTint = TintType.Error,
                    icon = icon,
                    isEnabled = isSupported,
                ),
            )
        }

        val sectionHeader = SectionHeaderListItem(
            id = category.toString(),
            text = getString(ActionUtils.getCategoryLabel(category)),
        )

        return sectionHeader to childrenListItems.toList()
    }

    /**
     * @return whether the user approved the message
     */
    private suspend fun showMessageForAction(id: ActionId): Boolean {
        // See issue #1379
        if (id == ActionId.APP) {
            val response = showPopup(
                "show_app_action_warning_dialog",
                PopupUi.Dialog(
                    message = getString(R.string.action_open_app_dialog_message),
                    title = getString(R.string.action_open_app_dialog_title),
                    positiveButtonText = getString(R.string.action_open_app_dialog_read_more_button),
                    negativeButtonText = getString(R.string.action_open_app_dialog_ignore_button)
                ),
            )

            if (response == DialogResponse.POSITIVE) {
                showPopup(
                    "app_action_permission_info",
                    PopupUi.OpenUrl(getString(R.string.url_action_guide))
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

            ActionId.MOVE_CURSOR_TO_END -> R.string.action_move_to_end_of_text_message

            ActionId.TOGGLE_KEYBOARD,
            ActionId.SHOW_KEYBOARD,
            ActionId.HIDE_KEYBOARD,
            -> R.string.action_toggle_keyboard_message

            ActionId.SECURE_LOCK_DEVICE -> R.string.action_secure_lock_device_message
            ActionId.POWER_ON_OFF_DEVICE -> R.string.action_power_on_off_device_message

            else -> null
        }

        if (messageToShow != null) {
            val response = showPopup(
                "show_action_message",
                PopupUi.Ok(message = getString(messageToShow)),
            )

            return response != null
        }

        return true
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val useCase: CreateActionUseCase,
        private val resourceProvider: ResourceProvider,
        private val isActionSupported: IsActionSupportedUseCase,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChooseActionViewModel(useCase, resourceProvider, isActionSupported) as T
    }
}
