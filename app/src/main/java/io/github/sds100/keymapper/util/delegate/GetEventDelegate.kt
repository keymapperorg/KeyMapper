package io.github.sds100.keymapper.util.delegate

import android.view.KeyEvent
import io.github.sds100.keymapper.util.KeyEventUtils
import io.github.sds100.keymapper.util.RootUtils
import io.github.sds100.keymapper.util.Shell
import io.github.sds100.keymapper.util.isExternalCompat
import kotlinx.coroutines.*
import splitties.systemservices.inputManager

/**
 * Created by sds100 on 21/06/2020.
 */
class GetEventDelegate(val onKeyEvent: suspend (keyCode: Int,
                                                action: Int,
                                                deviceDescriptor: String,
                                                isExternal: Boolean) -> Unit) {

    companion object {
        private const val REGEX_GET_DEVICE_LOCATION = "\\/.*(?=:)"
        private const val REGEX_KEY_EVENT_ACTION = "(?<= )(DOWN|UP)"
    }

    private var mJob: Job? = null

    fun startListening(scope: CoroutineScope) {
        mJob = scope.launch {
            withContext(Dispatchers.IO) {

                val getEventDevices: String

                RootUtils.getRootCommandOutput("getevent -i").apply {
                    getEventDevices = bufferedReader().readText()
                    close()
                }

                val deviceLocationToDescriptorMap = mutableMapOf<String, String>()
                val descriptorToIsExternalMap = mutableMapOf<String, Boolean>()

                inputManager.inputDeviceIds.forEach { id ->
                    val device = inputManager.getInputDevice(id)
                    val deviceLocation = getDeviceLocation(getEventDevices, device.name) ?: return@forEach
                    deviceLocationToDescriptorMap[deviceLocation] = device.descriptor
                    descriptorToIsExternalMap[device.descriptor] = device.isExternalCompat
                }

                val getEventLabels = KeyEventUtils.GET_EVENT_LABEL_TO_KEYCODE.keys

                val deviceLocationRegex = Regex(REGEX_GET_DEVICE_LOCATION)
                val actionRegex = Regex(REGEX_KEY_EVENT_ACTION)

                //use -q option to not initially output the list of devices
                val inputStream = Shell.getShellCommandStdOut("su", "-c", "getevent -lq")
                var line: String?

                while (inputStream.bufferedReader().readLine().also { line = it } != null && isActive) {
                    line ?: continue

                    getEventLabels.forEach { label ->
                        if (line?.contains(label) == true) {
                            val keycode = KeyEventUtils.GET_EVENT_LABEL_TO_KEYCODE[label]!!
                            val deviceLocation = deviceLocationRegex.find(line!!)?.value ?: return@forEach
                            val deviceDescriptor = deviceLocationToDescriptorMap[deviceLocation]!!
                            val isExternal = descriptorToIsExternalMap[deviceDescriptor]!!
                            val actionString = actionRegex.find(line!!)?.value ?: return@forEach

                            when (actionString) {
                                "UP" -> {
                                    onKeyEvent.invoke(keycode, KeyEvent.ACTION_UP, deviceDescriptor, isExternal)
                                }

                                "DOWN" -> {
                                    onKeyEvent.invoke(keycode, KeyEvent.ACTION_DOWN, deviceDescriptor, isExternal)
                                }
                            }

                            return@forEach
                        }
                    }
                }

                inputStream.close()
            }
        }
    }

    fun stopListening() {
        mJob?.cancel()
    }

    private fun getDeviceLocation(getEventDeviceOutput: String, deviceName: String): String? {
        val regex = Regex("(/.*)(?=(\\n.*){5}\"$deviceName\")")
        return regex.find(getEventDeviceOutput)?.value
    }
}