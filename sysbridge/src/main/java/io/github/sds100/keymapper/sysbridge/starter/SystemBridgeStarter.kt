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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    private companion object {
        /**
         * How long to wait for the Shizuku user service to connect before
         * assuming it failed (e.g. due to the OEM bug on Xiaomi/MediaTek devices).
         */
        private const val SHIZUKU_USER_SERVICE_TIMEOUT_MS = 5000L
    }

    private fun buildShizukuUserServiceArgs(): Shizuku.UserServiceArgs {
        val serviceComponentName = ComponentName(ctx, ShizukuStarterService::class.java)
        return Shizuku.UserServiceArgs(serviceComponentName)
            .daemon(false)
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(buildConfigProvider.versionCode)
    }

    suspend fun startWithShizuku() {
        if (!Shizuku.pingBinder()) {
            Timber.w("Shizuku is not running. Cannot start System Bridge with Shizuku.")
            return
        }

        val serviceConnected = CompletableDeferred<Unit>()

        // Shizuku will start a service which will then start the System Bridge. Shizuku won't be
        // used to start the System Bridge directly because native libraries need to be used
        // and we want to limit the dependency on Shizuku as much as possible. Also, the System
        // Bridge should still be running even if Shizuku dies.
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                Timber.i("Shizuku starter service connected")

                // Signal that the user service started successfully before doing the work.
                serviceConnected.complete(Unit)

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

        val args = buildShizukuUserServiceArgs()

        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: Exception) {
            Timber.e("Exception when starting System Bridge with Shizuku. $e")
            return
        }

        // Wait for the user service to connect. On most devices this is near-instant.
        // On Xiaomi/MediaTek devices with OEM-modified LoadedApk.makeApplicationInner(),
        // the user service process crashes with a NPE before it can connect
        // (see https://github.com/RikkaApps/Shizuku/issues/1198).
        val connected = withTimeoutOrNull(SHIZUKU_USER_SERVICE_TIMEOUT_MS) {
            serviceConnected.await()
        }

        if (connected != null) {
            // User service connected successfully; the existing flow handles the rest.
            return
        }

        // Timeout expired. Use peekUserService to confirm the service isn't running.
        val serviceVersion = Shizuku.peekUserService(args, connection)

        if (serviceVersion >= 0) {
            // The service is running but just slow to connect. Let it proceed.
            Timber.w(
                "Shizuku user service is running (version=$serviceVersion) but took " +
                    "longer than ${SHIZUKU_USER_SERVICE_TIMEOUT_MS}ms to connect.",
            )
            return
        }

        // The service failed to start. Fall back to Shizuku.newProcess() via reflection.
        Timber.w("Falling back to Shizuku.newProcess() workaround.")
        startWithShizukuNewProcess()
    }

    /**
     * Fallback for starting the System Bridge via Shizuku when the user service fails to start.
     * This can happen on Xiaomi/MediaTek devices where an OEM modification to
     * LoadedApk.makeApplicationInner() causes a NPE during user service process creation.
     *
     * This method uses reflection to call the private Shizuku.newProcess() method, which
     * spawns a process directly on the Shizuku server side without going through
     * LoadedApk.makeApplication(), bypassing the OEM bug entirely.
     *
     * Note: Shizuku.newProcess() is deprecated and planned for removal in Shizuku API 14.
     * This workaround should be removed once the upstream Shizuku bug is fixed.
     */
    @Suppress("DiscouragedPrivateApi")
    private suspend fun startWithShizukuNewProcess() {
        Timber.i("Starting System Bridge with Shizuku newProcess fallback")

        startSystemBridgeWithLock(
            commandExecutor = { command ->
                try {
                    val method = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java,
                    )
                    method.isAccessible = true

                    val process = method.invoke(
                        null,
                        arrayOf("sh", "-c", command),
                        null,
                        null,
                    ) as Process

                    val stdout = process.inputStream.bufferedReader().readText()
                    val stderr = process.errorStream.bufferedReader().readText()
                    process.waitFor()

                    Success("$stdout\n$stderr")
                } catch (e: Exception) {
                    Timber.e("Shizuku newProcess fallback failed: $e")
                    KMError.Exception(e)
                }
            },
        )
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

    suspend fun refreshStarterScript() {
        writeStarterScript()
    }

    /**
     * Get the shell command that can be used to start the system bridge manually.
     * This command should be executed with 'adb shell'.
     */
    suspend fun getStartCommand(): KMResult<String> {
        return writeStarterScript().then { starterPath -> Success("sh $starterPath") }
    }

    private suspend fun writeStarterScript(): KMResult<String> {
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

        return copyStarterFiles(directory!!)
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
