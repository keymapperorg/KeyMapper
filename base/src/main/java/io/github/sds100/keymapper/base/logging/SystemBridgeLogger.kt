package io.github.sds100.keymapper.base.logging

import android.util.Log
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.ILogCallback
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Listens for SystemBridge connection and registers a log callback to receive
 * log messages from the Rust SystemBridge code. Respects the "extra logging"
 * preference to control the log level.
 */
@Singleton
class SystemBridgeLogger @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val systemBridgeConnManager: SystemBridgeConnectionManager,
    private val preferenceRepository: PreferenceRepository,
) : ILogCallback.Stub() {

    private val extraLoggingEnabled: StateFlow<Boolean> =
        preferenceRepository.get(Keys.log)
            .map { it ?: false }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    fun start() {
        // Listen for connection state changes
        coroutineScope.launch {
            systemBridgeConnManager.connectionState
                .filterIsInstance<SystemBridgeConnectionState.Connected>()
                .collect {
                    registerWithSystemBridge()
                }
        }

        // Listen for preference changes and update log level
        coroutineScope.launch {
            extraLoggingEnabled.collect { enabled ->
                updateLogLevel(enabled)
            }
        }
    }

    private fun registerWithSystemBridge() {
        systemBridgeConnManager.run { bridge ->
            bridge.registerLogCallback(this@SystemBridgeLogger)
            bridge.setLogLevel(getSystemBridgeLogLevel(extraLoggingEnabled.value))
        }
    }

    private fun updateLogLevel(extraLogging: Boolean) {
        systemBridgeConnManager.run { bridge ->
            bridge.setLogLevel(getSystemBridgeLogLevel(extraLogging))
        }
    }

    override fun onLog(level: Int, message: String) {
        // Log with Timber so the messages appear under the Key Mapper package name
        // in logcat, and the KeyMapperLoggingTree will then save it to the LogRepository.
        Timber.log(priority = level, message = "systembridge: $message")
    }

    private fun getSystemBridgeLogLevel(extraLogging: Boolean): Int {
        val level = if (extraLogging) {
            Log.DEBUG
        } else {
            Log.INFO
        }
        return level
    }
}
