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
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.isSuccess
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.sysbridge.adb.AdbManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

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

    override val isDeveloperOptionsEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(getDeveloperOptionsEnabled())

    override val isWirelessDebuggingEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(getWirelessDebuggingEnabled())

    // Use a SharedFlow so that the same value can be emitted repeatedly.
    override val setupAssistantStep: MutableSharedFlow<SystemBridgeSetupStep?> = MutableSharedFlow()
    private val setupAssistantStepState =
        setupAssistantStep.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    init {
        // Automatically go back to the Key Mapper app when turning on wireless debugging
        coroutineScope.launch {
            val uri = Settings.Global.getUriFor(ADB_WIRELESS_SETTING)
            SettingsUtils.settingsCallbackFlow(ctx, uri).collect {
                isWirelessDebuggingEnabled.update { getWirelessDebuggingEnabled() }

                // Only go back if the user is currently setting up the wireless debugging step.
                // This stops Key Mapper going back if they are turning on wireless debugging
                // for another reason.
                if (isWirelessDebuggingEnabled.value && setupAssistantStepState.value == SystemBridgeSetupStep.WIRELESS_DEBUGGING) {
                    // TODO only go back if the ADB server is actually running. The first time wireless debugging is turned on in a new network, it shows a dialog
                    getKeyMapperAppTask()?.moveToFront()
                }
            }
        }

        coroutineScope.launch {
            val uri = Settings.Global.getUriFor(DEVELOPER_OPTIONS_SETTING)
            SettingsUtils.settingsCallbackFlow(ctx, uri).collect {
                isDeveloperOptionsEnabled.update { getDeveloperOptionsEnabled() }

                if (isDeveloperOptionsEnabled.value && setupAssistantStepState.value == SystemBridgeSetupStep.DEVELOPER_OPTIONS) {
                    getKeyMapperAppTask()?.moveToFront()
                }
            }
        }
    }

    override fun startWithRoot() {
        coroutineScope.launch {
            connectionManager.startWithRoot()
        }
    }

    override fun startWithShizuku() {
        connectionManager.startWithShizuku()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startWithAdb() {
        connectionManager.startWithAdb()
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
            invalidateSettings()
        } else {
            SettingsUtils.launchSettingsScreen(
                ctx,
                Settings.ACTION_DEVICE_INFO_SETTINGS,
                "build_number"
            )

            coroutineScope.launch {
                setupAssistantStep.emit(SystemBridgeSetupStep.DEVELOPER_OPTIONS)
            }
        }
    }

    override fun enableWirelessDebugging() {
        if (canWriteGlobalSettings()) {
            SettingsUtils.putGlobalSetting(ctx, ADB_WIRELESS_SETTING, 1)
            invalidateSettings()
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
        if (!getWirelessDebuggingEnabled()) {
            return false
        }

        // Try running a command to see if the pairing is working correctly.
        return adbManager.executeCommand("sh").isSuccess
    }

    /**
     * @return whether it opened the wireless debugging activity successfully. If it is
     * false then developer options was launched.
     */
    private fun launchWirelessDebuggingActivity(): Boolean {
        val quickSettingsIntent = Intent(TileService.ACTION_QS_TILE_PREFERENCES).apply {
            // Set the package name because this action can also resolve to a "Permission Controller" activity.
            val packageName = "com.android.settings"
            setPackage(packageName)

            putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                ComponentName(
                    packageName,
                    "com.android.settings.development.qstile.DevelopmentTiles\$WirelessDebugging"
                )
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }

        try {
            ctx.startActivity(quickSettingsIntent)
            return true
        } catch (_: ActivityNotFoundException) {
            SettingsUtils.launchSettingsScreen(
                ctx,
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                "toggle_adb_wireless"
            )

            return false
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
            .firstOrNull { it.taskInfo.topActivity?.className == keyMapperClassProvider.getMainActivity().name }
        return task
    }

    private fun canWriteGlobalSettings(): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.Q)
interface SystemBridgeSetupController {
    val setupAssistantStep: Flow<SystemBridgeSetupStep?>

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
}