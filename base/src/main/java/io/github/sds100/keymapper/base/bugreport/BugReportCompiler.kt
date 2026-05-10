package io.github.sds100.keymapper.base.bugreport

import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import io.github.sds100.keymapper.base.actions.ExecuteShellCommandUseCase
import io.github.sds100.keymapper.base.backup.BackupManager
import io.github.sds100.keymapper.base.debug.GetEventRecorder
import io.github.sds100.keymapper.base.input.EvdevDevicesDelegate
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.data.repositories.LogRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.isConnected
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.files.IFile
import io.github.sds100.keymapper.system.files.toJavaFile
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Compiles a zip file of diagnostic information that the user can attach to a bug report.
 *
 * Only one bug report can be compiled at a time; concurrent calls to [compile] are serialized
 * by an internal [Mutex].
 */
@Singleton
class BugReportCompiler @Inject constructor(
    private val fileAdapter: FileAdapter,
    private val buildConfigProvider: BuildConfigProvider,
    private val packageManagerAdapter: PackageManagerAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val devicesAdapter: DevicesAdapter,
    private val evdevDevicesDelegate: EvdevDevicesDelegate,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val preferenceRepository: PreferenceRepository,
    private val logRepository: LogRepository,
    private val backupManager: BackupManager,
    private val executeShellCommandUseCase: ExecuteShellCommandUseCase,
    private val getEventRecorder: GetEventRecorder,
) {
    companion object {
        // Public so the FileProvider path entry can reuse the same value.
        const val BUG_REPORT_DIR = "bug_reports"
        private const val TEMP_BUG_REPORT_ROOT_DIR = "bug_report_temp"

        private const val FILE_APP_INFO = "app_info.txt"
        private const val FILE_INPUT_METHODS = "input_methods.txt"
        private const val FILE_ANDROID_INPUT_DEVICES = "android_input_devices.txt"
        private const val FILE_EVDEV_DEVICES = "evdev_devices.txt"
        private const val FILE_GETEVENT_DEVICE_INFO = "getevent_device_info.txt"
        private const val FILE_GETEVENT_EVENTS = "getevent_events.txt"
        private const val FILE_LOG = "log.txt"
        private const val FILE_KEY_MAPS = "key_maps.zip"
        private const val FILE_KEY_LAYOUT_MAPS = "keylayout_files.zip"

        private const val STAGING_KEYLAYOUT_DIR = "keylayout_files_staging"
        private const val README_NO_KEYLAYOUT_FILES = "keylayout_README.txt"

        private val KEYLAYOUT_SEARCH_DIRS = listOf(
            "/odm/usr/keylayout",
            "/vendor/usr/keylayout",
            "/system/usr/keylayout",
            "/data/system/devices/keylayout",
        )

        private const val FIND_KEYLAYOUT_TIMEOUT_MS = 30_000L
        private const val CAT_KEYLAYOUT_TIMEOUT_MS = 5_000L
        private const val MAX_KEYLAYOUT_FILES = 500
        private const val MAX_KEYLAYOUT_TOTAL_BYTES = 5_000_000
    }

    private val mutex = Mutex()

    private val _state: MutableStateFlow<BugReportState> = MutableStateFlow(BugReportState.Idle)
    val state: StateFlow<BugReportState> = _state.asStateFlow()

    private val logDateFormat = SimpleDateFormat("MM/dd HH:mm:ss.SSS", Locale.US)
    private val severityString: Map<Int, String> = mapOf(
        LogEntryEntity.SEVERITY_ERROR to "ERROR",
        LogEntryEntity.SEVERITY_WARNING to "WARN",
        LogEntryEntity.SEVERITY_INFO to "INFO",
        LogEntryEntity.SEVERITY_DEBUG to "DEBUG",
    )

    /**
     * Compile a bug report zip. If a compilation is already in progress, the call suspends until
     * it finishes.
     *
     * @return [Success] with the [IFile] pointing at the zip on disk, or a [KMError] on failure.
     */
    suspend fun compile(): KMResult<IFile> = mutex.withLock {
        _state.value = BugReportState.Compiling
        val result = withContext(Dispatchers.IO) { compileInternal() }
        _state.value = when (result) {
            is Success -> BugReportState.Success(result.value)
            is KMError -> BugReportState.Error(result)
        }
        result
    }

    private suspend fun compileInternal(): KMResult<IFile> {
        val tempUid = UUID.randomUUID().toString()
        val tempDir = fileAdapter.getPrivateFile("$TEMP_BUG_REPORT_ROOT_DIR/$tempUid")
        tempDir.createDirectory()

        try {
            val files = mutableSetOf<IFile>()

            files += writeTextFile(tempDir, FILE_APP_INFO, buildAppInfo())
            files += writeTextFile(tempDir, FILE_INPUT_METHODS, buildInputMethodsInfo())
            files += writeTextFile(
                tempDir,
                FILE_ANDROID_INPUT_DEVICES,
                buildAndroidInputDevicesInfo(),
            )
            files += writeTextFile(tempDir, FILE_EVDEV_DEVICES, buildEvdevDevicesInfo())

            if (systemBridgeConnectionManager.isConnected()) {
                maybeRefreshGetEventDeviceInfoIfEmpty()
                files += writeTextFile(
                    tempDir,
                    FILE_GETEVENT_DEVICE_INFO,
                    preferenceRepository.get(Keys.getEventDeviceInfoOutput).first().orEmpty(),
                )
                files += writeTextFile(
                    tempDir,
                    FILE_GETEVENT_EVENTS,
                    preferenceRepository.get(Keys.getEventEventsOutput).first().orEmpty(),
                )
            }

            files += writeTextFile(tempDir, FILE_LOG, buildLogText())

            val keyMapsResult = buildKeyMapsBackup(tempDir)
            if (keyMapsResult is Success) {
                files += keyMapsResult.value
            } else if (keyMapsResult is KMError) {
                Timber.w("Failed to include key maps backup in bug report: $keyMapsResult")
            }

            val keyLayoutZip = buildKeyLayoutMapsZip(tempDir)
            if (keyLayoutZip != null) {
                files += keyLayoutZip
            }

            val outputDir = fileAdapter.getPrivateFile(BUG_REPORT_DIR)
            outputDir.createDirectory()

            val outputFileName = "key_mapper_bug_report_${FileUtils.createFileDate()}.zip"
            val outputFile = fileAdapter.getPrivateFile("$BUG_REPORT_DIR/$outputFileName")
            outputFile.createFile()

            return fileAdapter.createZipFile(outputFile, files).then { Success(outputFile) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to compile bug report")
            return KMError.Exception(e)
        } finally {
            runCatching {
                tempDir.toJavaFile().deleteRecursively()
            }.onFailure { Timber.w(it, "Failed to clean up bug report temp dir") }
        }
    }

    private fun writeTextFile(parent: IFile, name: String, contents: String): IFile {
        val file = fileAdapter.getFile(parent, name)
        file.createFile()
        file.outputStream()?.bufferedWriter()?.use { writer ->
            writer.write(contents)
            writer.flush()
        }
        return file
    }

    private fun buildAppInfo(): String = buildString {
        appendLine("Key Mapper version: ${buildConfigProvider.version}")
        appendLine("Key Mapper version code: ${buildConfigProvider.versionCode}")
        appendLine("Package name: ${buildConfigProvider.packageName}")

        val androidVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Build.VERSION.RELEASE_OR_CODENAME
        } else {
            Build.VERSION.RELEASE
        }
        appendLine("Android version: $androidVersion (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Model: ${Build.MODEL}")
        appendLine("Device: ${Build.DEVICE}")
        appendLine("Install source: ${formatInstallSource()}")
    }

    private fun formatInstallSource(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "Unknown (Android < 11)"
        }

        val installer = runCatching { packageManagerAdapter.getInstallSourcePackageName() }
            .getOrNull()

        return when (installer) {
            null -> "Sideload"
            "com.android.vending" -> "Google Play ($installer)"
            "org.fdroid.fdroid", "org.fdroid.fdroid.privileged" -> "F-Droid ($installer)"
            "com.github.android" -> "GitHub ($installer)"
            else -> installer
        }
    }

    private fun buildInputMethodsInfo(): String = buildString {
        val chosenIme = inputMethodAdapter.getChosenIme()
        appendLine("Chosen IME: ${chosenIme?.let { "${it.label} (${it.id})" } ?: "<none>"}")
        appendLine()

        appendLine("Installed input methods:")
        val inputMethods = inputMethodAdapter.inputMethods.value
        if (inputMethods.isEmpty()) {
            appendLine("  <none>")
        } else {
            for (ime in inputMethods) {
                appendLine("  - ${ime.label}")
                appendLine("      id: ${ime.id}")
                appendLine("      package: ${ime.packageName}")
                appendLine("      enabled: ${ime.isEnabled}")
                appendLine("      chosen: ${ime.isChosen}")
            }
        }
        appendLine()

        appendLine("Input method history (most recently used first):")
        val history = inputMethodAdapter.inputMethodHistory.value
        if (history.isEmpty()) {
            appendLine("  <none>")
        } else {
            for ((index, ime) in history.withIndex()) {
                appendLine("  ${index + 1}. ${ime.label} (${ime.id})")
            }
        }
    }

    private fun buildAndroidInputDevicesInfo(): String = buildString {
        appendLine("Android input devices (from android.view.InputDevice):")

        val devices = devicesAdapter.connectedInputDevices.value.dataOrNull()
        if (devices.isNullOrEmpty()) {
            appendLine("  <none>")
            return@buildString
        }

        for (device in devices) {
            appendLine("  - ${device.name}")
            appendLine("      id: ${device.id}")
            appendLine("      descriptor: ${device.descriptor}")
            appendLine("      isExternal: ${device.isExternal}")
            appendLine("      isGameController: ${device.isGameController}")
            appendLine("      sources: 0x${device.sources.toString(16)}")
        }
    }

    private fun buildEvdevDevicesInfo(): String = buildString {
        appendLine("Evdev input devices (from system bridge):")

        if (!systemBridgeConnectionManager.isConnected()) {
            appendLine("  <expert mode disabled>")
            return@buildString
        }

        val devices = evdevDevicesDelegate.allDevices.value
        if (devices.isEmpty()) {
            appendLine("  <none>")
            return@buildString
        }

        for (device in devices) {
            appendLine("  - ${device.name}")
            appendLine("      bus: 0x${device.bus.toString(16)}")
            appendLine("      vendor: 0x${device.vendor.toString(16)}")
            appendLine("      product: 0x${device.product.toString(16)}")
        }
    }

    private suspend fun buildLogText(): String {
        val entries = logRepository.log.first()
        if (entries.isEmpty()) {
            return "<empty>"
        }
        return entries.joinToString(separator = "\n") { entry ->
            val date = logDateFormat.format(Date(entry.time))
            "$date ${severityString[entry.severity] ?: entry.severity} ${entry.message}"
        }
    }

    private suspend fun maybeRefreshGetEventDeviceInfoIfEmpty() {
        if (!systemBridgeConnectionManager.isConnected()) return

        val current = preferenceRepository.get(Keys.getEventDeviceInfoOutput).first().orEmpty()
        if (current.isNotBlank()) return

        getEventRecorder.refreshDeviceInfo()
    }

    /**
     * Collects key layout map files from standard paths into [FILE_KEY_LAYOUT_MAPS] via ADB-mode
     * shell (system bridge). Returns null only when the zip could not be created; when the bridge
     * is disconnected this returns null without logging (nothing to collect).
     */
    private suspend fun buildKeyLayoutMapsZip(parent: IFile): IFile? {
        if (!systemBridgeConnectionManager.isConnected()) return null

        val staging = fileAdapter.getFile(parent, STAGING_KEYLAYOUT_DIR)
        staging.createDirectory()

        val findResult = executeShellCommandUseCase.execute(
            command = buildFindKeylayoutCommand(),
            executionMode = ShellExecutionMode.ADB,
            timeoutMillis = FIND_KEYLAYOUT_TIMEOUT_MS,
        )

        val paths: List<String>
        val enumerateOk: Boolean
        when (findResult) {
            is Success -> {
                enumerateOk = true
                paths = parseKeylayoutPaths(findResult.value.stdout)
            }

            is KMError -> {
                enumerateOk = false
                paths = emptyList()
                Timber.w("Failed to enumerate keylayout files: $findResult")
            }
        }

        var totalBytes = 0
        var fileCount = 0

        for (path in paths) {
            if (fileCount >= MAX_KEYLAYOUT_FILES) {
                Timber.w("Keylayout collection stopped: reached max files ($MAX_KEYLAYOUT_FILES)")
                break
            }

            val catResult = executeShellCommandUseCase.execute(
                command = "cat ${shellSingleQuote(path)}",
                executionMode = ShellExecutionMode.ADB,
                timeoutMillis = CAT_KEYLAYOUT_TIMEOUT_MS,
            )

            val text = when (catResult) {
                is Success -> {
                    val shell = catResult.value
                    val code = shell.exitCode
                    if (code != null && code != 0) {
                        Timber.w("cat keylayout file failed (exit $code): $path")
                        continue
                    }
                    shell.stdout
                }

                is KMError -> {
                    Timber.w("cat keylayout file error for $path: $catResult")
                    continue
                }
            }

            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            if (totalBytes + bytes.size > MAX_KEYLAYOUT_TOTAL_BYTES) {
                Timber.w(
                    "Keylayout collection stopped: size cap ($MAX_KEYLAYOUT_TOTAL_BYTES bytes)",
                )
                break
            }

            val relative = path.trimStart('/')
            try {
                val out = fileAdapter.getFile(staging, relative)
                out.createFile()
                out.outputStream()?.use { it.write(bytes) }
                totalBytes += bytes.size
                fileCount++
            } catch (e: Exception) {
                Timber.w(e, "Failed to write staged keylayout file $relative")
            }
        }

        if (fileCount == 0) {
            writeTextFile(
                staging,
                README_NO_KEYLAYOUT_FILES,
                buildKeylayoutReadmeText(enumerateOk, paths.size),
            )
        }

        val zipFile = fileAdapter.getFile(parent, FILE_KEY_LAYOUT_MAPS)
        zipFile.createFile()

        val zipResult = fileAdapter.createZipFile(zipFile, setOf(staging))
        return when (zipResult) {
            is Success -> zipFile
            is KMError -> {
                Timber.w("Failed to create keylayout zip in bug report: $zipResult")
                null
            }
        }
    }

    private fun buildFindKeylayoutCommand(): String {
        val dirs = KEYLAYOUT_SEARCH_DIRS.joinToString(" ") { shellSingleQuote(it) }
        return "for d in $dirs; do [ -d \"${'$'}d\" ] && find \"${'$'}d\" -type f 2>/dev/null; done"
    }

    private fun shellSingleQuote(path: String): String = "'${path.replace("'", "'\\''")}'"

    private fun buildKeylayoutReadmeText(enumerateOk: Boolean, pathsFound: Int): String =
        buildString {
            if (!enumerateOk) {
                appendLine("Could not list keylayout directories (ADB shell command failed).")
                appendLine()
            }
            if (enumerateOk && pathsFound == 0) {
                appendLine("No files were found under:")
                KEYLAYOUT_SEARCH_DIRS.forEach { appendLine("  - $it") }
                appendLine()
            }
            if (enumerateOk && pathsFound > 0) {
                appendLine(
                    "Listed $pathsFound file path(s) but could not read any contents " +
                        "(permission denied, missing file, or non-zero exit from cat).",
                )
                appendLine()
            }
        }

    private suspend fun buildKeyMapsBackup(parent: IFile): KMResult<IFile> {
        val backupFile = fileAdapter.getFile(parent, FILE_KEY_MAPS)
        backupFile.createFile()

        return backupManager.backupEverything(backupFile).then { Success(backupFile) }
    }

    /**
     * Returns a content [Uri] for sharing the given bug report file via the FileProvider
     * declared in the base module's AndroidManifest.
     */
    fun getShareUri(file: IFile): Uri = fileAdapter.getPublicUriForPrivateFile(file).toUri()
}

sealed class BugReportState {
    data object Idle : BugReportState()
    data object Compiling : BugReportState()
    data class Success(val file: IFile) : BugReportState()
    data class Error(val error: KMError) : BugReportState()
}
