package io.github.sds100.keymapper.base.actions

import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.base.input.InjectKeyEventModel
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.common.utils.InputEventAction
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.ifIsData
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PerformKeyEventActionDelegate(
    private val coroutineScope: CoroutineScope,
    private val settingsRepository: PreferenceRepository,
    private val inputEventHub: InputEventHub,
    private val devicesAdapter: DevicesAdapter,
) {
    private val injectKeyEventsWithSystemBridge: StateFlow<Boolean> =
        settingsRepository.get(Keys.keyEventActionsUseSystemBridge)
            .map { it ?: PreferenceDefaults.KEY_EVENT_ACTIONS_USE_SYSTEM_BRIDGE }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    suspend fun perform(
        action: ActionData.InputKeyEvent,
        inputEventAction: InputEventAction,
        keyMetaState: Int,
        triggerDevice: PerformActionTriggerDevice,
    ): KMResult<Unit> {
        if (injectKeyEventsWithSystemBridge.value &&
            triggerDevice is PerformActionTriggerDevice.Evdev &&
            (action.device == null || isActionDeviceGrabbed(action.device))
        ) {
            return injectEvdevEvent(inputEventAction, triggerDevice.deviceId, action)
        }

        val deviceId: Int = getDeviceIdForKeyEventAction(action)

        // If the device that the user specified in the action can not be found
        // then fallback to evdev injection.
        if (injectKeyEventsWithSystemBridge.value &&
            deviceId == -1 &&
            triggerDevice is PerformActionTriggerDevice.Evdev
        ) {
            return injectEvdevEvent(inputEventAction, triggerDevice.deviceId, action)
        }

        // See issue #1683. Some apps ignore key events which do not have a source.
        val source = when {
            KeyEventUtils.isDpadKeyCode(action.keyCode) -> InputDevice.SOURCE_DPAD
            KeyEventUtils.isGamepadButton(action.keyCode) -> InputDevice.SOURCE_GAMEPAD
            else -> InputDevice.SOURCE_KEYBOARD
        }

        val firstInputAction = if (inputEventAction == InputEventAction.UP) {
            KeyEvent.ACTION_UP
        } else {
            KeyEvent.ACTION_DOWN
        }

        val model = InjectKeyEventModel(
            keyCode = action.keyCode,
            action = firstInputAction,
            metaState = keyMetaState.withFlag(action.metaState),
            deviceId = deviceId,
            source = source,
            repeatCount = 0,
            scanCode = 0,
        )

        return injectAndroidKeyEvent(inputEventAction, model)
    }

    private fun isActionDeviceGrabbed(device: ActionData.InputKeyEvent.Device): Boolean {
        return inputEventHub.getGrabbedDevices().any { it.name == device.name }
    }

    private fun injectEvdevEvent(
        inputEventAction: InputEventAction,
        deviceId: Int,
        action: ActionData.InputKeyEvent,
    ): KMResult<Unit> {
        when (inputEventAction) {
            InputEventAction.DOWN_UP -> {
                return injectDownEvdevEvent(
                    deviceId,
                    action,
                ).then { injectUpEvdevEvent(deviceId, action) }
            }

            InputEventAction.DOWN -> return injectDownEvdevEvent(
                deviceId,
                action,
            )

            InputEventAction.UP -> return injectUpEvdevEvent(
                deviceId,
                action,
            )
        }
    }

    private fun injectDownEvdevEvent(
        deviceId: Int,
        action: ActionData.InputKeyEvent,
    ): KMResult<Unit> {
        return inputEventHub.injectEvdevEventKeyCode(
            deviceId = deviceId,
            keyCode = action.keyCode,
            value = 1,
        )
    }

    private fun injectUpEvdevEvent(
        deviceId: Int,
        action: ActionData.InputKeyEvent,
    ): KMResult<Unit> {
        return inputEventHub.injectEvdevEventKeyCode(
            deviceId = deviceId,
            action.keyCode,
            value = 0,
        )
    }

    private suspend fun injectAndroidKeyEvent(
        inputEventAction: InputEventAction,
        model: InjectKeyEventModel,
    ): KMResult<Unit> {
        if (inputEventAction == InputEventAction.DOWN_UP) {
            return inputEventHub.injectKeyEvent(
                model,
                useSystemBridgeIfAvailable = injectKeyEventsWithSystemBridge.value,
            ).then {
                inputEventHub.injectKeyEvent(
                    model.copy(action = KeyEvent.ACTION_UP),
                    useSystemBridgeIfAvailable = injectKeyEventsWithSystemBridge.value,
                )
            }
        } else {
            return inputEventHub.injectKeyEvent(
                model,
                useSystemBridgeIfAvailable = injectKeyEventsWithSystemBridge.value,
            )
        }
    }

    private fun getDeviceIdForKeyEventAction(action: ActionData.InputKeyEvent): Int {
        if (action.device?.descriptor == null) {
            // automatically select a game controller as the input device for game controller key events

            if (KeyEventUtils.isGamepadKeyCode(action.keyCode)) {
                devicesAdapter.connectedInputDevices.value.ifIsData { inputDevices ->
                    val device = inputDevices.find { it.isGameController }

                    if (device != null) {
                        return device.id
                    }
                }
            }

            return 0
        }

        val inputDevices = devicesAdapter.connectedInputDevices.value

        val devicesWithSameDescriptor =
            inputDevices.dataOrNull()
                ?.filter { it.descriptor == action.device.descriptor }
                ?: emptyList()

        if (devicesWithSameDescriptor.isEmpty()) {
            return -1
        }

        if (devicesWithSameDescriptor.size == 1) {
            return devicesWithSameDescriptor[0].id
        }

        /*
        if there are multiple devices use the device that supports the key
        code. if none do then use the first one
         */
        val deviceThatHasKey = devicesWithSameDescriptor.singleOrNull {
            devicesAdapter.deviceHasKey(it.id, action.keyCode)
        }

        val device = deviceThatHasKey
            ?: devicesWithSameDescriptor.singleOrNull { it.name == action.device.name }
            ?: devicesWithSameDescriptor[0]

        return device.id
    }
}
