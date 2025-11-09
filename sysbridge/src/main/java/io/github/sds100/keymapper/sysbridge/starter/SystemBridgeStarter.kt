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
import io.github.sds100.keymapper.sysbridge.ktx.loge
import io.github.sds100.keymapper.sysbridge.shizuku.ShizukuStarterService
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
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
                    startSystemBridge(executeCommand = { command ->
                        val output = service.executeCommand(command)

                        if (output == null) {
                            KMError.UnknownIOError
                        } else {
                            Success(output)
                        }
                    })
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

        return startSystemBridge(executeCommand = adbManager::executeCommand)
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
        startSystemBridge(executeCommand = { command ->
            val output = withContext(Dispatchers.IO) {
                Shell.cmd(command).exec()
            }

            if (output.isSuccess) {
                Success(output.out.plus(output.err).joinToString("\n"))
            } else {
                KMError.UnknownIOError
            }
        })
    }

    suspend fun startSystemBridge(
        executeCommand: suspend (String) -> KMResult<String>,
    ): KMResult<String> {
        startMutex.withLock {
            val externalFilesParent = try {
                ctx.getExternalFilesDir(null)?.parentFile
            } catch (e: IOException) {
                return KMError.UnknownIOError
            }

            Timber.i("Copy starter files to ${externalFilesParent?.absolutePath}")

            val outputStarterBinary = File(externalFilesParent, "starter")
            val outputStarterScript = File(externalFilesParent, "start.sh")

            val copyFilesResult = withContext(Dispatchers.IO) {
                copyNativeLibrary(outputStarterBinary).then {
                    // Create the start.sh shell script
                    writeStarterScript(
                        outputStarterScript,
                        outputStarterBinary.absolutePath,
                    )
                    Success(Unit)
                }
            }

            val startCommand =
                "sh ${outputStarterScript.absolutePath} --apk=$baseApkPath --lib=$libPath --package=$packageName --version=${buildConfigProvider.versionCode}"

            return copyFilesResult
                .then { executeCommand(startCommand) }
                .then { output ->
                    // Adb on Android 11 has no permission to access Android/data so use /data/user_de.
                    if (output.contains(
                            "/Android/data/${ctx.packageName}/start.sh: Permission denied",
                        )
                    ) {
                        Timber.w(
                            "ADB has no permission to access Android/data/${ctx.packageName}/start.sh. Trying to use /data/user_de instead...",
                        )

                        startSystemBridgeFromProtectedStorage(executeCommand)
                    } else {
                        Success(output)
                    }
                }
        }
    }

    private suspend fun startSystemBridgeFromProtectedStorage(
        executeCommand: suspend (String) -> KMResult<String>,
    ): KMResult<String> {
        val protectedStorageDir =
            ctx.createDeviceProtectedStorageContext().filesDir.parentFile!!

        Timber.i("Protected storage dir: ${protectedStorageDir.absolutePath}")

        try {
            // 0711
            Os.chmod(protectedStorageDir.absolutePath, 457)
        } catch (e: ErrnoException) {
            e.printStackTrace()
        }

        Timber.i("Copy starter files to ${protectedStorageDir.absolutePath}")

        try {
            val outputStarterBinary = File(protectedStorageDir, "starter")
            val outputStarterScript = File(protectedStorageDir, "start.sh")

            withContext(Dispatchers.IO) {
                copyNativeLibrary(outputStarterBinary)

                writeStarterScript(
                    outputStarterScript,
                    outputStarterBinary.absolutePath,
                )
            }

            val startCommand =
                "sh ${outputStarterScript.absolutePath} --apk=$baseApkPath --lib=$libPath  --package=$packageName --version=${buildConfigProvider.versionCode}"

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

            return executeCommand(startCommand)
        } catch (e: IOException) {
            loge("write files", e)
            return KMError.UnknownIOError
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
     * Write the start.sh shell script to the specified [out] file. The path to the starter
     * binary will be substituted in the script with the [starterPath].
     */
    private fun writeStarterScript(out: File, starterPath: String) {
        if (!out.exists()) {
            out.createNewFile()
        }

        val scriptInputStream = ctx.resources.openRawResource(R.raw.start)

        with(scriptInputStream) {
            val reader = BufferedReader(InputStreamReader(this))

            val outputWriter = PrintWriter(FileWriter(out))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                outputWriter.println(line!!.replace("%%%STARTER_PATH%%%", starterPath))
            }

            outputWriter.flush()
            outputWriter.close()
        }
    }
}
