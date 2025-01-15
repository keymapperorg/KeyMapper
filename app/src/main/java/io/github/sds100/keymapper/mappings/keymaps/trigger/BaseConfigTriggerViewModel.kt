package io.github.sds100.keymapper.mappings.keymaps.trigger

import android.os.Build
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.mappings.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.TextListItem
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 24/11/20.
 */

abstract class BaseConfigTriggerViewModel(
    private val coroutineScope: CoroutineScope,
    private val onboarding: OnboardingUseCase,
    private val config: ConfigKeyMapUseCase,
    private val recordTrigger: RecordTriggerUseCase,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val displayKeyMap: DisplayKeyMapUseCase,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    val optionsViewModel = ConfigTriggerOptionsViewModel(
        coroutineScope,
        config,
        createKeyMapShortcut,
        resourceProvider,
    )

    private val _openEditOptions = MutableSharedFlow<String>()

    /**
     * value is the uid of the action
     */
    val openEditOptions = _openEditOptions.asSharedFlow()

    val recordTriggerState: StateFlow<RecordTriggerState> = recordTrigger.state.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        RecordTriggerState.Idle,
    )

    val triggerModeButtonsEnabled: StateFlow<Boolean> = config.mapping.map { state ->
        when (state) {
            is State.Data -> state.data.trigger.keys.size > 1
            State.Loading -> false
        }
    }.flowOn(Dispatchers.Default).stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val checkedTriggerModeRadioButton: StateFlow<Int> = config.mapping.map { state ->
        when (state) {
            is State.Data -> when (state.data.trigger.mode) {
                is TriggerMode.Parallel -> R.id.radioButtonParallel
                TriggerMode.Sequence -> R.id.radioButtonSequence
                TriggerMode.Undefined -> R.id.radioButtonUndefined
            }

            State.Loading -> R.id.radioButtonUndefined
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(coroutineScope, SharingStarted.Eagerly, R.id.radioButtonUndefined)

    val triggerKeyListItems: StateFlow<State<List<TriggerKeyListItem>>> =
        combine(
            config.mapping,
            displayKeyMap.showDeviceDescriptors,
        ) { mappingState, showDeviceDescriptors ->

            mappingState.mapData { keyMap ->
                createListItems(keyMap.trigger, showDeviceDescriptors)
            }
        }.flowOn(Dispatchers.Default).stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    /**
     * The click type radio buttons are only visible if there is one key
     * or there are only key code keys in the trigger. It is not possible to do a long press of
     * non-key code keys in a parallel trigger.
     */
    val clickTypeRadioButtonsVisible: StateFlow<Boolean> = config.mapping.map { state ->
        when (state) {
            is State.Data -> {
                val trigger = state.data.trigger

                if (trigger.mode is TriggerMode.Parallel) {
                    trigger.keys.all { it is KeyCodeTriggerKey }
                } else {
                    trigger.keys.size == 1
                }
            }

            State.Loading -> false
        }
    }.flowOn(Dispatchers.Default).stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val doublePressButtonVisible: StateFlow<Boolean> = config.mapping.map { state ->
        when (state) {
            is State.Data -> state.data.trigger.keys.size == 1
            State.Loading -> false
        }
    }.flowOn(Dispatchers.Default).stateIn(coroutineScope, SharingStarted.Eagerly, false)

    /**
     * Long press is only allowed for triggers that only use key code trigger keys.
     */
    val longPressButtonVisible: StateFlow<Boolean> = config.mapping.map { state ->
        when (state) {
            is State.Data -> state.data.trigger.keys.all { it is KeyCodeTriggerKey }
            State.Loading -> false
        }
    }.flowOn(Dispatchers.Default).stateIn(coroutineScope, SharingStarted.Eagerly, false)

    /**
     * Only show the buttons for the trigger mode if keys have been added. The buttons
     * shouldn't be shown when no trigger is selected because they aren't relevant
     * for advanced triggers.
     */
    val triggerModeRadioButtonsVisible: StateFlow<Boolean> = config.mapping
        .map { state ->
            when (state) {
                is State.Data -> state.data.trigger.keys.isNotEmpty()
                State.Loading -> false
            }
        }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

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
    }.flowOn(Dispatchers.Default)
        .stateIn(coroutineScope, SharingStarted.Eagerly, R.id.radioButtonShortPress)

    private val _errorListItems = MutableStateFlow<List<TextListItem.Error>>(emptyList())
    val errorListItems = _errorListItems.asStateFlow()

    private val _reportBug = MutableSharedFlow<Unit>()
    val reportBug = _reportBug.asSharedFlow()

    private val _fixAppKilling = MutableSharedFlow<Unit>()
    val fixAppKilling = _fixAppKilling.asSharedFlow()

    var showAdvancedTriggersBottomSheet: Boolean by mutableStateOf(false)
    var showDpadTriggerSetupBottomSheet: Boolean by mutableStateOf(false)
    var showNoKeysRecordedBottomSheet: Boolean by mutableStateOf(false)

    val setupGuiKeyboardState: StateFlow<SetupGuiKeyboardState> = combine(
        setupGuiKeyboard.isInstalled,
        setupGuiKeyboard.isEnabled,
        setupGuiKeyboard.isChosen,
    ) { isInstalled, isEnabled, isChosen ->
        SetupGuiKeyboardState(
            isInstalled,
            isEnabled,
            isChosen,
        )
    }.stateIn(coroutineScope, SharingStarted.Lazily, SetupGuiKeyboardState.DEFAULT)

    init {
        val rebuildErrorList = MutableSharedFlow<State<KeyMap>>(replay = 1)

        coroutineScope.launch(Dispatchers.Default) {
            rebuildErrorList.collectLatest { keyMapState ->
                if (keyMapState !is State.Data) {
                    _errorListItems.value = emptyList()
                    return@collectLatest
                }

                val triggerErrors = displayKeyMap.getTriggerErrors(keyMapState.data)
                val errorListItems = buildTriggerErrorListItems(triggerErrors)

                _errorListItems.value = errorListItems
            }
        }

        coroutineScope.launch {
            config.mapping.collect { mapping ->
                rebuildErrorList.emit(mapping)
            }
        }

        coroutineScope.launch {
            displayKeyMap.invalidateTriggerErrors.collectLatest {
                rebuildErrorList.emit(rebuildErrorList.first())
            }
        }

        coroutineScope.launch {
            recordTrigger.onRecordKey.collectLatest {
                onRecordTriggerKey(it)
            }
        }

        coroutineScope.launch {
            config.mapping
                .mapNotNull { it.dataOrNull()?.trigger?.mode }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { mode ->
                    onTriggerModeChanged(mode)
                }
        }

        recordTrigger.state.onEach { state ->
            if (state is RecordTriggerState.Completed &&
                state.recordedKeys.isEmpty() &&
                onboarding.showNoKeysDetectedBottomSheet.first()
            ) {
                showNoKeysRecordedBottomSheet = true
            }
        }.launchIn(coroutineScope)
    }

    private suspend fun onTriggerModeChanged(mode: TriggerMode) {
        if (mode is TriggerMode.Parallel) {
            if (onboarding.shownParallelTriggerOrderExplanation) {
                return
            }

            val dialog = PopupUi.Ok(
                message = getString(R.string.dialog_message_parallel_trigger_order),
            )

            showPopup("parallel_trigger_order", dialog) ?: return

            onboarding.shownParallelTriggerOrderExplanation = true
        }

        if (mode is TriggerMode.Sequence) {
            if (onboarding.shownSequenceTriggerExplanation) {
                return
            }

            val dialog = PopupUi.Ok(
                message = getString(R.string.dialog_message_sequence_trigger_explanation),
            )

            showPopup("sequence_trigger_explanation", dialog)
                ?: return

            onboarding.shownSequenceTriggerExplanation = true
        }
    }

    private suspend fun onRecordTriggerKey(key: RecordedKey) {
        // Add the trigger key before showing the dialog so it doesn't
        // need to be dismissed before it is added.
        config.addKeyCodeTriggerKey(key.keyCode, key.device, key.detectionSource)

        if (key.keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
            val dialog = PopupUi.Ok(
                message = getString(R.string.dialog_message_enable_physical_keyboard_caps_lock_a_keyboard_layout),
            )

            showPopup("caps_lock_message", dialog)
        }

        if (key.keyCode == KeyEvent.KEYCODE_BACK) {
            val dialog = PopupUi.Ok(
                message = getString(R.string.dialog_message_screen_pinning_warning),
            )

            showPopup("screen_pinning_message", dialog)
        }

        // Issue #491. Some key codes can only be detected through an input method. This will
        // be shown to the user by showing a keyboard icon next to the trigger key name so
        // explain this to the user.
        if (key.detectionSource == KeyEventDetectionSource.INPUT_METHOD && displayKeyMap.showTriggerKeyboardIconExplanation.first()) {
            val dialog = PopupUi.Dialog(
                title = getString(R.string.dialog_title_keyboard_icon_means_ime_detection),
                message = getString(R.string.dialog_message_keyboard_icon_means_ime_detection),
                negativeButtonText = getString(R.string.neg_dont_show_again),
                positiveButtonText = getString(R.string.pos_ok),
            )

            val response = showPopup("keyboard_icon_explanation", dialog)

            if (response == DialogResponse.NEGATIVE) {
                displayKeyMap.neverShowTriggerKeyboardIconExplanation()
            }
        }
    }

    private fun buildTriggerErrorListItems(triggerErrors: List<TriggerError>): List<TextListItem.Error> = triggerErrors.map { error ->
        when (error) {
            TriggerError.DND_ACCESS_DENIED -> TextListItem.Error(
                id = error.toString(),
                text = getString(R.string.trigger_error_dnd_access_denied),
            )

            TriggerError.SCREEN_OFF_ROOT_DENIED -> TextListItem.Error(
                id = error.toString(),
                text = getString(R.string.trigger_error_screen_off_root_permission_denied),
            )

            TriggerError.CANT_DETECT_IN_PHONE_CALL -> TextListItem.Error(
                id = error.toString(),
                text = getString(R.string.trigger_error_cant_detect_in_phone_call),
            )

            TriggerError.ASSISTANT_NOT_SELECTED -> TextListItem.Error(
                id = error.toString(),
                text = getString(R.string.trigger_error_assistant_activity_not_chosen),
            )

            TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> TextListItem.Error(
                id = error.toString(),
                text = getString(R.string.trigger_error_assistant_not_purchased),
            )

            TriggerError.DPAD_IME_NOT_SELECTED -> TextListItem.Error(
                id = error.toString(),
                text = getString(R.string.trigger_error_dpad_ime_not_selected),
            )
        }
    }

    fun onParallelRadioButtonCheckedChange(isChecked: Boolean) {
        if (isChecked) {
            config.setParallelTriggerMode()
        }
    }

    fun onSequenceRadioButtonCheckedChange(isChecked: Boolean) {
        if (isChecked) {
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

    open fun onTriggerKeyOptionsClick(id: String) {
        runBlocking { _openEditOptions.emit(id) }
    }

    fun onChooseDeviceClick(keyUid: String) {
        coroutineScope.launch {
            chooseDeviceForKeyCodeTriggerKey(keyUid)
        }
    }

    private suspend fun chooseDeviceForKeyCodeTriggerKey(keyUid: String) {
        val idAny = "any"
        val idInternal = "this_device"
        val devices = config.getAvailableTriggerKeyDevices()
        val showDeviceDescriptors = displayKeyMap.showDeviceDescriptors.first()

        val listItems = devices.map { device: TriggerKeyDevice ->
            when (device) {
                TriggerKeyDevice.Any -> idAny to getString(R.string.any_device)
                TriggerKeyDevice.Internal -> idInternal to getString(R.string.this_device)
                is TriggerKeyDevice.External -> {
                    if (showDeviceDescriptors) {
                        val name = InputDeviceUtils.appendDeviceDescriptorToName(
                            device.descriptor,
                            device.name,
                        )
                        device.descriptor to name
                    } else {
                        device.descriptor to device.name
                    }
                }
            }
        }

        val triggerKeyDeviceId = showPopup(
            "pick_trigger_key_device",
            PopupUi.SingleChoice(listItems),
        ) ?: return

        val selectedTriggerKeyDevice = when (triggerKeyDeviceId) {
            idAny -> TriggerKeyDevice.Any
            idInternal -> TriggerKeyDevice.Internal
            else -> devices.single { it is TriggerKeyDevice.External && it.descriptor == triggerKeyDeviceId }
        }

        config.setTriggerKeyDevice(keyUid, selectedTriggerKeyDevice)
    }

    fun onRecordTriggerButtonClick() {
        coroutineScope.launch {
            val recordTriggerState = recordTrigger.state.firstOrNull() ?: return@launch

            val result = when (recordTriggerState) {
                is RecordTriggerState.CountingDown -> recordTrigger.stopRecording()
                is RecordTriggerState.Completed,
                RecordTriggerState.Idle,
                -> recordTrigger.startRecording()
            }

            if (result is Error.AccessibilityServiceDisabled) {
                ViewModelHelper.handleAccessibilityServiceStoppedDialog(
                    resourceProvider = this@BaseConfigTriggerViewModel,
                    popupViewModel = this@BaseConfigTriggerViewModel,
                    startService = displayKeyMap::startAccessibilityService,
                )
            }

            if (result is Error.AccessibilityServiceCrashed) {
                ViewModelHelper.handleAccessibilityServiceCrashedSnackBar(
                    resourceProvider = this@BaseConfigTriggerViewModel,
                    popupViewModel = this@BaseConfigTriggerViewModel,
                    restartService = displayKeyMap::restartAccessibilityService,
                    message = R.string.dialog_message_restart_accessibility_service_to_record_trigger,
                )
            }
        }
    }

    fun stopRecordingTrigger() {
        coroutineScope.launch {
            recordTrigger.stopRecording()
        }
    }

    fun onTriggerErrorClick(listItemId: String) {
        coroutineScope.launch {
            when (TriggerError.valueOf(listItemId)) {
                TriggerError.DND_ACCESS_DENIED -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@BaseConfigTriggerViewModel,
                        popupViewModel = this@BaseConfigTriggerViewModel,
                        neverShowDndTriggerErrorAgain = { displayKeyMap.neverShowDndTriggerError() },
                        fixError = { displayKeyMap.fixError(it) },
                    )
                }

                TriggerError.SCREEN_OFF_ROOT_DENIED -> {
                    val error = Error.PermissionDenied(Permission.ROOT)
                    displayKeyMap.fixError(error)
                }

                TriggerError.CANT_DETECT_IN_PHONE_CALL -> {
                    displayKeyMap.fixError(Error.CantDetectKeyEventsInPhoneCall)
                }

                TriggerError.ASSISTANT_NOT_SELECTED -> {
                    displayKeyMap.fixError(Error.PermissionDenied(Permission.DEVICE_ASSISTANT))
                }

                TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> {
                    showAdvancedTriggersBottomSheet = true
                }

                TriggerError.DPAD_IME_NOT_SELECTED -> {
                    showDpadTriggerSetupBottomSheet = true
                }
            }
        }
    }

    private fun createListItems(
        trigger: Trigger,
        showDeviceDescriptors: Boolean,
    ): List<TriggerKeyListItem> = trigger.keys.mapIndexed { index, key ->
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
            name = getTriggerKeyName(key),
            clickTypeString = clickTypeString,
            extraInfo = getTriggerKeyExtraInfo(key, showDeviceDescriptors),
            linkType = linkDrawable,
            isDragDropEnabled = trigger.keys.size > 1,
            isChooseDeviceButtonVisible = key is KeyCodeTriggerKey,
        )
    }

    private fun getTriggerKeyExtraInfo(key: TriggerKey, showDeviceDescriptors: Boolean): String? {
        if (key !is KeyCodeTriggerKey) {
            return null
        }

        return buildString {
            append(getTriggerKeyDeviceName(key.device, showDeviceDescriptors))
            val midDot = getString(R.string.middot)

            if (!key.consumeEvent) {
                append(" $midDot ${getString(R.string.flag_dont_override_default_action)}")
            }
        }
    }

    private fun getTriggerKeyName(key: TriggerKey): String {
        return when (key) {
            is AssistantTriggerKey -> when (key.type) {
                AssistantTriggerType.ANY -> getString(R.string.assistant_any_trigger_name)
                AssistantTriggerType.VOICE -> getString(R.string.assistant_voice_trigger_name)
                AssistantTriggerType.DEVICE -> getString(R.string.assistant_device_trigger_name)
            }

            is KeyCodeTriggerKey -> buildString {
                append(InputEventUtils.keyCodeToString(key.keyCode))

                if (key.detectionSource == KeyEventDetectionSource.INPUT_METHOD) {
                    val midDot = getString(R.string.middot)
                    append(" $midDot ${getString(R.string.flag_detect_from_input_method)}")
                }
            }
        }
    }

    private fun getTriggerKeyDeviceName(
        device: TriggerKeyDevice,
        showDeviceDescriptors: Boolean,
    ): String = when (device) {
        is TriggerKeyDevice.Internal -> getString(R.string.this_device)
        is TriggerKeyDevice.Any -> getString(R.string.any_device)
        is TriggerKeyDevice.External -> {
            if (showDeviceDescriptors) {
                InputDeviceUtils.appendDeviceDescriptorToName(
                    device.descriptor,
                    device.name,
                )
            } else {
                device.name
            }
        }
    }

    fun onEnableGuiKeyboardClick() {
        setupGuiKeyboard.enableInputMethod()
    }

    fun onChooseGuiKeyboardClick() {
        setupGuiKeyboard.chooseInputMethod()
    }

    fun onNeverShowSetupDpadClick() {
        displayKeyMap.neverShowDpadImeSetupError()
    }

    fun onNeverShowNoKeysRecordedClick() {
        onboarding.neverShowNoKeysRecordedBottomSheet()
    }
}
