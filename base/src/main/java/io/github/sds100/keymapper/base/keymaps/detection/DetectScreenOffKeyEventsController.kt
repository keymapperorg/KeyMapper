package io.github.sds100.keymapper.base.keymaps.detection

import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.common.result.valueOrNull
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputevents.MyKeyEvent
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.common.state.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class DetectScreenOffKeyEventsController(
    private val suAdapter: SuAdapter,
    private val devicesAdapter: DevicesAdapter,
    private val onKeyEvent: suspend (event: MyKeyEvent) -> Unit,
) {

    companion object {
        private const val REGEX_GET_DEVICE_LOCATION = "/.*(?=:)"
        private const val REGEX_KEY_EVENT_ACTION = "(?<= )(DOWN|UP)"
    }

    private var job: Job? = null

    /**
     * @return whether it successfully started listening.
     */
    fun startListening(scope: CoroutineScope): Boolean {
        try {
            job = scope.launch(Dispatchers.IO) {
                val devicesInputStream =
                    suAdapter.getCommandOutput("getevent -i").valueOrNull() ?: return@launch

                val getEventDevices: String = devicesInputStream.bufferedReader().readText()
                devicesInputStream.close()

                val deviceLocationToDeviceMap = mutableMapOf<String, InputDeviceInfo>()

                val inputDevices =
                    devicesAdapter.connectedInputDevices.first { it is State.Data } as State.Data

                inputDevices.data.forEach { device ->
                    val deviceLocation =
                        getDeviceLocation(getEventDevices, device.name) ?: return@forEach

                    deviceLocationToDeviceMap[deviceLocation] = device
                }

                val deviceLocationRegex = Regex(REGEX_GET_DEVICE_LOCATION)
                val actionRegex = Regex(REGEX_KEY_EVENT_ACTION)

                // use -q option to not initially output the list of devices
                val inputStream =
                    suAdapter.getCommandOutput("getevent -lq").valueOrNull() ?: return@launch

                var line: String?

                while (inputStream.bufferedReader().readLine()
                        .also { line = it } != null &&
                    isActive
                ) {
                    line ?: continue

                    InputEventUtils.GET_EVENT_LABEL_TO_KEYCODE.forEach { (label, keyCode) ->
                        if (line!!.contains(label)) {
                            val deviceLocation =
                                deviceLocationRegex.find(line!!)?.value ?: return@forEach

                            val device = deviceLocationToDeviceMap[deviceLocation] ?: return@forEach

                            val actionString = actionRegex.find(line!!)?.value ?: return@forEach

                            when (actionString) {
                                "UP" -> {
                                    onKeyEvent.invoke(
                                        MyKeyEvent(
                                            keyCode = keyCode,
                                            action = KeyEvent.ACTION_UP,
                                            device = device,
                                            scanCode = 0,
                                            metaState = 0,
                                            repeatCount = 0,
                                            source = InputDevice.SOURCE_UNKNOWN,
                                        ),
                                    )
                                }

                                "DOWN" -> {
                                    onKeyEvent.invoke(
                                        MyKeyEvent(
                                            keyCode = keyCode,
                                            action = KeyEvent.ACTION_DOWN,
                                            device = device,
                                            scanCode = 0,
                                            metaState = 0,
                                            repeatCount = 0,
                                            source = InputDevice.SOURCE_UNKNOWN,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                inputStream.close()
            }
        } catch (e: Exception) {
            Timber.e(e)
            job?.cancel()
            return false
        }

        return true
    }

    fun stopListening() {
        job?.cancel()
        job = null
    }

    private fun getDeviceLocation(getEventDeviceOutput: String, deviceName: String): String? {
        val regex = Regex("(/.*)(?=(\\n.*){5}\"$deviceName\")")
        return regex.find(getEventDeviceOutput)?.value
    }
}
