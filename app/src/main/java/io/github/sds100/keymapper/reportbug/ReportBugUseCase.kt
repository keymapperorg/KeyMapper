package io.github.sds100.keymapper.reportbug

import android.os.Build
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.logging.LogRepository
import io.github.sds100.keymapper.logging.LogUtils
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.IFile
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

/**
 * Created by sds100 on 30/06/2021.
 */

class ReportBugUseCaseImpl(
    private val fileAdapter: FileAdapter,
    private val logRepository: LogRepository,
    private val backupManager: BackupManager,
) : ReportBugUseCase {

    companion object {
        private const val FILE_DEVICE_INFO = "info.txt"
        private const val FILE_MAPPINGS = "mappings.zip"
        private const val FILE_LOG = "log.txt"
    }

    override suspend fun createBugReport(uri: String): Result<*> {
        var tempFolder: IFile? = null

        try {
            tempFolder = fileAdapter.getPrivateFile("bug-report-temp/${UUID.randomUUID()}")
            tempFolder.createDirectory()

            // device info
            val infoText = buildString {
                append("Key Mapper version: ${Constants.VERSION}\n")

                append("Manufacturer: ${Build.DEVICE}\n")
                append("Device: ${Build.MANUFACTURER}\n")
                append("Model: ${Build.MODEL}\n")
                append("Android SDK version: ${Build.VERSION.SDK_INT}")
            }

            val deviceInfoFile = fileAdapter.getFile(tempFolder, FILE_DEVICE_INFO)
            deviceInfoFile.createFile()

            deviceInfoFile.outputStream()?.bufferedWriter()?.use {
                withContext(Dispatchers.IO) {
                    it.write(infoText)
                }
            }

            deviceInfoFile.createFile()

            deviceInfoFile.outputStream()?.bufferedWriter()?.use {
                withContext(Dispatchers.IO) {
                    it.write(infoText)
                }
            }

            // mappings
            val mappingsFile = fileAdapter.getFile(tempFolder, FILE_MAPPINGS)

            backupManager.backupEverything(mappingsFile.uri).onFailure {
                return it
            }

            // log
            val logFile = fileAdapter.getFile(tempFolder, FILE_LOG)
            logFile.createFile()

            logRepository.log
                .first { it is State.Data }
                .ifIsData { logEntries ->
                    val logText = LogUtils.createLogText(logEntries)

                    logFile.outputStream()?.bufferedWriter()?.use { it.write(logText) }
                }

            // zip
            val zipFile = fileAdapter.getFileFromUri(uri)
            val result =
                fileAdapter.createZipFile(zipFile, setOf(deviceInfoFile, logFile, mappingsFile))

            return result
        } catch (e: Exception) {
            Timber.e(e)

            return Error.Exception(e)
        } finally {
            tempFolder?.delete()
        }
    }
}

interface ReportBugUseCase {
    /**
     * @return the uri to the bug report
     */
    suspend fun createBugReport(uri: String): Result<*>
}
