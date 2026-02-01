package io.github.sds100.keymapper.sysbridge.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.KeyMapperClassProvider
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.isSuccess
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.sysbridge.adb.AdbManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState.Connected
import io.github.sds100.keymapper.sysbridge.manager.awaitConnected
import io.github.sds100.keymapper.sysbridge.manager.isConnected
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

@Singleton
class SystemBridgeSetupControllerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val adbManager: AdbManager,
    private val keyMapperClassProvider: KeyMapperClassProvider,
    private val connectionManager: SystemBridgeConnectionManager,
) : SystemBridgeSetupController {

    companion object {
        private const val DEVELOPER_OPTIONS_SETTING = "development_settings_enabled"
        private const val ADB_WIRELESS_SETTING = "adb_wifi_enabled"
    }

    private val activityManager: ActivityManager by lazy { ctx.getSystemService()!! }

    private val _isStarting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isStarting: StateFlow<Boolean> = _isStarting
    private var startJob: Job? = null

    override val isDeveloperOptionsEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(getDeveloperOptionsEnabled())

    override val isWirelessDebuggingEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(getWirelessDebuggingEnabled())

    // Use a SharedFlow so that the same value can be emitted repeatedly.
    override val setupAssistantStep: MutableSharedFlow<SystemBridgeSetupStep?> = MutableSharedFlow()
    private val setupAssistantStepState =
        setupAssistantStep.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val isAdbPairedResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private var isAdbPairedJob: Job? = null

    override val isAdbInputSecurityEnabled: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private var checkAdbInputSecurityJob: Job? = null

    init {
        // Automatically go back to the Key Mapper app when turning on wireless debugging
        coroutineScope.launch {
            val uri = Settings.Global.getUriFor(ADB_WIRELESS_SETTING)
            SettingsUtils.settingsCallbackFlow(ctx, uri).collect {
                isWirelessDebuggingEnabled.update { getWirelessDebuggingEnabled() }

                // Do not automatically go back to Key Mapper after this step because
                // some devices show a dialog that will be auto dismissed resulting in wireless
                // ADB being immediately disabled. E.g OnePlus 6T Oxygen OS 11
                // Note: ADB input security check is handled by monitoring isWirelessDebuggingEnabled flow
            }
        }

        coroutineScope.launch {
            val uri = Settings.Global.getUriFor(DEVELOPER_OPTIONS_SETTING)
            SettingsUtils.settingsCallbackFlow(ctx, uri).collect {
                isDeveloperOptionsEnabled.update { getDeveloperOptionsEnabled() }

                if (isDeveloperOptionsEnabled.value &&
                    setupAssistantStepState.value == SystemBridgeSetupStep.DEVELOPER_OPTIONS
                ) {
                    getKeyMapperAppTask()?.moveToFront()
                }
            }
        }

        // Automatically check ADB input security when SystemBridge is connected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            coroutineScope.launch {
                // Check when SystemBridge becomes connected
                connectionManager.connectionState.collect { connectionState ->
                    when (connectionState) {
                        is Connected -> {
                            // Delay a bit to ensure SystemBridge is ready
                            kotlinx.coroutines.delay(1000L)
                            checkAdbInputSecurityEnabled()
                        }

                        is SystemBridgeConnectionState.Disconnected -> {
                            // Reset to null when SystemBridge is disconnected
                            isAdbInputSecurityEnabled.value = null
                        }
                    }
                }
            }
        }
    }

    override fun startWithRoot() {
        if (startJob?.isActive == true) {
            Timber.i("System Bridge is already starting")
            return
        }

        startJob = coroutineScope.launch {
            _isStarting.value = true
            try {
                connectionManager.startWithRoot()
                // Wait for the service to bind and start system bridge
                withTimeoutOrNull(10000L) {
                    connectionManager.awaitConnected()
                }
            } finally {
                _isStarting.value = false
            }
        }
    }

    override fun startWithShizuku() {
        if (startJob?.isActive == true) {
            Timber.i("System Bridge is already starting")
            return
        }

        startJob = coroutineScope.launch {
            _isStarting.value = true
            try {
                connectionManager.startWithShizuku()

                // Wait for the service to bind and start system bridge
                withTimeoutOrNull(10000L) {
                    connectionManager.awaitConnected()
                }
            } finally {
                _isStarting.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startWithAdb() {
        if (startJob?.isActive == true) {
            Timber.i("System Bridge is already starting")
            return
        }

        startJob = coroutineScope.launch {
            _isStarting.value = true
            try {
                connectionManager.startWithAdb()
                // Wait for the service to bind and start system bridge
                withTimeoutOrNull(10000L) {
                    connectionManager.awaitConnected()
                }
            } finally {
                _isStarting.value = false
            }
        }
    }

    /**
     * If Key Mapper has WRITE_SECURE_SETTINGS permission then it can turn on wireless debugging
     * and ADB and then start the system bridge.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun autoStartWithAdb() {
        if (startJob?.isActive == true) {
            Timber.i("System Bridge is already starting")
            return
        }

        startJob = coroutineScope.launch {
            _isStarting.value = true
            try {
                if (!canWriteGlobalSettings()) {
                    Timber.w(
                        "Cannot auto start with ADB. WRITE_SECURE_SETTINGS permission not granted",
                    )
                    return@launch
                }

                if (connectionManager.connectionState.value
                        !is SystemBridgeConnectionState.Disconnected
                ) {
                    Timber.w("Not auto starting. System Bridge is already connected.")
                    return@launch
                }

                SettingsUtils.putGlobalSetting(ctx, DEVELOPER_OPTIONS_SETTING, 1)

                try {
                    withTimeout(5000L) { isDeveloperOptionsEnabled.first { it } }
                } catch (_: TimeoutCancellationException) {
                    return@launch
                }

                if (isAdbPaired()) {
                    // This is IMPORTANT. First turn on ADB before enabling wireless debugging because
                    // turning on developer options just before can cause the Shell to be killed once
                    // the system bridge is started.
                    SettingsUtils.putGlobalSetting(ctx, Settings.Global.ADB_ENABLED, 1)
                    SettingsUtils.putGlobalSetting(ctx, ADB_WIRELESS_SETTING, 1)

                    // Wait for wireless debugging to be enabled before starting with ADB
                    try {
                        withTimeout(5000L) { isWirelessDebuggingEnabled.first { it } }
                    } catch (_: TimeoutCancellationException) {
                        return@launch
                    }

                    connectionManager.startWithAdb()

                    // Wait for the service to connect before turning off wireless debugging
                    withTimeoutOrNull(10000L) {
                        connectionManager.awaitConnected()
                    }

                    // Disable wireless debugging when done
                    SettingsUtils.putGlobalSetting(ctx, ADB_WIRELESS_SETTING, 0)
                } else {
                    Timber.e("Autostart failed. ADB not paired successfully.")
                }
            } finally {
                _isStarting.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun launchPairingAssistant() {
        launchWirelessDebuggingActivity()

        coroutineScope.launch {
            setupAssistantStep.emit(SystemBridgeSetupStep.ADB_PAIRING)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun pairWirelessAdb(code: String): KMResult<Unit> {
        return adbManager.pair(code).onSuccess {
            // Clear the step if still at the pairing step.
            if (setupAssistantStepState.value == SystemBridgeSetupStep.ADB_PAIRING) {
                setupAssistantStep.emit(null)
            }
        }
    }

    override fun enableDeveloperOptions() {
        if (canWriteGlobalSettings()) {
            SettingsUtils.putGlobalSetting(ctx, DEVELOPER_OPTIONS_SETTING, 1)
        } else {
            SettingsUtils.launchSettingsScreen(
                ctx,
                Settings.ACTION_DEVICE_INFO_SETTINGS,
                fragmentArg = "build_number",
            )

            coroutineScope.launch {
                setupAssistantStep.emit(SystemBridgeSetupStep.DEVELOPER_OPTIONS)
            }
        }
    }

    override fun enableWirelessDebugging() {
        if (canWriteGlobalSettings()) {
            SettingsUtils.putGlobalSetting(ctx, ADB_WIRELESS_SETTING, 1)
        } else {
            // This is the intent sent by the quick settings tile. Not all devices support this.
            launchWirelessDebuggingActivity()

            coroutineScope.launch {
                setupAssistantStep.emit(SystemBridgeSetupStep.WIRELESS_DEBUGGING)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun isAdbPaired(): Boolean {
        // Sometimes multiple calls to this function can happen in a short space of time
        // so only run one job to check whether it is paired.
        if (isAdbPairedJob == null || isAdbPairedJob?.isCompleted == true) {
            isAdbPairedJob?.cancel()
            isAdbPairedResult.value = null

            isAdbPairedJob = coroutineScope.launch {
                SettingsUtils.putGlobalSetting(ctx, ADB_WIRELESS_SETTING, 1)

                // Try running a command to see if the pairing is working correctly.
                // This will execute the "exit" command in the shell so it immediately closes
                // the connection.
                isAdbPairedResult.value = adbManager.executeCommand("exit").isSuccess
            }
        }

        // Wait for the next result
        return isAdbPairedResult.filterNotNull().first()
    }

    /**
     * @return whether it opened the wireless debugging activity successfully. If it is
     * false then developer options was launched.
     */
    private fun launchWirelessDebuggingActivity(): Boolean {
        // See issue #1898. Xiaomi like to do dodgy stuff, which causes a crash
        // when long pressing the quick settings tile for wireless debugging.
        if (Build.BRAND in setOf("xiaomi", "redmi", "poco")) {
            highlightDeveloperOptionsWirelessDebuggingOption()
            return false
        }

        val quickSettingsIntent = Intent(TileService.ACTION_QS_TILE_PREFERENCES).apply {
            // Set the package name because this action can also resolve to a "Permission Controller" activity.
            val packageName = "com.android.settings"
            setPackage(packageName)

            putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                ComponentName(
                    packageName,
                    "com.android.settings.development.qstile.DevelopmentTiles\$WirelessDebugging",
                ),
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
            )
        }

        try {
            ctx.startActivity(quickSettingsIntent)
            return true
        } catch (_: SecurityException) {
            highlightDeveloperOptionsWirelessDebuggingOption()

            return false
        } catch (_: ActivityNotFoundException) {
            highlightDeveloperOptionsWirelessDebuggingOption()

            return false
        }
    }

    private fun highlightDeveloperOptionsWirelessDebuggingOption() {
        SettingsUtils.launchSettingsScreen(
            ctx,
            Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            fragmentArg = "toggle_adb_wireless",
        )
    }

    override fun launchDeveloperOptions() {
        SettingsUtils.launchSettingsScreen(
            ctx,
            Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            null,
        )
    }

    private fun checkAdbInputSecurityEnabled() {
        if (!connectionManager.isConnected()) {
            isAdbInputSecurityEnabled.value = null
            return
        }

        // Only run one check at a time
        if (checkAdbInputSecurityJob == null || checkAdbInputSecurityJob?.isCompleted == true) {
            checkAdbInputSecurityJob?.cancel()

            checkAdbInputSecurityJob = coroutineScope.launch {
                try {
                    val result = connectionManager.run { systemBridge ->
                        systemBridge.executeCommand("getprop persist.security.adbinput", 5000L)
                    }

                    val isEnabled = when (result) {
                        is Success -> {
                            val stdout = result.value.stdout.trim()

                            when (stdout) {
                                "1" -> true

                                "0" -> false

                                // If it is empty or anything else then set the value to null
                                // because what we are expecting does not exist.
                                else -> null
                            }
                        }

                        else -> null
                    }
                    isAdbInputSecurityEnabled.value = isEnabled
                } catch (_: Exception) {
                    // If check fails, set to null
                    isAdbInputSecurityEnabled.value = null
                }
            }
        }
    }

    fun invalidateSettings() {
        isDeveloperOptionsEnabled.update { getDeveloperOptionsEnabled() }
        isWirelessDebuggingEnabled.update { getWirelessDebuggingEnabled() }
    }

    private fun getDeveloperOptionsEnabled(): Boolean {
        try {
            return SettingsUtils.getGlobalSetting<Int>(ctx, DEVELOPER_OPTIONS_SETTING) == 1
        } catch (_: Settings.SettingNotFoundException) {
            return false
        }
    }

    private fun getWirelessDebuggingEnabled(): Boolean {
        try {
            return SettingsUtils.getGlobalSetting<Int>(ctx, ADB_WIRELESS_SETTING) == 1
        } catch (_: Settings.SettingNotFoundException) {
            return false
        }
    }

    private fun getKeyMapperAppTask(): ActivityManager.AppTask? {
        val task = activityManager.appTasks
            ?.firstOrNull {
                it.taskInfo.topActivity?.className ==
                    keyMapperClassProvider.getMainActivity().name
            }
        return task
    }

    private fun canWriteGlobalSettings(): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.WRITE_SECURE_SETTINGS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getShellStartCommand(): KMResult<String> {
        return connectionManager.getShellStartCommand()
    }
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
interface SystemBridgeSetupController {
    val setupAssistantStep: Flow<SystemBridgeSetupStep?>
    val isStarting: StateFlow<Boolean>

    val isDeveloperOptionsEnabled: Flow<Boolean>
    fun enableDeveloperOptions()

    val isWirelessDebuggingEnabled: Flow<Boolean>
    fun enableWirelessDebugging()

    fun launchPairingAssistant()

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun isAdbPaired(): Boolean

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun pairWirelessAdb(code: String): KMResult<Unit>

    fun startWithRoot()
    fun startWithShizuku()
    fun startWithAdb()
    fun autoStartWithAdb()

    /**
     * If this value is null then the option does not exist or can not be checked
     * because the system bridge is disconnected.
     */
    val isAdbInputSecurityEnabled: StateFlow<Boolean?>

    fun launchDeveloperOptions()

    suspend fun getShellStartCommand(): KMResult<String>
}
