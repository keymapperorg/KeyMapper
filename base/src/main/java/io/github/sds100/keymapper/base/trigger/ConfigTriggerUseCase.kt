package io.github.sds100.keymapper.base.trigger

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.floating.FloatingButtonEntityMapper
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.base.keymaps.GetDefaultKeyMapOptionsUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.InputDeviceUtils
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.FloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ViewModelScoped
class ConfigTriggerUseCaseImpl @Inject constructor(
    private val state: ConfigKeyMapState,
    private val preferenceRepository: PreferenceRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val devicesAdapter: DevicesAdapter,
    private val floatingLayoutRepository: FloatingLayoutRepository,
    private val getDefaultKeyMapOptionsUseCase: GetDefaultKeyMapOptionsUseCase
) : ConfigTriggerUseCase, GetDefaultKeyMapOptionsUseCase by getDefaultKeyMapOptionsUseCase {
    override val keyMap: StateFlow<State<KeyMap>> = state.keyMap

    override val floatingButtonToUse: MutableStateFlow<String?> = state.floatingButtonToUse

    private val showDeviceDescriptors: Flow<Boolean> =
        preferenceRepository.get(Keys.showDeviceDescriptors).map { it == true }

    override fun setEnabled(enabled: Boolean) {
        state.update { it.copy(isEnabled = enabled) }
    }

    override suspend fun getFloatingLayoutCount(): Int {
        return floatingLayoutRepository.count()
    }

    override suspend fun addFloatingButtonTriggerKey(buttonUid: String) {
        floatingButtonToUse.update { null }

        val button = floatingButtonRepository.get(buttonUid)
            ?.let { entity ->
                FloatingButtonEntityMapper.fromEntity(
                    entity.button,
                    entity.layout.name,
                )
            }

        updateTrigger { trigger ->
            val clickType = when (trigger.mode) {
                is TriggerMode.Parallel -> trigger.mode.clickType
                TriggerMode.Sequence -> ClickType.SHORT_PRESS
                TriggerMode.Undefined -> ClickType.SHORT_PRESS
            }

            // Check whether the trigger already contains the key because if so
            // then it must be converted to a sequence trigger.
            val containsKey = trigger.keys
                .filterIsInstance<FloatingButtonKey>()
                .any { keyToCompare -> keyToCompare.buttonUid == buttonUid }

            val triggerKey = FloatingButtonKey(
                buttonUid = buttonUid,
                button = button,
                clickType = clickType,
            )

            var newKeys = trigger.keys.plus(triggerKey)

            val newMode = when {
                trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
                newKeys.size <= 1 -> TriggerMode.Undefined

                /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
                because this is what most users are expecting when they make a trigger with multiple keys */
                newKeys.size == 2 && !containsKey -> {
                    newKeys = newKeys.map { it.setClickType(triggerKey.clickType) }
                    TriggerMode.Parallel(triggerKey.clickType)
                }

                else -> trigger.mode
            }

            trigger.copy(keys = newKeys, mode = newMode)
        }
    }

    override fun addAssistantTriggerKey(type: AssistantTriggerType) = updateTrigger { trigger ->
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsAssistantKey = trigger.keys.any { it is AssistantTriggerKey }

        val triggerKey = AssistantTriggerKey(type = type, clickType = clickType)

        val newKeys = trigger.keys.plus(triggerKey).map { it.setClickType(ClickType.SHORT_PRESS) }

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsAssistantKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys.

            It must be a short press because long pressing the assistant key isn't supported.
             */
            !containsAssistantKey -> TriggerMode.Parallel(ClickType.SHORT_PRESS)
            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun addFingerprintGesture(type: FingerprintGestureType) = updateTrigger { trigger ->
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsFingerprintGesture = trigger.keys.any { it is FingerprintTriggerKey }

        val triggerKey = FingerprintTriggerKey(type = type, clickType = clickType)

        val newKeys = trigger.keys.plus(triggerKey).map { it.setClickType(ClickType.SHORT_PRESS) }

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsFingerprintGesture -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys.

            It must be a short press because long pressing the assistant key isn't supported.
             */
            !containsFingerprintGesture -> TriggerMode.Parallel(ClickType.SHORT_PRESS)
            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun addKeyEventTriggerKey(
        keyCode: Int,
        scanCode: Int,
        device: KeyEventTriggerDevice,
        requiresIme: Boolean,
    ) = updateTrigger { trigger ->
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsKey = trigger.keys
            .filterIsInstance<KeyEventTriggerKey>()
            .any { keyToCompare ->
                keyToCompare.keyCode == keyCode && keyToCompare.device.isSameDevice(device)
            }

        var consumeKeyEvent = true

        // Issue #753
        if (KeyEventUtils.isModifierKey(keyCode)) {
            consumeKeyEvent = false
        }

        val triggerKey = KeyEventTriggerKey(
            keyCode = keyCode,
            device = device,
            clickType = clickType,
            consumeEvent = consumeKeyEvent,
            requiresIme = requiresIme,
        )

        var newKeys = trigger.keys.filter { it !is EvdevTriggerKey }.plus(triggerKey)

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys */
            newKeys.size == 2 && !containsKey -> {
                newKeys = newKeys.map { it.setClickType(triggerKey.clickType) }
                TriggerMode.Parallel(triggerKey.clickType)
            }

            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun addEvdevTriggerKey(
        keyCode: Int,
        scanCode: Int,
        device: EvdevDeviceInfo,
    ) = updateTrigger { trigger ->
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsKey = trigger.keys
            .filterIsInstance<EvdevTriggerKey>()
            .any { keyToCompare ->
                keyToCompare.keyCode == keyCode && keyToCompare.device == device
            }

        val triggerKey = EvdevTriggerKey(
            keyCode = keyCode,
            scanCode = scanCode,
            device = device,
            clickType = clickType,
            consumeEvent = true,
        )

        var newKeys = trigger.keys.filter { it !is KeyEventTriggerKey }.plus(triggerKey)

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys */
            newKeys.size == 2 && !containsKey -> {
                newKeys = newKeys.map { it.setClickType(triggerKey.clickType) }
                TriggerMode.Parallel(triggerKey.clickType)
            }

            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun removeTriggerKey(uid: String) = updateTrigger { trigger ->
        val newKeys = trigger.keys.toMutableList().apply {
            removeAll { it.uid == uid }
        }

        val newMode = when {
            newKeys.size <= 1 -> TriggerMode.Undefined
            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun moveTriggerKey(fromIndex: Int, toIndex: Int) = updateTrigger { trigger ->
        trigger.copy(
            keys = trigger.keys.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            },
        )
    }

    override fun getTriggerKey(uid: String): TriggerKey? {
        return state.keyMap.value.dataOrNull()?.trigger?.keys?.find { it.uid == uid }
    }

    override fun setParallelTriggerMode() = updateTrigger { trigger ->
        if (trigger.mode is TriggerMode.Parallel) {
            return@updateTrigger trigger
        }

        // undefined mode only allowed if one or no keys
        if (trigger.keys.size <= 1) {
            return@updateTrigger trigger.copy(mode = TriggerMode.Undefined)
        }

        val oldKeys = trigger.keys
        var newKeys = oldKeys

        // set all the keys to a short press if coming from a non-parallel trigger
        // because they must all be the same click type and can't all be double pressed
        newKeys = newKeys
            .map { key -> key.setClickType(clickType = ClickType.SHORT_PRESS) }
            // remove duplicates of keys that have the same keycode and device id
            .distinctBy { key ->
                when (key) {
                    // You can't mix assistant trigger types in a parallel trigger because there is no notion of a "down" key event, which means they can't be pressed at the same time
                    is AssistantTriggerKey, is FingerprintTriggerKey -> 0
                    is KeyEventTriggerKey -> Pair(
                        key.keyCode,
                        key.device,
                    )

                    is FloatingButtonKey -> key.buttonUid
                    is EvdevTriggerKey -> Pair(
                        key.keyCode,
                        key.device,
                    )
                }
            }

        val newMode = if (newKeys.size <= 1) {
            TriggerMode.Undefined
        } else {
            TriggerMode.Parallel(newKeys[0].clickType)
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun setSequenceTriggerMode() = updateTrigger { trigger ->
        if (trigger.mode == TriggerMode.Sequence) return@updateTrigger trigger
        // undefined mode only allowed if one or no keys
        if (trigger.keys.size <= 1) {
            return@updateTrigger trigger.copy(mode = TriggerMode.Undefined)
        }

        trigger.copy(mode = TriggerMode.Sequence)
    }

    override fun setUndefinedTriggerMode() = updateTrigger { trigger ->
        if (trigger.mode == TriggerMode.Undefined) return@updateTrigger trigger

        // undefined mode only allowed if one or no keys
        if (trigger.keys.size > 1) {
            return@updateTrigger trigger
        }

        trigger.copy(mode = TriggerMode.Undefined)
    }

    override fun setTriggerShortPress() {
        updateTrigger { oldTrigger ->
            if (oldTrigger.mode == TriggerMode.Sequence) {
                return@updateTrigger oldTrigger
            }

            val newKeys = oldTrigger.keys.map { it.setClickType(clickType = ClickType.SHORT_PRESS) }
            val newMode = if (newKeys.size <= 1) {
                TriggerMode.Undefined
            } else {
                TriggerMode.Parallel(ClickType.SHORT_PRESS)
            }
            oldTrigger.copy(keys = newKeys, mode = newMode)
        }
    }

    override fun setTriggerLongPress() {
        updateTrigger { trigger ->
            if (trigger.mode == TriggerMode.Sequence) {
                return@updateTrigger trigger
            }

            // You can't set the trigger to a long press if it contains a key
            // that isn't detected with key codes. This is because there aren't
            // separate key events for the up and down press that can be timed.
            if (trigger.keys.any { !it.allowedLongPress }) {
                return@updateTrigger trigger
            }

            val newKeys = trigger.keys.map { it.setClickType(clickType = ClickType.LONG_PRESS) }
            val newMode = if (newKeys.size <= 1) {
                TriggerMode.Undefined
            } else {
                TriggerMode.Parallel(ClickType.LONG_PRESS)
            }

            trigger.copy(keys = newKeys, mode = newMode)
        }
    }

    override fun setTriggerDoublePress() {
        updateTrigger { trigger ->
            if (trigger.mode != TriggerMode.Undefined) {
                return@updateTrigger trigger
            }

            if (trigger.keys.any { !it.allowedDoublePress }) {
                return@updateTrigger trigger
            }

            val newKeys = trigger.keys.map { it.setClickType(clickType = ClickType.DOUBLE_PRESS) }
            val newMode = TriggerMode.Undefined

            trigger.copy(keys = newKeys, mode = newMode)
        }
    }

    override fun setTriggerKeyClickType(keyUid: String, clickType: ClickType) {
        updateTriggerKey(keyUid) { key ->
            key.setClickType(clickType = clickType)
        }
    }

    override fun setTriggerKeyDevice(keyUid: String, device: KeyEventTriggerDevice) {
        updateTriggerKey(keyUid) { key ->
            if (key !is KeyEventTriggerKey) {
                throw IllegalArgumentException("You can not set the device for non KeyEventTriggerKeys.")
            }

            key.copy(device = device)
        }
    }

    override fun setTriggerKeyConsumeKeyEvent(keyUid: String, consumeKeyEvent: Boolean) {
        updateTriggerKey(keyUid) { key ->
            when (key) {
                is KeyEventTriggerKey -> {
                    key.copy(consumeEvent = consumeKeyEvent)
                }

                is EvdevTriggerKey -> {
                    key.copy(consumeEvent = consumeKeyEvent)
                }

                else -> {
                    key
                }
            }
        }
    }

    override fun setAssistantTriggerKeyType(keyUid: String, type: AssistantTriggerType) {
        updateTriggerKey(keyUid) { key ->
            if (key is AssistantTriggerKey) {
                key.copy(type = type)
            } else {
                key
            }
        }
    }

    override fun setFingerprintGestureType(keyUid: String, type: FingerprintGestureType) {
        updateTriggerKey(keyUid) { key ->
            if (key is FingerprintTriggerKey) {
                key.copy(type = type)
            } else {
                key
            }
        }
    }

    override fun setVibrateEnabled(enabled: Boolean) = updateTrigger { it.copy(vibrate = enabled) }

    override fun setVibrationDuration(duration: Int) = updateTrigger { trigger ->
        if (duration == defaultVibrateDuration.value) {
            trigger.copy(vibrateDuration = null)
        } else {
            trigger.copy(vibrateDuration = duration)
        }
    }

    override fun setLongPressDelay(delay: Int) = updateTrigger { trigger ->
        if (delay == defaultLongPressDelay.value) {
            trigger.copy(longPressDelay = null)
        } else {
            trigger.copy(longPressDelay = delay)
        }
    }

    override fun setDoublePressDelay(delay: Int) {
        updateTrigger { trigger ->
            if (delay == defaultDoublePressDelay.value) {
                trigger.copy(doublePressDelay = null)
            } else {
                trigger.copy(doublePressDelay = delay)
            }
        }
    }

    override fun setSequenceTriggerTimeout(delay: Int) {
        updateTrigger { trigger ->
            if (delay == defaultSequenceTriggerTimeout.value) {
                trigger.copy(sequenceTriggerTimeout = null)
            } else {
                trigger.copy(sequenceTriggerTimeout = delay)
            }
        }
    }

    override fun setLongPressDoubleVibrationEnabled(enabled: Boolean) {
        updateTrigger { it.copy(longPressDoubleVibration = enabled) }
    }

    override fun setTriggerWhenScreenOff(enabled: Boolean) {
        updateTrigger { it.copy(screenOffTrigger = enabled) }
    }

    override fun setTriggerFromOtherAppsEnabled(enabled: Boolean) {
        updateTrigger { it.copy(triggerFromOtherApps = enabled) }
    }

    override fun setShowToastEnabled(enabled: Boolean) {
        updateTrigger { it.copy(showToast = enabled) }
    }

    override fun getAvailableTriggerKeyDevices(): List<KeyEventTriggerDevice> {
        val externalKeyEventTriggerDevices = sequence {
            val inputDevices =
                devicesAdapter.connectedInputDevices.value.dataOrNull() ?: emptyList()

            val showDeviceDescriptors = showDeviceDescriptors.firstBlocking()

            inputDevices.forEach { device ->

                if (device.isExternal) {
                    val name = if (showDeviceDescriptors) {
                        InputDeviceUtils.appendDeviceDescriptorToName(
                            device.descriptor,
                            device.name,
                        )
                    } else {
                        device.name
                    }

                    yield(KeyEventTriggerDevice.External(device.descriptor, name))
                }
            }
        }

        return sequence {
            yield(KeyEventTriggerDevice.Internal)
            yield(KeyEventTriggerDevice.Any)
            yieldAll(externalKeyEventTriggerDevices)
        }.toList()
    }

    private fun updateTrigger(block: (trigger: Trigger) -> Trigger) {
        state.update { keyMap ->
            val newTrigger = block(keyMap.trigger)

            keyMap.copy(trigger = newTrigger)
        }
    }

    private fun updateTriggerKey(uid: String, block: (key: TriggerKey) -> TriggerKey) {
        updateTrigger { oldTrigger ->
            val newKeys = oldTrigger.keys.map {
                if (it.uid == uid) {
                    block.invoke(it)
                } else {
                    it
                }
            }

            oldTrigger.copy(keys = newKeys)
        }
    }

}

interface ConfigTriggerUseCase : GetDefaultKeyMapOptionsUseCase {

    val keyMap: StateFlow<State<KeyMap>>

    fun setEnabled(enabled: Boolean)

    // trigger
    fun addKeyEventTriggerKey(
        keyCode: Int,
        scanCode: Int,
        device: KeyEventTriggerDevice,
        requiresIme: Boolean,
    )

    suspend fun addFloatingButtonTriggerKey(buttonUid: String)
    fun addAssistantTriggerKey(type: AssistantTriggerType)
    fun addFingerprintGesture(type: FingerprintGestureType)
    fun addEvdevTriggerKey(
        keyCode: Int,
        scanCode: Int,
        device: EvdevDeviceInfo,
    )

    fun removeTriggerKey(uid: String)
    fun getTriggerKey(uid: String): TriggerKey?
    fun moveTriggerKey(fromIndex: Int, toIndex: Int)

    fun setParallelTriggerMode()
    fun setSequenceTriggerMode()
    fun setUndefinedTriggerMode()

    fun setTriggerShortPress()
    fun setTriggerLongPress()
    fun setTriggerDoublePress()

    fun setTriggerKeyClickType(keyUid: String, clickType: ClickType)
    fun setTriggerKeyDevice(keyUid: String, device: KeyEventTriggerDevice)
    fun setTriggerKeyConsumeKeyEvent(keyUid: String, consumeKeyEvent: Boolean)
    fun setAssistantTriggerKeyType(keyUid: String, type: AssistantTriggerType)
    fun setFingerprintGestureType(keyUid: String, type: FingerprintGestureType)

    fun setVibrateEnabled(enabled: Boolean)
    fun setVibrationDuration(duration: Int)
    fun setLongPressDelay(delay: Int)
    fun setDoublePressDelay(delay: Int)
    fun setSequenceTriggerTimeout(delay: Int)
    fun setLongPressDoubleVibrationEnabled(enabled: Boolean)
    fun setTriggerWhenScreenOff(enabled: Boolean)
    fun setTriggerFromOtherAppsEnabled(enabled: Boolean)
    fun setShowToastEnabled(enabled: Boolean)

    fun getAvailableTriggerKeyDevices(): List<KeyEventTriggerDevice>

    val floatingButtonToUse: MutableStateFlow<String?>
    suspend fun getFloatingLayoutCount(): Int
}