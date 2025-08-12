package io.github.sds100.keymapper.sysbridge.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.sysbridge.BuildConfig
import io.github.sds100.keymapper.sysbridge.IShizukuStarterService
import io.github.sds100.keymapper.sysbridge.adb.AdbClient
import io.github.sds100.keymapper.sysbridge.adb.AdbKey
import io.github.sds100.keymapper.sysbridge.adb.AdbKeyException
import io.github.sds100.keymapper.sysbridge.adb.AdbMdns
import io.github.sds100.keymapper.sysbridge.adb.AdbPairingClient
import io.github.sds100.keymapper.sysbridge.adb.AdbServiceType
import io.github.sds100.keymapper.sysbridge.adb.PreferenceAdbKeyStore
import io.github.sds100.keymapper.sysbridge.shizuku.ShizukuStarterService
import io.github.sds100.keymapper.sysbridge.starter.SystemBridgeStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This starter code is taken from the Shizuku project.
 */

@Singleton
class SystemBridgeSetupControllerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val buildConfigProvider: BuildConfigProvider
) : SystemBridgeSetupController {

    companion object {
        private const val DEVELOPER_OPTIONS_SETTING = "development_settings_enabled"
    }

    override val isDeveloperOptionsEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(getDeveloperOptionsEnabled())

    val sb = StringBuilder()

    private val adbConnectMdns: AdbMdns?

    private val scriptPath: String by lazy { SystemBridgeStarter.writeSdcardFiles(ctx) }
    private val apkPath = ctx.applicationInfo.sourceDir
    private val libPath = ctx.applicationInfo.nativeLibraryDir
    private val packageName = ctx.applicationInfo.packageName

    private val shizukuStarterConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            binder: IBinder?
        ) {
            Timber.i("Shizuku starter service connected")

            val service = IShizukuStarterService.Stub.asInterface(binder)

            Timber.i("Starting System Bridge with Shizuku starter service")
            try {
                service.startSystemBridge(scriptPath, apkPath, libPath, packageName)

            } catch (e: RemoteException) {
                Timber.e("Exception starting with Shizuku starter service: $e")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Do nothing. The service is supposed to immediately kill itself
            // after starting the command.
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            adbConnectMdns = AdbMdns(ctx, AdbServiceType.TLS_CONNECT)
        } else {
            adbConnectMdns = null
        }
    }

    override fun startWithRoot() {
        try {
            if (Shell.isAppGrantedRoot() != true) {
                Timber.w("Root is not granted. Cannot start System Bridge with Root.")
                return
            }

            val command =
                SystemBridgeStarter.buildStartCommand(scriptPath, apkPath, libPath, packageName)

            Timber.i("Starting System Bridge with root")
            Shell.cmd(command).exec().isSuccess

        } catch (e: Exception) {
            Timber.e("Exception when starting System Bridge with Root: $e")
        }
    }

    override fun startWithShizuku() {
        if (!Shizuku.pingBinder()) {
            Timber.w("Shizuku is not running. Cannot start System Bridge with Shizuku.")
            return
        }

        // Shizuku will start a service which will then start the System Bridge. Shizuku won't be
        // used to start the System Bridge directly because native libraries need to be used
        // and we want to limit the dependency on Shizuku as much as possible. Also, the System
        // Bridge should still be running even if Shizuku dies.
        val serviceComponentName = ComponentName(ctx, ShizukuStarterService::class.java)
        val args = Shizuku.UserServiceArgs(serviceComponentName)
            .daemon(false)
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(buildConfigProvider.versionCode)

        try {
            Shizuku.bindUserService(
                args,
                shizukuStarterConnection
            )
        } catch (e: Exception) {
            Timber.e("Exception when starting System Bridge with Shizuku. $e")
        }
    }

    // TODO clean up
    // TODO have lock so can only launch one start job at a time
    @RequiresApi(Build.VERSION_CODES.R)
    override fun startWithAdb() {
        // TODO kill the current service before starting it?

        if (adbConnectMdns == null) {
            return
        }

        coroutineScope.launch(Dispatchers.IO) {

            adbConnectMdns.start()

            val host = "127.0.0.1"
            val port = withTimeout(1000L) { adbConnectMdns.port.first { it != null } }

            if (port == null) {
                return@launch
            }

            writeStarterFiles()

            sb.append("Starting with wireless adb...").append('\n').append('\n')
            postResult()

            val key = try {
                val adbKey = AdbKey(
                    PreferenceAdbKeyStore(PreferenceManager.getDefaultSharedPreferences(ctx)),
                    "keymapper",
                )
                adbKey
            } catch (e: Throwable) {
                e.printStackTrace()
                sb.append('\n').append(Log.getStackTraceString(e))

                postResult(AdbKeyException(e))
                return@launch
            }

            AdbClient(host, port, key).runCatching {
                connect()
                shellCommand(SystemBridgeStarter.sdcardCommand) {
                    sb.append(String(it))
                    postResult()
                }
                close()
            }.onFailure {
                it.printStackTrace()

                sb.append('\n').append(Log.getStackTraceString(it))
                postResult(it)
            }

            /* Adb on MIUI Android 11 has no permission to access Android/data.
               Before MIUI Android 12, we can temporarily use /data/user_de.
               After that, is better to implement "adb push" and push files directly to /data/local/tmp.
             */
            if (sb.contains("/Android/data/${ctx.packageName}/start.sh: Permission denied")) {
                sb.append('\n')
                    .appendLine("adb have no permission to access Android/data, how could this possible ?!")
                    .appendLine("try /data/user_de instead...")
                    .appendLine()
                postResult()

                SystemBridgeStarter.writeDataFiles(ctx, true)

                AdbClient(host, port, key).runCatching {
                    connect()
                    shellCommand(SystemBridgeStarter.dataCommand) {
                        sb.append(String(it))
                        postResult()
                    }
                    close()
                }.onFailure {
                    it.printStackTrace()

                    sb.append('\n').append(Log.getStackTraceString(it))
                    postResult(it)
                }
            }

            adbConnectMdns.stop()

        }
    }

    private fun writeStarterFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                SystemBridgeStarter.writeSdcardFiles(ctx)
            } catch (e: Throwable) {
                // TODO show error message if fails to start
            }
        }
    }

    fun postResult(throwable: Throwable? = null) {
        if (throwable == null) {
            Timber.e(sb.toString())
        } else {
            Timber.e(throwable)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun pairWirelessAdb(port: Int, code: Int) {
        // TODO move this to AdbManager class
        coroutineScope.launch(Dispatchers.IO) {
            val host = "127.0.0.1"

            val key = try {
                AdbKey(
                    PreferenceAdbKeyStore(PreferenceManager.getDefaultSharedPreferences(ctx)),
                    "keymapper",
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                return@launch
            }

            AdbPairingClient(host, port, code.toString(), key).runCatching {
                start()
            }.onFailure {
                Timber.d("Pairing failed: $it")
//                handleResult(false, it)
            }.onSuccess {
                Timber.d("Pairing success")
//                handleResult(it, null)
            }
        }

//        val intent = AdbPairingService.startIntent(ctx)
//        try {
//            ctx.startForegroundService(intent)
//        } catch (e: Throwable) {
//            Timber.e("start ADB pairing service failed: $e")
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//                e is ForegroundServiceStartNotAllowedException
//            ) {
//                val mode = ctx.getSystemService(AppOpsManager::class.java)
//                    .noteOpNoThrow(
//                        "android:start_foreground",
//                        android.os.Process.myUid(),
//                        ctx.packageName,
//                        null,
//                        null,
//                    )
//                if (mode == AppOpsManager.MODE_ERRORED) {
//                    Toast.makeText(
//                        ctx,
//                        "OP_START_FOREGROUND is denied. What are you doing?",
//                        Toast.LENGTH_LONG,
//                    ).show()
//                }
//                ctx.startService(intent)
//            }
//        }
    }

    override fun enableDeveloperOptions() {
        // TODO show notification after the actvitiy is to tap the Build Number repeatedly

        SettingsUtils.launchSettingsScreen(
            ctx,
            Settings.ACTION_DEVICE_INFO_SETTINGS,
            "build_number"
        )
    }

    override fun enableWirelessDebugging() {
        SettingsUtils.launchSettingsScreen(
            ctx,
            Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            "toggle_adb_wireless"
        )
    }

    fun updateDeveloperOptionsEnabled() {
        isDeveloperOptionsEnabled.update { getDeveloperOptionsEnabled() }
    }

    private fun getDeveloperOptionsEnabled(): Boolean {
        try {
            return SettingsUtils.getGlobalSetting<Int>(ctx, DEVELOPER_OPTIONS_SETTING) == 1
        } catch (_: Settings.SettingNotFoundException) {
            return false
        }
    }
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.Q)
interface SystemBridgeSetupController {
    val isDeveloperOptionsEnabled: Flow<Boolean>
    fun enableDeveloperOptions()

    fun enableWirelessDebugging()

    @RequiresApi(Build.VERSION_CODES.R)
    fun pairWirelessAdb(port: Int, code: Int)

    fun startWithRoot()
    fun startWithShizuku()
    fun startWithAdb()
}