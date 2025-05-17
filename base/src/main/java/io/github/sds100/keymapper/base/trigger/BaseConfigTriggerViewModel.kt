package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assistant
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapOptionsViewModel
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGestureType
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.keymaps.ShortcutModel
import io.github.sds100.keymapper.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.purchasing.ProductId
import io.github.sds100.keymapper.purchasing.PurchasingManager
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.ifIsData
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.base.utils.ui.CheckBoxListItem
import io.github.sds100.keymapper.base.utils.ui.DialogResponse
import io.github.sds100.keymapper.base.utils.ui.LinkType
import io.github.sds100.keymapper.base.utils.ui.NavigationViewModel
import io.github.sds100.keymapper.base.utils.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.base.utils.ui.PopupUi
import io.github.sds100.keymapper.base.utils.ui.PopupViewModel
import io.github.sds100.keymapper.base.utils.ui.PopupViewModelImpl
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseConfigTriggerViewModel(
    private val coroutineScope: CoroutineScope,
    private val onboarding: OnboardingUseCase,
    private val config: ConfigKeyMapUseCase,
    private val recordTrigger: io.github.sds100.keymapper.base.trigger.RecordTriggerUseCase,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val displayKeyMap: DisplayKeyMapUseCase,
    private val purchasingManager: PurchasingManager,
    private val setupGuiKeyboard: io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase,
    private val fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    companion object {
        private const val DEVICE_ID_ANY = "any"
        private const val DEVICE_ID_INTERNAL = "internal"
    }

    val optionsViewModel = ConfigKeyMapOptionsViewModel(
        coroutineScope,
        config,
        displayKeyMap,
        createKeyMapShortcut,
        resourceProvider,
    )

    private val triggerKeyShortcuts = combine(
        fingerprintGesturesSupported.isSupported,
        purchasingManager.purchases,
    ) { isFingerprintGesturesSupported, purchasesState ->
        val newShortcuts = mutableSetOf<ShortcutModel<io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut>>()

        if (isFingerprintGesturesSupported == true) {
            newShortcuts.add(
                ShortcutModel(
                    icon = ComposeIconInfo.Vector(Icons.Rounded.Fingerprint),
                    text = getString(R.string.trigger_key_shortcut_add_fingerprint_gesture),
                    data = _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut.FINGERPRINT_GESTURE,
                ),
            )
        }

        purchasesState.ifIsData { result ->
            result.onSuccess { purchases ->
                if (purchases.contains(ProductId.ASSISTANT_TRIGGER)) {
                    newShortcuts.add(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.Assistant),
                            text = getString(R.string.trigger_key_shortcut_add_assistant),
                            data = _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut.ASSISTANT,
                        ),
                    )
                }

                if (purchases.contains(ProductId.FLOATING_BUTTONS)) {
                    newShortcuts.add(
                        ShortcutModel(
                            icon = ComposeIconInfo.Vector(Icons.Rounded.BubbleChart),
                            text = getString(R.string.trigger_key_shortcut_add_floating_button),
                            data = _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut.FLOATING_BUTTON,
                        ),
                    )
                }
            }
        }

        newShortcuts
    }

    private val _state: MutableStateFlow<State<ConfigTriggerState>> =
        MutableStateFlow(State.Loading)
    val state: StateFlow<State<ConfigTriggerState>> = _state.asStateFlow()

    val recordTriggerState: StateFlow<io.github.sds100.keymapper.base.trigger.RecordTriggerState> = recordTrigger.state.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        _root_ide_package_.io.github.sds100.keymapper.base.trigger.RecordTriggerState.Idle,
    )

    var showAdvancedTriggersBottomSheet: Boolean by mutableStateOf(false)
    var showDpadTriggerSetupBottomSheet: Boolean by mutableStateOf(false)
    var showNoKeysRecordedBottomSheet: Boolean by mutableStateOf(false)

    val setupGuiKeyboardState: StateFlow<io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardState> = combine(
        setupGuiKeyboard.isInstalled,
        setupGuiKeyboard.isEnabled,
        setupGuiKeyboard.isChosen,
    ) { isInstalled, isEnabled, isChosen ->
        _root_ide_package_.io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardState(
            isInstalled,
            isEnabled,
            isChosen,
        )
    }.stateIn(coroutineScope, SharingStarted.Lazily,
        _root_ide_package_.io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardState.Companion.DEFAULT
    )

    val triggerKeyOptionsUid = MutableStateFlow<String?>(null)
    val triggerKeyOptionsState: StateFlow<io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState?> =
        combine(config.keyMap, triggerKeyOptionsUid, transform = ::buildKeyOptionsUiState)
            .stateIn(coroutineScope, SharingStarted.Lazily, null)

    /**
     * Check whether the user stopped the trigger recording countdown. This
     * distinction is important so that the bottom sheet describing what to do
     * when no buttons are recorded is not shown.
     */
    private var isRecordingCompletionUserInitiated: Boolean = false

    init {
        val showTapTargetsPairFlow: Flow<Pair<Boolean, Boolean>> = combine(
            onboarding.showTapTarget(OnboardingTapTarget.RECORD_TRIGGER),
            onboarding.showTapTarget(OnboardingTapTarget.ADVANCED_TRIGGERS),
        ) { recordTriggerTapTarget, advancedTriggersTapTarget ->
            Pair(recordTriggerTapTarget, advancedTriggersTapTarget)
        }

        // IMPORTANT! Do not flow on another thread because this causes the drag and drop
        // animations to be more janky.
        combine(
            displayKeyMap.triggerErrorSnapshot,
            config.keyMap,
            displayKeyMap.showDeviceDescriptors,
            triggerKeyShortcuts,
            showTapTargetsPairFlow,
        ) { triggerErrorSnapshot, keyMap, showDeviceDescriptors, shortcuts, showTapTargetsPair ->
            _state.update {
                buildUiState(
                    keyMap,
                    showDeviceDescriptors,
                    shortcuts,
                    triggerErrorSnapshot,
                    showTapTargetsPair.first,
                    showTapTargetsPair.second,
                )
            }
        }.launchIn(coroutineScope)

        coroutineScope.launch {
            recordTrigger.onRecordKey.collectLatest {
                onRecordTriggerKey(it)
            }
        }

        coroutineScope.launch {
            config.keyMap
                .mapNotNull { it.dataOrNull()?.trigger?.mode }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { mode ->
                    onTriggerModeChanged(mode)
                }
        }

        // Drop the first state in case it is in the Completed state so the
        // "button not detected" bottom sheet isn't shown when
        // the screen is opened.
        recordTrigger.state.drop(1).onEach { state ->
            if (state is io.github.sds100.keymapper.base.trigger.RecordTriggerState.Completed &&
                state.recordedKeys.isEmpty() &&
                onboarding.showNoKeysDetectedBottomSheet.first() &&
                !isRecordingCompletionUserInitiated
            ) {
                showNoKeysRecordedBottomSheet = true
            }

            // reset this field when recording has completed
            isRecordingCompletionUserInitiated = false
        }.launchIn(coroutineScope)
    }

    open fun onClickTriggerKeyShortcut(shortcut: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut) {
        if (shortcut == _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut.FINGERPRINT_GESTURE) {
            coroutineScope.launch {
                val listItems = listOf(
                    FingerprintGestureType.SWIPE_DOWN to getString(R.string.fingerprint_gesture_down),
                    FingerprintGestureType.SWIPE_UP to getString(R.string.fingerprint_gesture_up),
                    FingerprintGestureType.SWIPE_LEFT to getString(R.string.fingerprint_gesture_left),
                    FingerprintGestureType.SWIPE_RIGHT to getString(R.string.fingerprint_gesture_right),
                )

                val selectedType = showPopup("pick_assistant_type", PopupUi.SingleChoice(listItems))
                    ?: return@launch

                config.addFingerprintGesture(type = selectedType)
            }
        }
    }

    fun onAdvancedTriggersClick() {
        onboarding.viewedAdvancedTriggers()
        showAdvancedTriggersBottomSheet = true
    }

    private fun buildUiState(
        keyMapState: State<KeyMap>,
        showDeviceDescriptors: Boolean,
        triggerKeyShortcuts: Set<ShortcutModel<_root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut>>,
        triggerErrorSnapshot: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerErrorSnapshot,
        showRecordTriggerTapTarget: Boolean,
        showAdvancedTriggersTapTarget: Boolean,
    ): State<ConfigTriggerState> {
        return keyMapState.mapData { keyMap ->
            val trigger = keyMap.trigger

            if (trigger.keys.isEmpty()) {
                return@mapData _root_ide_package_.io.github.sds100.keymapper.base.trigger.ConfigTriggerState.Empty(
                    triggerKeyShortcuts,
                    showRecordTriggerTapTarget = showRecordTriggerTapTarget,
                    showAdvancedTriggersTapTarget = showAdvancedTriggersTapTarget,
                )
            }

            val triggerKeys =
                createListItems(
                    keyMap,
                    showDeviceDescriptors,
                    triggerKeyShortcuts.size,
                    triggerErrorSnapshot,
                )
            val isReorderingEnabled = trigger.keys.size > 1
            val triggerModeButtonsEnabled = keyMap.trigger.keys.size > 1

            /**
             * Only show the buttons for the trigger mode if keys have been added. The buttons
             * shouldn't be shown when no trigger is selected because they aren't relevant
             * for advanced triggers.
             */
            val triggerModeButtonsVisible = trigger.keys.isNotEmpty()

            val clickTypeButtons = mutableSetOf<ClickType>()

            /**
             * The click type radio buttons are only visible if there is one key
             * or there are only key code keys in the trigger. It is not possible to do a long press of
             * non-key code keys in a parallel trigger.
             */
            if (trigger.keys.size == 1 && trigger.keys.all { it.allowedDoublePress }) {
                clickTypeButtons.add(ClickType.SHORT_PRESS)
                clickTypeButtons.add(ClickType.DOUBLE_PRESS)
            }

            if (trigger.keys.isNotEmpty() && trigger.mode !is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode.Sequence && trigger.keys.all { it.allowedLongPress }) {
                clickTypeButtons.add(ClickType.SHORT_PRESS)
                clickTypeButtons.add(ClickType.LONG_PRESS)
            }

            val checkedClickType: ClickType? = when {
                trigger.mode is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode.Parallel -> trigger.mode.clickType
                trigger.keys.size == 1 -> trigger.keys[0].clickType
                else -> null
            }

            _root_ide_package_.io.github.sds100.keymapper.base.trigger.ConfigTriggerState.Loaded(
                triggerKeys = triggerKeys,
                isReorderingEnabled = isReorderingEnabled,
                clickTypeButtons = clickTypeButtons,
                checkedClickType = checkedClickType,
                triggerModeButtonsEnabled = triggerModeButtonsEnabled,
                triggerModeButtonsVisible = triggerModeButtonsVisible,
                checkedTriggerMode = trigger.mode,
                shortcuts = triggerKeyShortcuts,
                showAdvancedTriggersTapTarget = showAdvancedTriggersTapTarget,
            )
        }
    }

    private suspend fun buildKeyOptionsUiState(
        keyMapState: State<KeyMap>,
        triggerKeyUid: String?,
    ): _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState? {
        if (triggerKeyUid == null) {
            return null
        }

        when (keyMapState) {
            State.Loading -> return null
            is State.Data -> {
                val trigger = keyMapState.data.trigger
                val key = trigger.keys.find { it.uid == triggerKeyUid }
                    ?: return null

                val showClickTypes = trigger.mode is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode.Sequence

                when (key) {
                    is _root_ide_package_.io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey -> {
                        val showDeviceDescriptors = displayKeyMap.showDeviceDescriptors.first()
                        val deviceListItems: List<CheckBoxListItem> =
                            config.getAvailableTriggerKeyDevices()
                                .map { device: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice ->
                                    buildDeviceListItem(
                                        device = device,
                                        showDeviceDescriptors = showDeviceDescriptors,
                                        isChecked = key.device == device,
                                    )
                                }

                        return _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState.KeyCode(
                            doNotRemapChecked = !key.consumeEvent,
                            clickType = key.clickType,
                            showClickTypes = showClickTypes,
                            devices = deviceListItems,
                        )
                    }

                    is _root_ide_package_.io.github.sds100.keymapper.base.trigger.AssistantTriggerKey -> {
                        return _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState.Assistant(
                            assistantType = key.type,
                            clickType = key.clickType,
                        )
                    }

                    is _root_ide_package_.io.github.sds100.keymapper.base.trigger.FloatingButtonKey -> {
                        return _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState.FloatingButton(
                            clickType = key.clickType,
                            showClickTypes = showClickTypes,
                            isPurchased = displayKeyMap.isFloatingButtonsPurchased(),
                        )
                    }

                    is _root_ide_package_.io.github.sds100.keymapper.base.trigger.FingerprintTriggerKey -> {
                        return _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState.FingerprintGesture(
                            gestureType = key.type,
                            clickType = key.clickType,
                        )
                    }
                }
            }
        }
    }

    private fun buildDeviceListItem(
        device: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice,
        isChecked: Boolean,
        showDeviceDescriptors: Boolean,
    ): CheckBoxListItem {
        return when (device) {
            _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.Any -> CheckBoxListItem(
                id = _root_ide_package_.io.github.sds100.keymapper.base.trigger.BaseConfigTriggerViewModel.Companion.DEVICE_ID_ANY,
                isChecked = isChecked,
                label = getString(R.string.any_device),
            )

            _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.Internal -> CheckBoxListItem(
                id = _root_ide_package_.io.github.sds100.keymapper.base.trigger.BaseConfigTriggerViewModel.Companion.DEVICE_ID_INTERNAL,
                isChecked = isChecked,
                label = getString(R.string.this_device),
            )

            is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.External -> {
                val name = if (showDeviceDescriptors) {
                    InputDeviceUtils.appendDeviceDescriptorToName(
                        device.descriptor,
                        device.name,
                    )
                } else {
                    device.name
                }

                CheckBoxListItem(
                    id = device.descriptor,
                    isChecked = isChecked,
                    label = name,
                )
            }
        }
    }

    private suspend fun onTriggerModeChanged(mode: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode) {
        if (mode is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode.Parallel) {
            if (onboarding.shownParallelTriggerOrderExplanation) {
                return
            }

            val dialog = PopupUi.Ok(
                message = getString(R.string.dialog_message_parallel_trigger_order),
            )

            showPopup("parallel_trigger_order", dialog) ?: return

            onboarding.shownParallelTriggerOrderExplanation = true
        }

        if (mode is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode.Sequence) {
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

    private suspend fun onRecordTriggerKey(key: _root_ide_package_.io.github.sds100.keymapper.base.trigger.RecordedKey) {
        // Add the trigger key before showing the dialog so it doesn't
        // need to be dismissed before it is added.
        config.addKeyCodeTriggerKey(key.keyCode, key.device, key.detectionSource)

        if (key.keyCode >= InputEventUtils.KEYCODE_TO_SCANCODE_OFFSET || key.keyCode < 0) {
            if (onboarding.shownKeyCodeToScanCodeTriggerExplanation) {
                return
            }

            val dialog = PopupUi.Dialog(
                title = getString(R.string.dialog_title_keycode_to_scancode_trigger_explanation),
                message = getString(R.string.dialog_message_keycode_to_scancode_trigger_explanation),
                positiveButtonText = getString(R.string.pos_understood),
            )

            val response = showPopup("keycode_to_scancode_message", dialog)

            if (response == DialogResponse.POSITIVE) {
                onboarding.shownKeyCodeToScanCodeTriggerExplanation = true
            }
        }

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
        if (key.detectionSource == _root_ide_package_.io.github.sds100.keymapper.base.trigger.KeyEventDetectionSource.INPUT_METHOD && displayKeyMap.showTriggerKeyboardIconExplanation.first()) {
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

    fun onParallelRadioButtonChecked() {
        config.setParallelTriggerMode()
    }

    fun onSequenceRadioButtonChecked() {
        config.setSequenceTriggerMode()
    }

    fun onClickTypeRadioButtonChecked(clickType: ClickType) {
        when (clickType) {
            ClickType.SHORT_PRESS -> config.setTriggerShortPress()
            ClickType.LONG_PRESS -> config.setTriggerLongPress()
            ClickType.DOUBLE_PRESS -> config.setTriggerDoublePress()
        }
    }

    fun onRemoveKeyClick(uid: String) = config.removeTriggerKey(uid)
    fun onMoveTriggerKey(fromIndex: Int, toIndex: Int) = config.moveTriggerKey(fromIndex, toIndex)

    fun onTriggerKeyOptionsClick(id: String) {
        triggerKeyOptionsUid.update { id }
    }

    fun onDismissTriggerKeyOptions() {
        triggerKeyOptionsUid.update { null }
    }

    fun onCheckDoNotRemap(isChecked: Boolean) {
        triggerKeyOptionsUid.value?.let { config.setTriggerKeyConsumeKeyEvent(it, !isChecked) }
    }

    fun onSelectKeyClickType(clickType: ClickType) {
        triggerKeyOptionsUid.value?.let { config.setTriggerKeyClickType(it, clickType) }
    }

    fun onSelectTriggerKeyDevice(descriptor: String) {
        triggerKeyOptionsUid.value?.let { triggerKeyUid ->
            val device = when (descriptor) {
                _root_ide_package_.io.github.sds100.keymapper.base.trigger.BaseConfigTriggerViewModel.Companion.DEVICE_ID_ANY -> _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.Any
                _root_ide_package_.io.github.sds100.keymapper.base.trigger.BaseConfigTriggerViewModel.Companion.DEVICE_ID_INTERNAL -> _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.Internal
                else -> {
                    val device = config.getAvailableTriggerKeyDevices()
                        .filterIsInstance<_root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.External>()
                        .firstOrNull { it.descriptor == descriptor }
                        ?: return

                    _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.External(
                        device.descriptor,
                        device.name
                    )
                }
            }

            config.setTriggerKeyDevice(
                triggerKeyUid,
                device,
            )
        }
    }

    fun onSelectTriggerKeyAssistantType(type: _root_ide_package_.io.github.sds100.keymapper.base.trigger.AssistantTriggerType) {
        triggerKeyOptionsUid.value?.let { triggerKeyUid ->
            config.setAssistantTriggerKeyType(triggerKeyUid, type)
        }
    }

    fun onSelectFingerprintGestureType(type: FingerprintGestureType) {
        triggerKeyOptionsUid.value?.let { triggerKeyUid ->
            config.setFingerprintGestureType(triggerKeyUid, type)
        }
    }

    fun onRecordTriggerButtonClick() {
        coroutineScope.launch {
            val recordTriggerState = recordTrigger.state.firstOrNull() ?: return@launch

            val result = when (recordTriggerState) {
                is _root_ide_package_.io.github.sds100.keymapper.base.trigger.RecordTriggerState.CountingDown -> {
                    isRecordingCompletionUserInitiated = true
                    recordTrigger.stopRecording()
                }

                is _root_ide_package_.io.github.sds100.keymapper.base.trigger.RecordTriggerState.Completed,
                _root_ide_package_.io.github.sds100.keymapper.base.trigger.RecordTriggerState.Idle,
                -> recordTrigger.startRecording()
            }

            // Show dialog if the accessibility service is disabled or crashed
            handleServiceEventResult(result)
        }
    }

    suspend fun handleServiceEventResult(result: Result<*>) {
        if (result is Error.AccessibilityServiceDisabled) {
            ViewModelHelper.handleAccessibilityServiceStoppedDialog(
                resourceProvider = this@BaseConfigTriggerViewModel,
                popupViewModel = this@BaseConfigTriggerViewModel,
                startService = displayKeyMap::startAccessibilityService,
            )
        }

        if (result is Error.AccessibilityServiceCrashed) {
            ViewModelHelper.handleAccessibilityServiceCrashedDialog(
                resourceProvider = this@BaseConfigTriggerViewModel,
                popupViewModel = this@BaseConfigTriggerViewModel,
                restartService = displayKeyMap::restartAccessibilityService,
            )
        }
    }

    open fun onTriggerErrorClick(error: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError) {
        coroutineScope.launch {
            when (error) {
                _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError.DND_ACCESS_DENIED ->
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@BaseConfigTriggerViewModel,
                        popupViewModel = this@BaseConfigTriggerViewModel,
                        neverShowDndTriggerErrorAgain = { displayKeyMap.neverShowDndTriggerError() },
                        fixError = { displayKeyMap.fixTriggerError(error) },
                    )

                _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError.DPAD_IME_NOT_SELECTED -> {
                    showDpadTriggerSetupBottomSheet = true
                }

                else -> displayKeyMap.fixTriggerError(error)
            }
        }
    }

    private fun createListItems(
        keyMap: KeyMap,
        showDeviceDescriptors: Boolean,
        shortcutCount: Int,
        triggerErrorSnapshot: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerErrorSnapshot,
    ): List<_root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel> {
        val trigger = keyMap.trigger

        return trigger.keys.mapIndexed { index, key ->
            val error = triggerErrorSnapshot.getTriggerError(keyMap, key)

            val clickType = if (trigger.mode is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode.Parallel) {
                trigger.mode.clickType
            } else {
                key.clickType
            }

            val linkType = when {
                trigger.mode is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode.Sequence && (index < trigger.keys.lastIndex) -> LinkType.ARROW
                (index < trigger.keys.lastIndex) -> LinkType.PLUS
                else -> LinkType.HIDDEN
            }

            when (key) {
                is _root_ide_package_.io.github.sds100.keymapper.base.trigger.AssistantTriggerKey -> _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel.Assistant(
                    id = key.uid,
                    assistantType = key.type,
                    clickType = clickType,
                    linkType = linkType,
                    error = error,
                )

                is _root_ide_package_.io.github.sds100.keymapper.base.trigger.FingerprintTriggerKey -> _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel.FingerprintGesture(
                    id = key.uid,
                    gestureType = key.type,
                    clickType = clickType,
                    linkType = linkType,
                    error = error,
                )

                is _root_ide_package_.io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey -> _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel.KeyCode(
                    id = key.uid,
                    keyName = getTriggerKeyName(key),
                    clickType = clickType,
                    extraInfo = getTriggerKeyExtraInfo(
                        key,
                        showDeviceDescriptors,
                    ).takeIf { it.isNotBlank() },
                    linkType = linkType,
                    error = error,
                )

                is _root_ide_package_.io.github.sds100.keymapper.base.trigger.FloatingButtonKey -> {
                    if (key.button == null) {
                        _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel.FloatingButtonDeleted(
                            id = key.uid,
                            clickType = clickType,
                            linkType = linkType,
                        )
                    } else {
                        _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel.FloatingButton(
                            id = key.uid,
                            buttonName = key.button.appearance.text,
                            layoutName = key.button.layoutName,
                            clickType = clickType,
                            linkType = linkType,
                            error = error,
                        )
                    }
                }
            }
        }
    }

    private fun getTriggerKeyExtraInfo(
        key: _root_ide_package_.io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey,
        showDeviceDescriptors: Boolean,
    ): String {
        return buildString {
            append(getTriggerKeyDeviceName(key.device, showDeviceDescriptors))
            val midDot = getString(R.string.middot)

            if (!key.consumeEvent) {
                append(" $midDot ${getString(R.string.flag_dont_override_default_action)}")
            }
        }
    }

    private fun getTriggerKeyName(key: _root_ide_package_.io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey): String {
        return buildString {
            append(InputEventUtils.keyCodeToString(key.keyCode))

            if (key.detectionSource == _root_ide_package_.io.github.sds100.keymapper.base.trigger.KeyEventDetectionSource.INPUT_METHOD) {
                val midDot = getString(R.string.middot)
                append(" $midDot ${getString(R.string.flag_detect_from_input_method)}")
            }
        }
    }

    private fun getTriggerKeyDeviceName(
        device: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice,
        showDeviceDescriptors: Boolean,
    ): String = when (device) {
        is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.Internal -> getString(R.string.this_device)
        is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.Any -> getString(R.string.any_device)
        is _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyDevice.External -> {
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
        coroutineScope.launch {
            setupGuiKeyboard.enableInputMethod()
        }
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

    fun onRecordTriggerTapTargetCompleted() {
        onboarding.completedTapTarget(OnboardingTapTarget.RECORD_TRIGGER)
    }

    fun onSkipTapTargetClick() {
        onboarding.skipTapTargetOnboarding()
    }

    fun onAdvancedTriggersTapTargetCompleted() {
        onboarding.completedTapTarget(OnboardingTapTarget.ADVANCED_TRIGGERS)
    }
}

sealed class ConfigTriggerState {
    abstract val shortcuts: Set<ShortcutModel<_root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut>>
    abstract val showAdvancedTriggersTapTarget: Boolean

    data class Empty(
        override val shortcuts: Set<ShortcutModel<_root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut>> = emptySet(),
        val showRecordTriggerTapTarget: Boolean = false,
        override val showAdvancedTriggersTapTarget: Boolean = false,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.ConfigTriggerState()

    data class Loaded(
        val triggerKeys: List<_root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel> = emptyList(),
        val isReorderingEnabled: Boolean = false,
        val clickTypeButtons: Set<ClickType> = emptySet(),
        val checkedClickType: ClickType? = null,
        val checkedTriggerMode: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode = _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerMode.Undefined,
        val triggerModeButtonsEnabled: Boolean = false,
        val triggerModeButtonsVisible: Boolean = false,
        override val shortcuts: Set<ShortcutModel<_root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyShortcut>> = emptySet(),
        override val showAdvancedTriggersTapTarget: Boolean = false,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.ConfigTriggerState()
}

sealed class TriggerKeyListItemModel {
    abstract val id: String
    abstract val linkType: LinkType
    abstract val error: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError?
    abstract val clickType: ClickType

    data class KeyCode(
        override val id: String,
        override val linkType: LinkType,
        val keyName: String,
        override val clickType: ClickType,
        val extraInfo: String?,
        override val error: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError?,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel()

    data class Assistant(
        override val id: String,
        override val linkType: LinkType,
        val assistantType: _root_ide_package_.io.github.sds100.keymapper.base.trigger.AssistantTriggerType,
        override val clickType: ClickType,
        override val error: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError?,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel()

    data class FingerprintGesture(
        override val id: String,
        override val linkType: LinkType,
        val gestureType: FingerprintGestureType,
        override val clickType: ClickType,
        override val error: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError?,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel()

    data class FloatingButton(
        override val id: String,
        override val linkType: LinkType,
        val buttonName: String,
        val layoutName: String,
        override val clickType: ClickType,
        override val error: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError?,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel()

    data class FloatingButtonDeleted(
        override val id: String,
        override val linkType: LinkType,
        override val clickType: ClickType,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyListItemModel() {
        override val error: _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError =
            _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerError.FLOATING_BUTTON_DELETED
    }
}

sealed class TriggerKeyOptionsState {
    abstract val clickType: ClickType
    abstract val showClickTypes: Boolean
    abstract val showLongPressClickType: Boolean

    data class KeyCode(
        val doNotRemapChecked: Boolean = false,
        override val clickType: ClickType,
        override val showClickTypes: Boolean,
        val devices: List<CheckBoxListItem>,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState() {
        override val showLongPressClickType: Boolean = true
    }

    data class Assistant(
        val assistantType: _root_ide_package_.io.github.sds100.keymapper.base.trigger.AssistantTriggerType,
        override val clickType: ClickType,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState() {
        override val showClickTypes: Boolean = false
        override val showLongPressClickType: Boolean = false
    }

    data class FingerprintGesture(
        val gestureType: FingerprintGestureType,
        override val clickType: ClickType,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState() {
        override val showClickTypes: Boolean = false
        override val showLongPressClickType: Boolean = false
    }

    data class FloatingButton(
        override val clickType: ClickType,
        override val showClickTypes: Boolean,
        val isPurchased: Boolean,
    ) : _root_ide_package_.io.github.sds100.keymapper.base.trigger.TriggerKeyOptionsState() {
        override val showLongPressClickType: Boolean = true
    }
}
