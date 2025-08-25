package io.github.sds100.keymapper.base.promode

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles auto starting the system bridge when Key Mapper is launched.
 */
@RequiresApi(Build.VERSION_CODES.Q)
@Singleton
class SystemBridgeAutoStarter @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val suAdapter: SuAdapter,
    private val shizukuAdapter: ShizukuAdapter,
    private val connectionManager: SystemBridgeConnectionManager,
    private val preferences: PreferenceRepository
) {
    private val isAutoStartEnabled: StateFlow<Boolean> =
        preferences.get(Keys.isProModeAutoStartEnabled)
            .map { it ?: PreferenceDefaults.PRO_MODE_AUTOSTART }
            .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.PRO_MODE_AUTOSTART)

    /**
     * This must only be called once in the application lifecycle
     */
    fun autoStart() {
        coroutineScope.launch {
            combine(
                isAutoStartEnabled,
                suAdapter.isRootGranted,
                shizukuAdapter.isStarted,
            ) { isAutoStartEnabled, isRooted, isShizukuStarted ->

                // Do not listen to changes in the connection state to prevent
                // auto starting straight after it has stopped
                val isSystemBridgeConnected = connectionManager.isConnected.first()

                if (!isAutoStartEnabled || isSystemBridgeConnected) {
                    return@combine
                }

                if (isRooted) {
                    Timber.i("Auto starting system bridge with root")
                    connectionManager.startWithRoot()
                } else if (isShizukuStarted) {
                    Timber.i("Auto starting system bridge with Shizuku")
                    connectionManager.startWithShizuku()
                }

            }.collect()
        }
    }
}