package io.github.sds100.keymapper.sysbridge.starter

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Build
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import android.os.UserManager
import android.system.ErrnoException
import android.system.Os
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.sysbridge.BuildConfig
import io.github.sds100.keymapper.sysbridge.IShizukuStarterService
import io.github.sds100.keymapper.sysbridge.R
import io.github.sds100.keymapper.sysbridge.adb.AdbManager
import io.github.sds100.keymapper.sysbridge.shizuku.ShizukuStarterService
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber

@Singleton
class SystemBridgeStarter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val adbManager: AdbManager,
    private val buildConfigProvider: BuildConfigProvider,
) {
    private val userManager by lazy { ctx.getSystemService(UserManager::class.java)!! }

    // Important! Use getters because the values can change at runtime just after the app process
    // starts
    private val baseApkPath: String
        get() = ctx.applicationInfo.sourceDir
    private val splitApkPaths: Array<String>
        get() = ctx.applicationInfo.splitSourceDirs ?: emptyArray()
    private val libPath: String?
        get() = ctx.applicationInfo.nativeLibraryDir
    private val packageName: String
        get() = ctx.applicationInfo.packageName

    private val startMutex: Mutex = Mutex()

    private val shizukuStarterConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Timber.i("Shizuku starter service connected")

            val service = IShizukuStarterService.Stub.asInterface(binder)

            Timber.i("Starting System Bridge with Shizuku starter service")
            try {
                runBlocking {
                    startSystemBridgeWithLock(
                        commandExecutor = { command ->
                            val output = service.executeCommand(command)

                            if (output == null) {
                                KMError.UnknownIOError
                            } else {
                                Success(output)
                            }
                        },
                    )
                }
            } catch (e: RemoteException) {
                Timber.e("Exception starting with Shizuku starter service: $e")
            } finally {
                try {
                    service.destroy()
                } catch (_: DeadObjectException) {
                    // Do nothing. Service is already dead.
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Do nothing. The service is supposed to immediately kill itself
            // after starting the command.
        }
    }

    fun startWithShizuku() {
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
                shizukuStarterConnection,
            )
        } catch (e: Exception) {
            Timber.e("Exception when starting System Bridge with Shizuku. $e")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun startWithAdb(): KMResult<String> {
        if (!userManager.isUserUnlocked) {
            return KMError.Exception(IllegalStateException("User is locked"))
        }

        return startSystemBridgeWithLock(commandExecutor = adbManager::executeCommand)
            .onFailure { error ->
                Timber.e("Failed to start system bridge with ADB: $error")
            }
    }

    suspend fun startWithRoot() {
        if (Shell.isAppGrantedRoot() != true) {
            Timber.e("Root is not granted. Cannot start System Bridge with Root.")
            return
        }

        Timber.i("Starting System Bridge with root")
        startSystemBridgeWithLock(
            commandExecutor = { command ->
                val output = withContext(Dispatchers.IO) {
                    Shell.cmd(command).exec()
                }

                if (output.isSuccess) {
                    Success(output.out.plus(output.err).joinToString("\n"))
                } else {
                    KMError.UnknownIOError
                }
            },
        )
    }

    suspend fun startSystemBridgeWithLock(
        commandExecutor: suspend (String) -> KMResult<String>,
    ): KMResult<String> {
        startMutex.withLock {
            return startSystemBridge(commandExecutor)
        }
    }

    /**
     * Get the shell command that can be used to start the system bridge manually.
     * This command should be executed with 'adb shell'.
     */
    suspend fun getStartCommand(): KMResult<String> {
        val directory = if (buildConfigProvider.sdkInt > Build.VERSION_CODES.R) {
            try {
                ctx.getExternalFilesDir(null)?.parentFile
            } catch (e: IOException) {
                return KMError.UnknownIOError
            }
        } else {
            // Adb on Android 11 has no permission to access Android/data so use /data/user_de.
            val protectedStorageDir =
                ctx.createDeviceProtectedStorageContext().filesDir.parentFile!!

            try {
                // 0711
                Os.chmod(protectedStorageDir.absolutePath, 457)
            } catch (e: ErrnoException) {
                e.printStackTrace()
            }

            protectedStorageDir
        }

        return copyStarterFiles(directory!!).then { starterPath -> Success("sh $starterPath") }
    }

    /**
     * @return The path to the starter script.
     */
    private suspend fun copyStarterFiles(directory: File): KMResult<String> {
        Timber.i("Copy starter files to ${directory.absolutePath}")

        val outputStarterBinary = File(directory, "starter")
        val outputStarterScript = File(directory, "start.sh")

        return withContext(Dispatchers.IO) {
            copyNativeLibrary(outputStarterBinary).then {
                // Create the start.sh shell script
                writeStarterScript(
                    outputStarterScript,
                    outputStarterBinary.absolutePath,
                )

                // Make starter binary executable
                try {
                    // 0644
                    Os.chmod(outputStarterBinary.absolutePath, 420)
                } catch (e: ErrnoException) {
                    e.printStackTrace()
                }

                // Make starter script executable
                try {
                    // 0644
                    Os.chmod(outputStarterScript.absolutePath, 420)
                } catch (e: ErrnoException) {
                    e.printStackTrace()
                }

                Success(outputStarterScript.absolutePath)
            }
        }
    }

    private suspend fun startSystemBridge(
        commandExecutor: suspend (String) -> KMResult<String>,
    ): KMResult<String> {
        return getStartCommand().then { scriptPath ->
            commandExecutor(scriptPath)
        }
    }

    /**
     * This extracts the library file from inside the apk and copies it to [out] File.
     */
    private fun copyNativeLibrary(out: File): KMResult<Unit> {
        Timber.i("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        Timber.i("Attempt to copy native library from: $libPath")

        val libraryName = "libsysbridge.so"

        try {
            // copyTo throws an exception if it already exists
            out.delete()

            File("$libPath/$libraryName").copyTo(out)
            return Success(Unit)
        } catch (e: Exception) {
            Timber.w("Native library not found. Extracting from APKs. Exception: $e")

            val apkPaths: Array<String> = arrayOf(baseApkPath, *splitApkPaths)

            Timber.i("APK paths: ${apkPaths.joinToString()}")

            for (apk in apkPaths) {
                with(ZipFile(apk)) {
                    for (abi in Build.SUPPORTED_ABIS) {
                        val expectedLibraryPath = "lib/$abi/$libraryName"

                        // Open the apk so the library file can be found
                        val entry: ZipEntry = getEntry(expectedLibraryPath) ?: continue

                        with(DataInputStream(getInputStream(entry))) {
                            val input = this
                            with(FileOutputStream(out)) {
                                val output = this
                                input.copyTo(output)
                            }
                        }

                        return Success(Unit)
                    }
                }
            }
        }

        return KMError.SourceFileNotFound(libraryName)
    }

    /**
     * Write the start.sh shell script to the specified [out] file. The placeholders in the script
     * will be substituted with the provided values.
     */
    private fun writeStarterScript(out: File, starterPath: String) {
        out.createNewFile()

        val scriptInputStream = ctx.resources.openRawResource(R.raw.start)

        with(BufferedReader(InputStreamReader(scriptInputStream))) {
            val text = readText()
                .replace("%%%STARTER_PATH%%%", starterPath)
                .replace("%%%APK_PATH%%%", baseApkPath)
                .replace("%%%LIB_PATH%%%", libPath ?: "")
                .replace("%%%PACKAGE_NAME%%%", packageName)
                .replace(
                    "%%%VERSION_CODE%%%",
                    buildConfigProvider.versionCode.toString(),
                )

            with(out) {
                writeText(text)
            }
        }
    }
}
