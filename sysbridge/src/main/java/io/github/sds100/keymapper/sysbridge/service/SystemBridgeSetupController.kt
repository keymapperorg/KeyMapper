package io.github.sds100.keymapper.sysbridge.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.KeyMapperClassProvider
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.isSuccess
import io.github.sds100.keymapper.sysbridge.adb.AdbManager
import io.github.sds100.keymapper.sysbridge.starter.SystemBridgeStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton


/**
 * This starter code is taken from the Shizuku project.
 */

@Singleton
class SystemBridgeSetupControllerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val buildConfigProvider: BuildConfigProvider,
    private val adbManager: AdbManager,
    private val keyMapperClassProvider: KeyMapperClassProvider
) : SystemBridgeSetupController {

    companion object {
        private const val DEVELOPER_OPTIONS_SETTING = "development_settings_enabled"
        private const val ADB_WIRELESS_SETTING = "adb_wifi_enabled"
    }

    private val activityManager: ActivityManager by lazy { ctx.getSystemService()!! }

    private val starter: SystemBridgeStarter by lazy {
        SystemBridgeStarter(ctx, adbManager, buildConfigProvider)
    }

    override val isDeveloperOptionsEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(getDeveloperOptionsEnabled())

    override val isWirelessDebuggingEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(getWirelessDebuggingEnabled())

    override val startSetupAssistantRequest: MutableSharedFlow<SystemBridgeSetupStep> =
        MutableSharedFlow()

    init {
        coroutineScope.launch {
            val uri = Settings.Global.getUriFor(ADB_WIRELESS_SETTING)
            SettingsUtils.settingsCallbackFlow(ctx, uri).collect {
                val value = getWirelessDebuggingEnabled()

                if (value) {
                    getKeyMapperAppTask()?.moveToFront()
                }
            }
        }
    }

    override fun startWithRoot() {
        coroutineScope.launch {
            starter.startWithRoot()
        }
    }

    override fun startWithShizuku() {
        starter.startWithShizuku()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startWithAdb() {
        coroutineScope.launch {
            starter.startWithAdb()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun launchPairingAssistant() {
        launchWirelessDebuggingActivity()

        coroutineScope.launch {
            startSetupAssistantRequest.emit(SystemBridgeSetupStep.ADB_PAIRING)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun pairWirelessAdb(port: Int, code: Int): KMResult<Unit> {
        return adbManager.pair(port, code)
    }

    override fun enableDeveloperOptions() {
        SettingsUtils.launchSettingsScreen(
            ctx,
            Settings.ACTION_DEVICE_INFO_SETTINGS,
            "build_number"
        )

        coroutineScope.launch {
            startSetupAssistantRequest.emit(SystemBridgeSetupStep.DEVELOPER_OPTIONS)
        }
    }

    override fun launchEnableWirelessDebuggingAssistant() {
        // This is the intent sent by the quick settings tile. Not all devices support this.
        launchWirelessDebuggingActivity()

        coroutineScope.launch {
            startSetupAssistantRequest.emit(SystemBridgeSetupStep.WIRELESS_DEBUGGING)
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

    private fun launchWirelessDebuggingActivity() {
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

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        try {
            ctx.startActivity(quickSettingsIntent)
        } catch (_: ActivityNotFoundException) {
            SettingsUtils.launchSettingsScreen(
                ctx,
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                "toggle_adb_wireless"
            )
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
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.Q)
interface SystemBridgeSetupController {
    /**
     * The setup assistant should be launched for the given step.
     */
    val startSetupAssistantRequest: Flow<SystemBridgeSetupStep>

    val isDeveloperOptionsEnabled: Flow<Boolean>
    fun enableDeveloperOptions()

    val isWirelessDebuggingEnabled: Flow<Boolean>
    fun launchEnableWirelessDebuggingAssistant()

    fun launchPairingAssistant()

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun isAdbPaired(): Boolean

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun pairWirelessAdb(port: Int, code: Int): KMResult<Unit>

    fun startWithRoot()
    fun startWithShizuku()
    fun startWithAdb()
}