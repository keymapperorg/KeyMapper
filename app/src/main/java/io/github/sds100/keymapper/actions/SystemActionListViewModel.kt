package io.github.sds100.keymapper.actions

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.domain.utils.*
import io.github.sds100.keymapper.system.volume.*
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.camera.CameraLensUtils
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.display.OrientationUtils
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.ui.utils.*
import io.github.sds100.keymapper.util.containsQuery
import io.github.sds100.keymapper.util.valueOrNull
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Created by sds100 on 31/03/2020.
 */

class SystemActionListViewModel(
    private val useCase: CreateSystemActionUseCase,
    resourceProvider: ResourceProvider,
) : ViewModel(), ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    val searchQuery = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(SystemActionListState(ListUiState.Loading, false))
    val state = _state.asStateFlow()

    private val _returnResult = MutableSharedFlow<SystemAction>()
    val returnResult = _returnResult.asSharedFlow()

    init {
        viewModelScope.launch {
            searchQuery.collectLatest { query ->
                _state.value = withContext(Dispatchers.Default) {
                    buildState(query)
                }
            }
        }
    }

    fun onSystemActionClick(id: SystemActionId) {
        viewModelScope.launch {
            val approvedMessage = showMessageForSystemAction(id)

            if (!approvedMessage) return@launch

            when (id) {
                SystemActionId.SWITCH_KEYBOARD -> {
                    val inputMethods = useCase.getInputMethods()
                    val items = inputMethods.map {
                        it.id to it.label
                    }

                    val imeId = showPopup("choose_ime", PopupUi.SingleChoice(items))?.item
                        ?: return@launch
                    val imeName = inputMethods.single { it.id == imeId }.label

                    _returnResult.emit(SwitchKeyboardSystemAction(imeId, imeName))
                }

                SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE,
                SystemActionId.PLAY_MEDIA_PACKAGE,
                SystemActionId.PAUSE_MEDIA_PACKAGE,
                SystemActionId.NEXT_TRACK_PACKAGE,
                SystemActionId.PREVIOUS_TRACK_PACKAGE,
                SystemActionId.FAST_FORWARD_PACKAGE,
                SystemActionId.REWIND_PACKAGE,
                -> {
                    val items: List<Pair<String, String>> = withContext(Dispatchers.Default) {
                        val packages = useCase.getInstalledPackages()

                        return@withContext packages
                            .filter { app -> app.canBeLaunched }
                            .mapNotNull { app ->
                                val appName = useCase.getAppName(app.packageName).valueOrNull()
                                    ?: return@mapNotNull null

                                app.packageName to appName
                            }
                            .sortedBy { it.second }
                    }

                    val packageName = showPopup("choose_package", PopupUi.SingleChoice(items))?.item ?: return@launch

                    val action = when (id) {
                        SystemActionId.PAUSE_MEDIA_PACKAGE ->
                            ControlMediaForAppSystemAction.Pause(packageName)
                        SystemActionId.PLAY_MEDIA_PACKAGE ->
                            ControlMediaForAppSystemAction.Play(packageName)
                        SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE ->
                            ControlMediaForAppSystemAction.PlayPause(packageName)
                        SystemActionId.NEXT_TRACK_PACKAGE ->
                            ControlMediaForAppSystemAction.NextTrack(packageName)
                        SystemActionId.PREVIOUS_TRACK_PACKAGE ->
                            ControlMediaForAppSystemAction.PreviousTrack(packageName)
                        SystemActionId.FAST_FORWARD_PACKAGE ->
                            ControlMediaForAppSystemAction.FastForward(packageName)
                        SystemActionId.REWIND_PACKAGE ->
                            ControlMediaForAppSystemAction.Rewind(packageName)
                        else -> throw Exception("don't know how to create system action for $id")
                    }

                    _returnResult.emit(action)
                }

                SystemActionId.VOLUME_INCREASE_STREAM,
                SystemActionId.VOLUME_DECREASE_STREAM -> {
                    val items = VolumeStream.values()
                        .map { it to getString(VolumeStreamUtils.getLabel(it)) }

                    val stream = showPopup("pick_volume_stream", PopupUi.SingleChoice(items))
                        ?.item ?: return@launch

                    val action = when (id) {
                        SystemActionId.VOLUME_INCREASE_STREAM ->
                            VolumeSystemAction.Stream.Increase(showVolumeUi = false, stream)

                        SystemActionId.VOLUME_DECREASE_STREAM ->
                            VolumeSystemAction.Stream.Decrease(showVolumeUi = false, stream)

                        else -> throw Exception("don't know how to create system action for $id")
                    }

                    _returnResult.emit(action)
                }

                SystemActionId.CHANGE_RINGER_MODE -> {
                    val items = RingerMode.values()
                        .map { it to getString(RingerModeUtils.getLabel(it)) }

                    val ringerMode =
                        showPopup("pick_ringer_mode", PopupUi.SingleChoice(items))?.item
                            ?: return@launch

                    _returnResult.emit(ChangeRingerModeSystemAction(ringerMode))
                }

                //don't need to show options for disabling do not disturb
                SystemActionId.TOGGLE_DND_MODE,
                SystemActionId.ENABLE_DND_MODE -> {
                    val items = DndMode.values()
                        .map { it to getString(DndModeUtils.getLabel(it)) }

                    val dndMode =
                        showPopup("pick_dnd_mode", PopupUi.SingleChoice(items))?.item
                            ?: return@launch

                    val action = when (id) {
                        SystemActionId.TOGGLE_DND_MODE ->ToggleDndMode(dndMode)

                        SystemActionId.ENABLE_DND_MODE ->EnableDndMode(dndMode)

                        else -> throw Exception("don't know how to create system action for $id")
                    }

                    _returnResult.emit(action)
                }

                SystemActionId.CYCLE_ROTATIONS -> {
                    val items = Orientation.values()
                        .map { it to getString(OrientationUtils.getLabel(it)) }

                    val orientations =
                        showPopup("pick_orientations", PopupUi.MultiChoice(items))?.items
                            ?: return@launch

                    _returnResult.emit(CycleRotationsSystemAction(orientations))
                }

                SystemActionId.TOGGLE_FLASHLIGHT,
                SystemActionId.ENABLE_FLASHLIGHT,
                SystemActionId.DISABLE_FLASHLIGHT -> {
                    val items = CameraLens.values().map {
                        it to getString(CameraLensUtils.getLabel(it))
                    }

                    val lens = showPopup("pick_lens", PopupUi.SingleChoice(items))?.item
                        ?: return@launch

                    val action = when (id) {
                        SystemActionId.TOGGLE_FLASHLIGHT -> FlashlightSystemAction.Toggle(lens)
                        SystemActionId.ENABLE_FLASHLIGHT -> FlashlightSystemAction.Enable(lens)
                        SystemActionId.DISABLE_FLASHLIGHT -> FlashlightSystemAction.Disable(lens)
                        else -> throw Exception("don't know how to create system action for $id")
                    }

                    _returnResult.emit(action)
                }

                else -> _returnResult.emit(SimpleSystemAction(id))
            }
        }
    }

    /**
     * @return whether the user approved the message
     */
    private suspend fun showMessageForSystemAction(id: SystemActionId): Boolean {
        @StringRes val messageToShow: Int? = when (id) {
            SystemActionId.FAST_FORWARD_PACKAGE,
            SystemActionId.FAST_FORWARD -> R.string.action_fast_forward_message

            SystemActionId.REWIND_PACKAGE,
            SystemActionId.REWIND -> R.string.action_rewind_message

            SystemActionId.MOVE_CURSOR_TO_END -> R.string.action_move_to_end_of_text_message

            SystemActionId.TOGGLE_KEYBOARD,
            SystemActionId.SHOW_KEYBOARD,
            SystemActionId.HIDE_KEYBOARD -> R.string.action_toggle_keyboard_message

            SystemActionId.SECURE_LOCK_DEVICE -> R.string.action_secure_lock_device_message
            SystemActionId.POWER_ON_OFF_DEVICE -> R.string.action_power_on_off_device_message

            else -> null
        }

        if (messageToShow != null) {
            val response = showPopup(
                "show_system_action_message",
                PopupUi.Ok(message = getString(messageToShow))
            )

            return response != null
        }

        return true
    }

    private fun buildState(query: String?): SystemActionListState {
        val groupedModels = SystemActionId.values().groupBy { SystemActionUtils.getCategory(it) }
        var unsupportedActions = false

        val listItems = sequence {
            groupedModels.forEach { (category, children) ->
                val childrenListItems = mutableListOf<SystemActionListItem>()

                for (systemActionId in children) {
                    if (useCase.isSupported(systemActionId) != null) {
                        unsupportedActions = true
                        continue
                    }

                    val title = getString(SystemActionUtils.getTitle(systemActionId))

                    if (!title.containsQuery(query)) {
                        continue
                    }

                    val icon = SystemActionUtils.getIcon(systemActionId)?.let {
                        getDrawable(it)
                    }

                    val requiresRoot = SystemActionUtils.getRequiredPermissions(systemActionId)
                        .contains(Permission.ROOT)

                    childrenListItems.add(
                        SystemActionListItem(
                            systemActionId = systemActionId,
                            title = title,
                            icon = icon,
                            showRequiresRootMessage = requiresRoot
                        )
                    )
                }

                if (childrenListItems.isNotEmpty()) {

                    val sectionHeader = SectionHeaderListItem(
                        id = category.toString(),
                        text = getString(SystemActionUtils.getCategoryLabel(category))
                    )

                    yield(sectionHeader)
                    yieldAll(childrenListItems)
                }
            }
        }.toList()

        return SystemActionListState(listItems.createListState(), unsupportedActions)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val useCase: CreateSystemActionUseCase,
        private val resourceProvider: ResourceProvider,
    ) :
        ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SystemActionListViewModel(useCase, resourceProvider) as T
        }
    }
}

data class SystemActionListState(
    val listItems: ListUiState<ListItem>,
    val showUnsupportedActionsMessage: Boolean
)