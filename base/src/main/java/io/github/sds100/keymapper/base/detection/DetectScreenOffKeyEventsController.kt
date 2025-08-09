package io.github.sds100.keymapper.base.detection

import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import io.github.sds100.keymapper.system.root.SuAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

// TODO delete
class DetectScreenOffKeyEventsController(
    private val suAdapter: SuAdapter,
    private val devicesAdapter: DevicesAdapter,
    private val onKeyEvent: suspend (event: KMKeyEvent) -> Unit,
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
        return false
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
