package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapOptionsViewModel
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.onboarding.OnboardingTipDelegate
import io.github.sds100.keymapper.base.onboarding.OnboardingTipDelegateImpl
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegate
import io.github.sds100.keymapper.base.shortcuts.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.CheckBoxListItem
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.LinkType
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.AccessibilityServiceError
import io.github.sds100.keymapper.common.utils.InputDeviceUtils
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.mapData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseConfigTriggerViewModel(
    private val onboarding: OnboardingUseCase,
    private val config: ConfigTriggerUseCase,
    private val recordTrigger: RecordTriggerController,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val displayKeyMap: DisplayKeyMapUseCase,
    private val fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    private val setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegate,
    onboardingTipDelegate: OnboardingTipDelegate,
    triggerSetupDelegate: TriggerSetupDelegate,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    SetupAccessibilityServiceDelegate by setupAccessibilityServiceDelegate,
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider,
    TriggerSetupDelegate by triggerSetupDelegate,
    OnboardingTipDelegate by onboardingTipDelegate {

    companion object {
        private const val DEVICE_ID_ANY = "any"
        private const val DEVICE_ID_INTERNAL = "internal"
    }

    val optionsViewModel = ConfigKeyMapOptionsViewModel(
        viewModelScope,
        config,
        displayKeyMap,
        createKeyMapShortcut,
        dialogProvider,
        resourceProvider,
    )

    private val _state: MutableStateFlow<State<ConfigTriggerState>> =
        MutableStateFlow(State.Loading)
    val state: StateFlow<State<ConfigTriggerState>> = _state.asStateFlow()

    val recordTriggerState: StateFlow<RecordTriggerState> = recordTrigger.state.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        RecordTriggerState.Idle,
    )

    val showFingerprintGesturesShortcut: StateFlow<Boolean> =
        fingerprintGesturesSupported.isSupported.map { it ?: false }
            .stateIn(viewModelScope, SharingStarted.Lazily, false)

    var showDiscoverTriggersBottomSheet: Boolean by mutableStateOf(false)

    val triggerKeyOptionsUid = MutableStateFlow<String?>(null)
    val triggerKeyOptionsState: StateFlow<TriggerKeyOptionsState?> =
        combine(config.keyMap, triggerKeyOptionsUid, transform = ::buildKeyOptionsUiState)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    /**
     * Check whether the user stopped the trigger recording countdown. This
     * distinction is important so that the bottom sheet describing what to do
     * when no buttons are recorded is not shown.
     */
    private var isRecordingCompletionUserInitiated: Boolean = false
    private val midDot = getString(R.string.middot)

    init {
        // IMPORTANT! Do not flow on another thread because this causes the drag and drop
        // animations to be more janky.
        combine(
            displayKeyMap.triggerErrorSnapshot,
            config.keyMap,
            displayKeyMap.showDeviceDescriptors,
        ) { triggerErrorSnapshot, keyMap, showDeviceDescriptors ->
            _state.update {
                buildUiState(
                    keyMap,
                    showDeviceDescriptors,
                    triggerErrorSnapshot,
                )
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            recordTrigger.onRecordKey.collect { key ->
                when (key) {
                    is RecordedKey.EvdevEvent -> onRecordEvdevEvent(key)
                    is RecordedKey.KeyEvent -> onRecordKeyEvent(key)
                }
            }
        }

        // Drop the first state in case it is in the Completed state so the
        // "button not detected" bottom sheet isn't shown when
        // the screen is opened.
        recordTrigger.state.drop(1).onEach { state ->
            if (state is RecordTriggerState.Completed &&
                state.recordedKeys.isEmpty() &&
                !isRecordingCompletionUserInitiated
            ) {
                showTriggerSetup(TriggerSetupShortcut.NOT_DETECTED)
            }

            // reset this field when recording has completed
            isRecordingCompletionUserInitiated = false
        }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        isRecordingCompletionUserInitiated = true
        recordTrigger.stopRecording()

        super.onCleared()
    }

    fun onAdvancedTriggersClick() {
        onboarding.viewedAdvancedTriggers()

        viewModelScope.launch {
            navigateToAdvancedTriggers("advanced_triggers_click")
        }
    }

    suspend fun navigateToAdvancedTriggers(navKey: String) {
        val result: TriggerSetupShortcut =
            navigate(navKey, NavDestination.AdvancedTriggers) ?: return

        showTriggerSetup(result)
    }

    private fun buildUiState(
        keyMapState: State<KeyMap>,
        showDeviceDescriptors: Boolean,
        triggerErrorSnapshot: TriggerErrorSnapshot,
    ): State<ConfigTriggerState> {
        return keyMapState.mapData { keyMap ->
            val trigger = keyMap.trigger

            if (trigger.keys.isEmpty()) {
                return@mapData ConfigTriggerState.Empty
            }

            val triggerKeys =
                createListItems(
                    keyMap,
                    showDeviceDescriptors,
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

            if (trigger.keys.isNotEmpty() &&
                trigger.mode !is TriggerMode.Sequence &&
                trigger.keys.all { it.allowedLongPress }
            ) {
                clickTypeButtons.add(ClickType.SHORT_PRESS)
                clickTypeButtons.add(ClickType.LONG_PRESS)
            }

            val checkedClickType: ClickType? = when {
                trigger.mode is TriggerMode.Parallel -> trigger.mode.clickType
                trigger.keys.size == 1 -> trigger.keys[0].clickType
                else -> null
            }

            ConfigTriggerState.Loaded(
                triggerKeys = triggerKeys,
                isReorderingEnabled = isReorderingEnabled,
                clickTypeButtons = clickTypeButtons,
                checkedClickType = checkedClickType,
                triggerModeButtonsEnabled = triggerModeButtonsEnabled,
                triggerModeButtonsVisible = triggerModeButtonsVisible,
                checkedTriggerMode = trigger.mode,
            )
        }
    }

    private suspend fun buildKeyOptionsUiState(
        keyMapState: State<KeyMap>,
        triggerKeyUid: String?,
    ): TriggerKeyOptionsState? {
        if (triggerKeyUid == null) {
            return null
        }

        when (keyMapState) {
            State.Loading -> return null
            is State.Data -> {
                val trigger = keyMapState.data.trigger
                val key = trigger.keys.find { it.uid == triggerKeyUid }
                    ?: return null

                val showClickTypes = trigger.mode is TriggerMode.Sequence

                when (key) {
                    is KeyEventTriggerKey -> {
                        val showDeviceDescriptors = displayKeyMap.showDeviceDescriptors.first()
                        val deviceListItems: List<CheckBoxListItem> =
                            config.getAvailableTriggerKeyDevices()
                                .map { device: KeyEventTriggerDevice ->
                                    buildDeviceListItem(
                                        device = device,
                                        showDeviceDescriptors = showDeviceDescriptors,
                                        isChecked = key.device == device,
                                    )
                                }

                        return TriggerKeyOptionsState.KeyEvent(
                            doNotRemapChecked = !key.consumeEvent,
                            clickType = key.clickType,
                            showClickTypes = showClickTypes,
                            devices = deviceListItems,
                            keyCode = key.keyCode,
                            scanCode = key.scanCode,
                            isScanCodeDetectionSelected = key.detectWithScancode(),
                            isScanCodeSettingEnabled = key.isScanCodeDetectionUserConfigurable(),
                        )
                    }

                    is AssistantTriggerKey -> {
                        return TriggerKeyOptionsState.Assistant(
                            assistantType = key.type,
                            clickType = key.clickType,
                        )
                    }

                    is FloatingButtonKey -> {
                        return TriggerKeyOptionsState.FloatingButton(
                            clickType = key.clickType,
                            showClickTypes = showClickTypes,
                            isPurchased = displayKeyMap.isFloatingButtonsPurchased(),
                        )
                    }

                    is FingerprintTriggerKey -> {
                        return TriggerKeyOptionsState.FingerprintGesture(
                            gestureType = key.type,
                            clickType = key.clickType,
                        )
                    }

                    is EvdevTriggerKey -> {
                        return TriggerKeyOptionsState.EvdevEvent(
                            doNotRemapChecked = !key.consumeEvent,
                            clickType = key.clickType,
                            showClickTypes = showClickTypes,
                            keyCode = key.keyCode,
                            scanCode = key.scanCode,
                            isScanCodeDetectionSelected = key.detectWithScancode(),
                            isScanCodeSettingEnabled = key.isScanCodeDetectionUserConfigurable(),
                        )
                    }
                }
            }
        }
    }

    private fun buildDeviceListItem(
        device: KeyEventTriggerDevice,
        isChecked: Boolean,
        showDeviceDescriptors: Boolean,
    ): CheckBoxListItem {
        return when (device) {
            KeyEventTriggerDevice.Any -> CheckBoxListItem(
                id = DEVICE_ID_ANY,
                isChecked = isChecked,
                label = getString(R.string.any_device),
            )

            KeyEventTriggerDevice.Internal -> CheckBoxListItem(
                id = DEVICE_ID_INTERNAL,
                isChecked = isChecked,
                label = getString(R.string.this_device),
            )

            is KeyEventTriggerDevice.External -> {
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

    private suspend fun onRecordKeyEvent(key: RecordedKey.KeyEvent) {
        val triggerDevice = if (key.isExternalDevice) {
            KeyEventTriggerDevice.External(key.deviceDescriptor, key.deviceName)
        } else {
            KeyEventTriggerDevice.Internal
        }

        config.addKeyEventTriggerKey(
            key.keyCode,
            key.scanCode,
            triggerDevice,
            key.detectionSource != InputEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
    }

    private suspend fun onRecordEvdevEvent(key: RecordedKey.EvdevEvent) {
        config.addEvdevTriggerKey(
            key.keyCode,
            key.scanCode,
            EvdevDeviceInfo(
                name = key.device.name,
                bus = key.device.bus,
                vendor = key.device.vendor,
                product = key.device.product,
            ),
        )

        if (key.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            key.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        ) {
            neverShowTipAgain(OnboardingTipDelegateImpl.VOLUME_BUTTONS_PRO_MODE_TIP_ID)
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
                DEVICE_ID_ANY -> KeyEventTriggerDevice.Any
                DEVICE_ID_INTERNAL -> KeyEventTriggerDevice.Internal
                else -> {
                    val device = config.getAvailableTriggerKeyDevices()
                        .filterIsInstance<KeyEventTriggerDevice.External>()
                        .firstOrNull { it.descriptor == descriptor }
                        ?: return

                    KeyEventTriggerDevice.External(
                        device.descriptor,
                        device.name,
                    )
                }
            }

            config.setTriggerKeyDevice(
                triggerKeyUid,
                device,
            )
        }
    }

    fun onSelectTriggerKeyAssistantType(type: AssistantTriggerType) {
        triggerKeyOptionsUid.value?.let { triggerKeyUid ->
            config.setAssistantTriggerKeyType(triggerKeyUid, type)
        }
    }

    fun onSelectFingerprintGestureType(type: FingerprintGestureType) {
        triggerKeyOptionsUid.value?.let { triggerKeyUid ->
            config.setFingerprintGestureType(triggerKeyUid, type)
        }
    }

    fun onSelectScanCodeDetection(isSelected: Boolean) {
        triggerKeyOptionsUid.value?.let { triggerKeyUid ->
            config.setScanCodeDetectionEnabled(triggerKeyUid, isSelected)
        }
    }

    fun onRecordTriggerButtonClick() {
        viewModelScope.launch {
            val recordTriggerState = recordTrigger.state.firstOrNull() ?: return@launch

            val result: KMResult<*> = when (recordTriggerState) {
                is RecordTriggerState.CountingDown -> {
                    isRecordingCompletionUserInitiated = true
                    recordTrigger.stopRecording()
                }

                is RecordTriggerState.Completed,
                RecordTriggerState.Idle,
                    -> recordTrigger.startRecording(enableEvdevRecording = false)
            }

            // Show dialog if the accessibility service is disabled or crashed
            handleServiceEventResult(result)
        }
    }

    fun handleServiceEventResult(result: KMResult<*>) {
        if (result is AccessibilityServiceError) {
            showFixAccessibilityServiceDialog(result)
        }
    }

    override fun onTipButtonClick(tipId: String) {
        when (tipId) {
            OnboardingTipDelegateImpl.CAPS_LOCK_PRO_MODE_COMPATIBILITY_TIP_ID -> {
                showTriggerSetup(TriggerSetupShortcut.KEYBOARD, forceProMode = true)
            }

            OnboardingTipDelegateImpl.VOLUME_BUTTONS_PRO_MODE_TIP_ID -> {
                showTriggerSetup(TriggerSetupShortcut.VOLUME, forceProMode = true)
            }
        }
    }

    open fun onTriggerErrorClick(error: TriggerError) {
        viewModelScope.launch {
            when (error) {
                TriggerError.DND_ACCESS_DENIED ->
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@BaseConfigTriggerViewModel,
                        dialogProvider = this@BaseConfigTriggerViewModel,
                        neverShowDndTriggerErrorAgain = {
                            displayKeyMap.neverShowDndTriggerError()
                        },
                        fixError = { displayKeyMap.fixTriggerError(error) },
                    )

                TriggerError.DPAD_IME_NOT_SELECTED -> {
                    showTriggerSetup(TriggerSetupShortcut.GAMEPAD)
                }

                else -> displayKeyMap.fixTriggerError(error)
            }
        }
    }

    private fun createListItems(
        keyMap: KeyMap,
        showDeviceDescriptors: Boolean,
        triggerErrorSnapshot: TriggerErrorSnapshot,
    ): List<TriggerKeyListItemModel> {
        val trigger = keyMap.trigger

        return trigger.keys.mapIndexed { index, key ->
            val error = triggerErrorSnapshot.getTriggerError(keyMap, key)

            val clickType = if (trigger.mode is TriggerMode.Parallel) {
                trigger.mode.clickType
            } else {
                key.clickType
            }

            val linkType = when (trigger.mode) {
                is TriggerMode.Sequence -> LinkType.ARROW
                else -> LinkType.PLUS
            }

            when (key) {
                is AssistantTriggerKey -> TriggerKeyListItemModel.Assistant(
                    id = key.uid,
                    assistantType = key.type,
                    clickType = clickType,
                    linkType = linkType,
                    error = error,
                )

                is FingerprintTriggerKey -> TriggerKeyListItemModel.FingerprintGesture(
                    id = key.uid,
                    gestureType = key.type,
                    clickType = clickType,
                    linkType = linkType,
                    error = error,
                )

                is KeyEventTriggerKey -> TriggerKeyListItemModel.KeyEvent(
                    id = key.uid,
                    keyName = getTriggerKeyName(key),
                    clickType = clickType,
                    extraInfo = getKeyEventTriggerKeyExtraInfo(
                        key,
                        showDeviceDescriptors,
                    ).takeIf { it.isNotBlank() },
                    linkType = linkType,
                    error = error,
                )

                is FloatingButtonKey -> {
                    if (key.button == null) {
                        TriggerKeyListItemModel.FloatingButtonDeleted(
                            id = key.uid,
                            clickType = clickType,
                            linkType = linkType,
                        )
                    } else {
                        TriggerKeyListItemModel.FloatingButton(
                            id = key.uid,
                            buttonName = key.button.appearance.text,
                            layoutName = key.button.layoutName,
                            clickType = clickType,
                            linkType = linkType,
                            error = error,
                        )
                    }
                }

                is EvdevTriggerKey -> TriggerKeyListItemModel.EvdevEvent(
                    id = key.uid,
                    keyName = key.getCodeLabel(this),
                    clickType = clickType,
                    extraInfo = getEvdevTriggerKeyExtraInfo(key),
                    linkType = linkType,
                    error = error,
                )
            }
        }
    }

    private fun getEvdevTriggerKeyExtraInfo(key: EvdevTriggerKey): String {
        return buildString {
            append(key.device.name)

            if (!key.consumeEvent) {
                append(" $midDot ${getString(R.string.flag_dont_override_default_action)}")
            }
        }
    }

    private fun getKeyEventTriggerKeyExtraInfo(
        key: KeyEventTriggerKey,
        showDeviceDescriptors: Boolean,
    ): String {
        return buildString {
            append(getTriggerKeyDeviceName(key.device, showDeviceDescriptors))

            if (!key.consumeEvent) {
                append(" $midDot ${getString(R.string.flag_dont_override_default_action)}")
            }
        }
    }

    private fun getTriggerKeyName(key: KeyEventTriggerKey): String {
        return buildString {
            append(key.getCodeLabel(this@BaseConfigTriggerViewModel))

            if (key.requiresIme) {
                append(" $midDot ${getString(R.string.flag_detect_from_input_method)}")
            }
        }
    }

    private fun getTriggerKeyDeviceName(
        device: KeyEventTriggerDevice,
        showDeviceDescriptors: Boolean,
    ): String = when (device) {
        is KeyEventTriggerDevice.Internal -> getString(R.string.this_device)
        is KeyEventTriggerDevice.Any -> getString(R.string.any_device)
        is KeyEventTriggerDevice.External -> {
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

    abstract fun onEditFloatingButtonClick()
    abstract fun onEditFloatingLayoutClick()
}

sealed class ConfigTriggerState {
    data object Empty : ConfigTriggerState()

    data class Loaded(
        val triggerKeys: List<TriggerKeyListItemModel> = emptyList(),
        val isReorderingEnabled: Boolean = false,
        val clickTypeButtons: Set<ClickType> = emptySet(),
        val checkedClickType: ClickType? = null,
        val checkedTriggerMode: TriggerMode = TriggerMode.Undefined,
        val triggerModeButtonsEnabled: Boolean = false,
        val triggerModeButtonsVisible: Boolean = false,
    ) : ConfigTriggerState()
}

sealed class TriggerKeyListItemModel {
    abstract val id: String
    abstract val linkType: LinkType
    abstract val error: TriggerError?
    abstract val clickType: ClickType

    data class KeyEvent(
        override val id: String,
        override val linkType: LinkType,
        val keyName: String,
        override val clickType: ClickType,
        val extraInfo: String?,
        override val error: TriggerError?,
    ) : TriggerKeyListItemModel()

    data class EvdevEvent(
        override val id: String,
        override val linkType: LinkType,
        val keyName: String,
        override val clickType: ClickType,
        val extraInfo: String?,
        override val error: TriggerError?,
    ) : TriggerKeyListItemModel()

    data class Assistant(
        override val id: String,
        override val linkType: LinkType,
        val assistantType: AssistantTriggerType,
        override val clickType: ClickType,
        override val error: TriggerError?,
    ) : TriggerKeyListItemModel()

    data class FingerprintGesture(
        override val id: String,
        override val linkType: LinkType,
        val gestureType: FingerprintGestureType,
        override val clickType: ClickType,
        override val error: TriggerError?,
    ) : TriggerKeyListItemModel()

    data class FloatingButton(
        override val id: String,
        override val linkType: LinkType,
        val buttonName: String,
        val layoutName: String,
        override val clickType: ClickType,
        override val error: TriggerError?,
    ) : TriggerKeyListItemModel()

    data class FloatingButtonDeleted(
        override val id: String,
        override val linkType: LinkType,
        override val clickType: ClickType,
    ) : TriggerKeyListItemModel() {
        override val error: TriggerError =
            TriggerError.FLOATING_BUTTON_DELETED
    }
}

sealed class TriggerKeyOptionsState {
    abstract val clickType: ClickType
    abstract val showClickTypes: Boolean
    abstract val showLongPressClickType: Boolean

    data class KeyEvent(
        val doNotRemapChecked: Boolean = false,
        override val clickType: ClickType,
        override val showClickTypes: Boolean,
        val devices: List<CheckBoxListItem>,
        val keyCode: Int,
        val scanCode: Int?,
        // Whether scan code is checked.
        val isScanCodeDetectionSelected: Boolean,
        // Whether the setting should be enabled and allow user interaction.
        val isScanCodeSettingEnabled: Boolean,
    ) : TriggerKeyOptionsState() {
        override val showLongPressClickType: Boolean = true
    }

    data class EvdevEvent(
        val doNotRemapChecked: Boolean = false,
        override val clickType: ClickType,
        override val showClickTypes: Boolean,
        val keyCode: Int,
        val scanCode: Int,
        // Whether scan code is checked.
        val isScanCodeDetectionSelected: Boolean,
        // Whether the setting should be enabled and allow user interaction.
        val isScanCodeSettingEnabled: Boolean,
    ) : TriggerKeyOptionsState() {
        override val showLongPressClickType: Boolean = true
    }

    data class Assistant(
        val assistantType: AssistantTriggerType,
        override val clickType: ClickType,
    ) : TriggerKeyOptionsState() {
        override val showClickTypes: Boolean = false
        override val showLongPressClickType: Boolean = false
    }

    data class FingerprintGesture(
        val gestureType: FingerprintGestureType,
        override val clickType: ClickType,
    ) : TriggerKeyOptionsState() {
        override val showClickTypes: Boolean = false
        override val showLongPressClickType: Boolean = false
    }

    data class FloatingButton(
        override val clickType: ClickType,
        override val showClickTypes: Boolean,
        val isPurchased: Boolean,
    ) : TriggerKeyOptionsState() {
        override val showLongPressClickType: Boolean = true
    }
}
