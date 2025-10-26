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
import io.github.sds100.keymapper.data.entities.AssistantTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.EvdevTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.FingerprintTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.FloatingButtonKeyEntity
import io.github.sds100.keymapper.data.entities.KeyEventTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.FloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@ViewModelScoped
class ConfigTriggerUseCaseImpl @Inject constructor(
    private val state: ConfigKeyMapState,
    private val preferenceRepository: PreferenceRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val devicesAdapter: DevicesAdapter,
    private val floatingLayoutRepository: FloatingLayoutRepository,
    private val getDefaultKeyMapOptionsUseCase: GetDefaultKeyMapOptionsUseCase,
    private val keyMapRepository: KeyMapRepository,
) : ConfigTriggerUseCase,
    GetDefaultKeyMapOptionsUseCase by getDefaultKeyMapOptionsUseCase {
    override val keyMap: StateFlow<State<KeyMap>> = state.keyMap

    override val floatingButtonToUse: MutableStateFlow<String?> = state.floatingButtonToUse

    private val showDeviceDescriptors: Flow<Boolean> =
        preferenceRepository.get(Keys.showDeviceDescriptors).map { it == true }

    private val delegate: ConfigTriggerDelegate = ConfigTriggerDelegate()

    // This class is viewmodel scoped so this will be recomputed each time
    // the user starts configuring a key map
    private val otherTriggerKeys: List<KeyCodeTriggerKey> by lazy {
        keyMapRepository.keyMapList
            .filterIsInstance<State.Data<List<KeyMapEntity>>>()
            .map { state -> state.data.flatMap { it.trigger.keys } }
            .map { keys ->
                keys
                    .mapNotNull { key ->
                        when (key) {
                            is EvdevTriggerKeyEntity -> EvdevTriggerKey.fromEntity(key)
                            is KeyEventTriggerKeyEntity -> KeyEventTriggerKey.fromEntity(key)
                            is AssistantTriggerKeyEntity, is FingerprintTriggerKeyEntity, is FloatingButtonKeyEntity -> null
                        }
                    }.filterIsInstance<KeyCodeTriggerKey>()
            }.firstBlocking()
    }

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
            delegate.addFloatingButtonTriggerKey(trigger, buttonUid, button)
        }
    }

    override fun addAssistantTriggerKey(type: AssistantTriggerType) = updateTrigger { trigger ->
        delegate.addAssistantTriggerKey(trigger, type)
    }

    override fun addFingerprintGesture(type: FingerprintGestureType) = updateTrigger { trigger ->
        delegate.addFingerprintGesture(trigger, type)
    }

    override suspend fun addKeyEventTriggerKey(
        keyCode: Int,
        scanCode: Int,
        device: KeyEventTriggerDevice,
        requiresIme: Boolean,
    ) = updateTrigger { trigger ->
        delegate.addKeyEventTriggerKey(
            trigger,
            keyCode,
            scanCode,
            device,
            requiresIme,
            otherTriggerKeys = otherTriggerKeys,
        )
    }

    override suspend fun addEvdevTriggerKey(keyCode: Int, scanCode: Int, device: EvdevDeviceInfo) =
        updateTrigger { trigger ->
            delegate.addEvdevTriggerKey(
                trigger,
                keyCode,
                scanCode,
                device,
                otherTriggerKeys = otherTriggerKeys,
            )
        }

    override fun removeTriggerKey(uid: String) = updateTrigger { trigger ->
        delegate.removeTriggerKey(trigger, uid)
    }

    override fun moveTriggerKey(fromIndex: Int, toIndex: Int) = updateTrigger { trigger ->
        delegate.moveTriggerKey(trigger, fromIndex, toIndex)
    }

    override fun getTriggerKey(uid: String): TriggerKey? {
        return state.keyMap.value.dataOrNull()?.trigger?.keys?.find { it.uid == uid }
    }

    override fun setParallelTriggerMode() = updateTrigger { trigger ->
        delegate.setParallelTriggerMode(trigger)
    }

    override fun setSequenceTriggerMode() = updateTrigger { trigger ->
        delegate.setSequenceTriggerMode(trigger)
    }

    override fun setUndefinedTriggerMode() = updateTrigger { trigger ->
        delegate.setUndefinedTriggerMode(trigger)
    }

    override fun setTriggerShortPress() {
        updateTrigger { trigger ->
            delegate.setTriggerShortPress(trigger)
        }
    }

    override fun setTriggerLongPress() {
        updateTrigger { trigger ->
            delegate.setTriggerLongPress(trigger)
        }
    }

    override fun setTriggerDoublePress() {
        updateTrigger { trigger ->
            delegate.setTriggerDoublePress(trigger)
        }
    }

    override fun setTriggerKeyClickType(keyUid: String, clickType: ClickType) {
        updateTrigger { trigger ->
            delegate.setTriggerKeyClickType(trigger, keyUid, clickType)
        }
    }

    override fun setTriggerKeyDevice(keyUid: String, device: KeyEventTriggerDevice) {
        updateTrigger { trigger ->
            delegate.setTriggerKeyDevice(trigger, keyUid, device)
        }
    }

    override fun setTriggerKeyConsumeKeyEvent(keyUid: String, consumeKeyEvent: Boolean) {
        updateTrigger { trigger ->
            delegate.setTriggerKeyConsumeKeyEvent(trigger, keyUid, consumeKeyEvent)
        }
    }

    override fun setAssistantTriggerKeyType(keyUid: String, type: AssistantTriggerType) {
        updateTrigger { trigger ->
            delegate.setAssistantTriggerKeyType(trigger, keyUid, type)
        }
    }

    override fun setFingerprintGestureType(keyUid: String, type: FingerprintGestureType) {
        updateTrigger { trigger ->
            delegate.setFingerprintGestureType(trigger, keyUid, type)
        }
    }

    override fun setVibrateEnabled(enabled: Boolean) = updateTrigger { trigger ->
        delegate.setVibrateEnabled(trigger, enabled)
    }

    override fun setVibrationDuration(duration: Int) = updateTrigger { trigger ->
        delegate.setVibrationDuration(trigger, duration, defaultVibrateDuration.value)
    }

    override fun setLongPressDelay(delay: Int) = updateTrigger { trigger ->
        delegate.setLongPressDelay(trigger, delay, defaultLongPressDelay.value)
    }

    override fun setDoublePressDelay(delay: Int) {
        updateTrigger { trigger ->
            delegate.setDoublePressDelay(trigger, delay, defaultDoublePressDelay.value)
        }
    }

    override fun setSequenceTriggerTimeout(delay: Int) {
        updateTrigger { trigger ->
            delegate.setSequenceTriggerTimeout(trigger, delay, defaultSequenceTriggerTimeout.value)
        }
    }

    override fun setLongPressDoubleVibrationEnabled(enabled: Boolean) {
        updateTrigger { trigger ->
            delegate.setLongPressDoubleVibrationEnabled(trigger, enabled)
        }
    }

    override fun setTriggerFromOtherAppsEnabled(enabled: Boolean) {
        updateTrigger { trigger ->
            delegate.setTriggerFromOtherAppsEnabled(trigger, enabled)
        }
    }

    override fun setShowToastEnabled(enabled: Boolean) {
        updateTrigger { trigger ->
            delegate.setShowToastEnabled(trigger, enabled)
        }
    }

    override fun setScanCodeDetectionEnabled(keyUid: String, enabled: Boolean) {
        updateTrigger { trigger ->
            delegate.setScanCodeDetectionEnabled(trigger, keyUid, enabled)
        }
    }

    override fun getAvailableTriggerKeyDevices(): List<KeyEventTriggerDevice> {
        val externalKeyEventTriggerDevices = sequence {
            val inputDevices =
                devicesAdapter.connectedInputDevices.value.dataOrNull() ?: emptyList()

            val showDeviceDescriptors = showDeviceDescriptors.firstBlocking()

            for (device in inputDevices) {
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
}

interface ConfigTriggerUseCase : GetDefaultKeyMapOptionsUseCase {

    val keyMap: StateFlow<State<KeyMap>>

    fun setEnabled(enabled: Boolean)

    // trigger
    suspend fun addKeyEventTriggerKey(
        keyCode: Int,
        scanCode: Int,
        device: KeyEventTriggerDevice,
        requiresIme: Boolean,
    )

    suspend fun addFloatingButtonTriggerKey(buttonUid: String)
    fun addAssistantTriggerKey(type: AssistantTriggerType)
    fun addFingerprintGesture(type: FingerprintGestureType)
    suspend fun addEvdevTriggerKey(keyCode: Int, scanCode: Int, device: EvdevDeviceInfo)

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
    fun setTriggerFromOtherAppsEnabled(enabled: Boolean)
    fun setShowToastEnabled(enabled: Boolean)
    fun setScanCodeDetectionEnabled(keyUid: String, enabled: Boolean)

    fun getAvailableTriggerKeyDevices(): List<KeyEventTriggerDevice>

    val floatingButtonToUse: MutableStateFlow<String?>
    suspend fun getFloatingLayoutCount(): Int
}
