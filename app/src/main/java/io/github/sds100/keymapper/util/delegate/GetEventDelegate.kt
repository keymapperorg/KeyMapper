package io.github.sds100.keymapper.util.delegate

import io.github.sds100.keymapper.util.RootUtils
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Created by sds100 on 21/06/2020.
 */
class GetEventDelegate(val onKeyEvent: () -> Unit) {

    private var mJob: Job? = null

    fun startListening(scope: CoroutineScope, keyCodes: List<Int>) {
        mJob = scope.launch {
            withContext(Dispatchers.IO) {

                //TODO grep only for keycodes
                val inputStream = RootUtils.getRootCommandOutput("getevent -l")
                var line: String

                while (inputStream.bufferedReader().readLine().also { line = it } != null && isActive) {
                    Timber.d(line)
                }

                inputStream.close()
            }
        }
    }

    fun stopListening() {
        mJob?.cancel()
    }
}