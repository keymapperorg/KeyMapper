package io.github.sds100.keymapper.util.delegate

import io.github.sds100.keymapper.util.KeyEventUtils
import io.github.sds100.keymapper.util.RootUtils
import io.github.sds100.keymapper.util.Shell
import kotlinx.coroutines.*
import splitties.systemservices.inputManager
import java.util.*

/**
 * Created by sds100 on 21/06/2020.
 */
class GetEventDelegate(val onKeyEvent: (keyCode: Int,
                                        action: Int,
                                        deviceDescriptor: String,
                                        isExternal: Boolean) -> Unit) {

    companion object {
        private const val REGEX_GET_DEVICE_LOCATION = "\\/.*(?=:)"
        private const val REGEX_KEY_EVENT_ACTION = "(DOWN\\n|UP\\n)"
    }

    private var mJob: Job? = null

    fun startListening(scope: CoroutineScope, keyCodes: List<Int>) {
        mJob = scope.launch {
            withContext(Dispatchers.IO) {

                val getEventDevices: String

                RootUtils.getRootCommandOutput("getevent -i").apply {
                    getEventDevices = bufferedReader().readText()
                    close()
                }

                val inputDeviceLocationMap = inputManager.inputDeviceIds.map { id ->
                    val device = inputManager.getInputDevice(id)
                    getDeviceLocation(getEventDevices, device.name) to device.descriptor
                }.toMap()

                val getEventLabels = keyCodes.map {
                    KeyEventUtils.KEY_EVENT_LABEL_TO_GET_EVENT_LABEL[it]
                        ?: throw Exception("Android keycode: $it isn't mapped to a getevent label")
                }

                val deviceLocationRegex = Regex(REGEX_GET_DEVICE_LOCATION)
                val actionRegex = Regex(REGEX_KEY_EVENT_ACTION)

                val inputStream = Shell.getShellCommandStdOut("su", "-c", "getevent -l")
                var line: String?

                while (inputStream.bufferedReader().readLine().also { line = it } != null && isActive) {
                    line ?: continue

                    getEventLabels.forEachIndexed { index, label ->
                        if (line?.contains(label) == true) {
                            val keycode = keyCodes[index]
                            val deviceLocation = deviceLocationRegex.find(line!!)?.value ?: return@forEachIndexed
                            val deviceDescriptor = inputDeviceLocationMap[deviceLocation]
                            val actionString = actionRegex.find(line!!)?.value?.toLowerCase(Locale.ROOT)
                                ?: return@forEachIndexed

                            if ()

                                return@forEachIndexed
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