package io.github.sds100.keymapper.mappings.keymaps

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.trigger.*
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 24/11/20.
 */

class ConfigKeyMapTriggerViewModel(
    private val coroutineScope: CoroutineScope,
    private val onboarding: OnboardingUseCase,
    private val config: ConfigKeyMapUseCase,
    private val recordTrigger: RecordTriggerUseCase,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val displayKeyMap: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider
) : ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    val optionsViewModel = ConfigKeyMapTriggerOptionsViewModel(
        coroutineScope,
        config,
        createKeyMapShortcut,
        resourceProvider
    )

    private val _openEditOptions = MutableSharedFlow<String>()

    /**
     * value is the uid of the action
     */
    val openEditOptions = _openEditOptions.asSharedFlow()

    val recordTriggerButtonText: StateFlow<String> = recordTrigger.state.map { recordTriggerState ->
        when (recordTriggerState) {
            is RecordTriggerState.CountingDown -> getString(
                R.string.button_recording_trigger_countdown,
                recordTriggerState.timeLeft
            )
            RecordTriggerState.Stopped -> getString(R.string.button_record_trigger)
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, "")

    val triggerModeButtonsEnabled: StateFlow<Boolean> = config.mapping.map { state ->
        when (state) {
            is State.Data -> state.data.trigger.keys.size > 1
            State.Loading -> false
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val checkedTriggerModeRadioButton: StateFlow<Int> = config.mapping.map { state ->
        when (state) {
            is State.Data -> when (state.data.trigger.mode) {
                is TriggerMode.Parallel -> R.id.radioButtonParallel
                TriggerMode.Sequence -> R.id.radioButtonSequence
                TriggerMode.Undefined -> R.id.radioButtonUndefined
            }

            State.Loading -> R.id.radioButtonUndefined
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, R.id.radioButtonUndefined)

    val triggerKeyListItems: StateFlow<ListUiState<TriggerKeyListItem>> = config.mapping.map{state ->
        when(state){
            is State.Data ->createListItems(state.data.trigger).createListState()
            is State.Loading -> ListUiState.Loading
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, ListUiState.Loading)

    val clickTypeRadioButtonsVisible: StateFlow<Boolean> = config.mapping.map { state ->
        when (state) {
            is State.Data -> {
                val trigger = state.data.trigger

               trigger.mode is TriggerMode.Parallel || trigger.keys.size == 1
            }
            State.Loading -> false
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val doublePressButtonVisible: StateFlow<Boolean> = config.mapping.map { state ->
        when (state) {
            is State.Data -> state.data.trigger.keys.size == 1
            State.Loading -> false
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val checkedClickTypeRadioButton: StateFlow<Int> = config.mapping.map { state ->
        when (state) {
            is State.Data -> {
                val trigger = state.data.trigger

                val clickType: ClickType? = when {
                   trigger.mode is TriggerMode.Parallel -> trigger.mode.clickType
                    trigger.keys.size == 1 -> trigger.keys[0].clickType
                    else -> null
                }

                when (clickType) {
                    ClickType.SHORT_PRESS -> R.id.radioButtonShortPress
                    ClickType.LONG_PRESS -> R.id.radioButtonLongPress
                    ClickType.DOUBLE_PRESS -> R.id.radioButtonDoublePress
                    null -> R.id.radioButtonShortPress
                }
            }
            State.Loading -> R.id.radioButtonShortPress
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, R.id.radioButtonShortPress)

    private val _errorListItems = MutableStateFlow<List<TextListItem.Error>>(emptyList())
    val errorListItems = _errorListItems.asStateFlow()

    init {
        val rebuildErrorList = MutableSharedFlow<State<KeyMapTrigger>>(replay = 1)

        coroutineScope.launch {
            rebuildErrorList.collectLatest { triggerState ->
                if (triggerState !is State.Data){
                    _errorListItems.value = emptyList()
                    return@collectLatest
                }

                displayKeyMap.getTriggerErrors(triggerState.data).map { error ->
                    when (error) {
                        KeyMapTriggerError.DND_ACCESS_DENIED -> TextListItem.Error(
                            id = error.toString(),
                            text = getString(R.string.trigger_error_dnd_access_denied),
                        )

                        KeyMapTriggerError.SCREEN_OFF_ROOT_DENIED -> TextListItem.Error(
                            id = error.toString(),
                            text = getString(R.string.trigger_error_screen_off_root_permission_denied)
                        )
                    }
                }
            }
        }

        coroutineScope.launch {
            config.mapping.collect { mapping ->
                rebuildErrorList.emit(mapping.mapData { it.trigger })
            }
        }

        coroutineScope.launch {
            displayKeyMap.invalidateErrors.collectLatest {
                rebuildErrorList.emit(rebuildErrorList.first())
            }
        }

        recordTrigger.onRecordKey.onEach {
            if (it.keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
                val dialog = PopupUi.Ok(
                    message = getString(R.string.dialog_message_enable_physical_keyboard_caps_lock_a_keyboard_layout)
                )

                showPopup("caps_lock_message", dialog)
            }

            config.addTriggerKey(it.keyCode, it.device)
        }.launchIn(coroutineScope)

        coroutineScope.launch {
            config.mapping
                .mapNotNull { it.dataOrNull()?.trigger?.mode }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { mode ->
                    if (mode is TriggerMode.Parallel) {
                        if (onboarding.shownParallelTriggerOrderExplanation) return@collectLatest

                        val dialog = PopupUi.Ok(
                            message = getString(R.string.dialog_message_parallel_trigger_order)
                        )

                        showPopup("parallel_trigger_order", dialog) ?: return@collectLatest

                        onboarding.shownParallelTriggerOrderExplanation = true
                    }

                    if (mode is TriggerMode.Sequence) {
                        if (onboarding.shownSequenceTriggerExplanation) return@collectLatest

                        val dialog = PopupUi.Ok(
                            message = getString(R.string.dialog_message_sequence_trigger_explanation)
                        )

                        showPopup("sequence_trigger_explanation", dialog) ?: return@collectLatest

                        onboarding.shownSequenceTriggerExplanation = true
                    }
                }
        }
    }

    fun onParallelRadioButtonCheckedChange(isChecked: Boolean){
        if (isChecked){
            config.setParallelTriggerMode()
        }
    }

    fun onSequenceRadioButtonCheckedChange(isChecked: Boolean){
        if (isChecked){
            config.setSequenceTriggerMode()
        }
    }

    fun onClickTypeRadioButtonCheckedChange(buttonId: Int) {
        when (buttonId) {
            R.id.radioButtonShortPress -> config.setTriggerShortPress()
            R.id.radioButtonLongPress -> config.setTriggerLongPress()
            R.id.radioButtonDoublePress -> config.setTriggerDoublePress()
        }
    }

    fun onRemoveKeyClick(uid: String) = config.removeTriggerKey(uid)
    fun onMoveTriggerKey(fromIndex: Int, toIndex: Int) = config.moveTriggerKey(fromIndex, toIndex)

    fun onTriggerKeyOptionsClick(id: String) {
        runBlocking { _openEditOptions.emit(id) }
    }

    fun onChooseDeviceClick(keyUid: String) {
        coroutineScope.launch {
            val idAny = "any"
            val idInternal = "this_device"
            val devices = config.getAvailableTriggerKeyDevices()

            val listItems = devices.map {
                when (it) {
                    TriggerKeyDevice.Any -> idAny to getString(R.string.any_device)
                    is TriggerKeyDevice.External -> it.descriptor to it.name
                    TriggerKeyDevice.Internal -> idInternal to getString(R.string.this_device)
                }
            }

            val response = showPopup(
                "pick_trigger_key_device",
                PopupUi.SingleChoice(listItems)
            ) ?: return@launch

            val selectedTriggerKeyDevice = when (response.item) {
                idAny -> TriggerKeyDevice.Any
                idInternal -> TriggerKeyDevice.Internal
                else -> devices.single { it is TriggerKeyDevice.External && it.descriptor == response.item }
            }

            config.setTriggerKeyDevice(keyUid, selectedTriggerKeyDevice)
        }
    }

    fun onRecordTriggerButtonClick() {
        coroutineScope.launch {
            val recordTriggerState = recordTrigger.state.firstOrNull()

            when (recordTriggerState) {
                is RecordTriggerState.CountingDown -> recordTrigger.stopRecording()
                RecordTriggerState.Stopped -> {
                    val recordResult = recordTrigger.startRecording()

                    if (recordResult is Error.AccessibilityServiceDisabled) {

                        val snackBar = PopupUi.SnackBar(
                            message = getString(R.string.dialog_message_enable_accessibility_service_to_record_trigger),
                            actionText = getString(R.string.pos_turn_on)
                        )

                        val response = showPopup("enable_service", snackBar)

                        if (response != null) {
                            displayKeyMap.fixError(Error.AccessibilityServiceDisabled)
                        }
                    }

                    if (recordResult is Error.AccessibilityServiceCrashed) {

                        val snackBar = PopupUi.SnackBar(
                            message = getString(R.string.dialog_message_restart_accessibility_service_to_record_trigger),
                            actionText = getString(R.string.pos_restart)
                        )

                        val response = showPopup("restart_service", snackBar)

                        if (response != null) {
                            displayKeyMap.fixError(Error.AccessibilityServiceCrashed)
                        }
                    }
                }
            }
        }
    }

    fun stopRecordingTrigger() = recordTrigger.stopRecording()

    fun fixError(listItemId: String) {
        coroutineScope.launch {
            when (KeyMapTriggerError.valueOf(listItemId)) {
                KeyMapTriggerError.DND_ACCESS_DENIED -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val error =
                        Error.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)
                    displayKeyMap.fixError(error)
                }

                KeyMapTriggerError.SCREEN_OFF_ROOT_DENIED -> {
                    val error = Error.PermissionDenied(Permission.ROOT)
                    displayKeyMap.fixError(error)
                }
            }
        }
    }

    private fun createListItems(trigger: KeyMapTrigger): List<TriggerKeyListItem> =
        trigger.keys.mapIndexed { index, key ->
            val extraInfo = buildString {
                append(getDeviceName(key.device))

                if (!key.consumeKeyEvent) {
                    val midDot = getString(R.string.middot)
                    append(" $midDot ${getString(R.string.flag_dont_override_default_action)}")
                }
            }

            val clickTypeString = when (key.clickType) {
                ClickType.SHORT_PRESS -> null
                ClickType.LONG_PRESS -> getString(R.string.clicktype_long_press)
                ClickType.DOUBLE_PRESS -> getString(R.string.clicktype_double_press)
            }

            val linkDrawable = when {
                trigger.mode is TriggerMode.Parallel && index < trigger.keys.lastIndex -> TriggerKeyLinkType.PLUS
                trigger.mode is TriggerMode.Sequence && index < trigger.keys.lastIndex -> TriggerKeyLinkType.ARROW
                else -> TriggerKeyLinkType.HIDDEN
            }

            TriggerKeyListItem(
                id = key.uid,
                keyCode = key.keyCode,
                name = KeyEventUtils.keycodeToString(key.keyCode),
                clickTypeString = clickTypeString,
                extraInfo = extraInfo,
                linkType = linkDrawable,
                isDragDropEnabled = trigger.keys.size > 1
            )
        }

    private fun getDeviceName(device: TriggerKeyDevice): String =
        when (device) {
            is TriggerKeyDevice.Internal -> getString(R.string.this_device)
            is TriggerKeyDevice.Any -> getString(R.string.any_device)
            is TriggerKeyDevice.External -> device.name
        }
}