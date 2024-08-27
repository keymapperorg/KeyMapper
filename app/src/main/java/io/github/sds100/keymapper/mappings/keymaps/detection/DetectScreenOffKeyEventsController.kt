package io.github.sds100.keymapper.mappings.keymaps.detection

import android.view.KeyEvent
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Created by sds100 on 21/06/2020.
 */
class DetectScreenOffKeyEventsController(
    private val suAdapter: SuAdapter,
    private val devicesAdapter: DevicesAdapter,
    private val onKeyEvent: suspend (
        keyCode: Int,
        action: Int,
        device: InputDeviceInfo,
    ) -> Unit,
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
                        .also { line = it } != null && isActive
                ) {
                    line ?: continue

                    KeyEventUtils.GET_EVENT_LABEL_TO_KEYCODE.forEach { (label, keyCode) ->
                        if (line?.contains(label) == true) {
                            val deviceLocation =
                                deviceLocationRegex.find(line!!)?.value ?: return@forEach

                            val device = deviceLocationToDeviceMap[deviceLocation] ?: return@forEach

                            val actionString = actionRegex.find(line!!)?.value ?: return@forEach

                            when (actionString) {
                                "UP" -> {
                                    onKeyEvent.invoke(
                                        keyCode,
                                        KeyEvent.ACTION_UP,
                                        device,
                                    )
                                }

                                "DOWN" -> {
                                    onKeyEvent.invoke(
                                        keyCode,
                                        KeyEvent.ACTION_DOWN,
                                        device,
                                    )
                                }
                            }

                            return@forEach
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
