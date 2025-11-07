package io.github.sds100.keymapper.base.logging

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ShareUtils
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.data.repositories.LogRepository
import io.github.sds100.keymapper.system.clipboard.ClipboardAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.files.IFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DisplayLogUseCaseImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val repository: LogRepository,
    private val resourceProvider: ResourceProvider,
    private val clipboardAdapter: ClipboardAdapter,
    private val fileAdapter: FileAdapter,
    private val buildConfigProvider: BuildConfigProvider,
) : DisplayLogUseCase {
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm:ss.SSS", Locale.getDefault())
    private val severityString: Map<Int, String> = mapOf(
        LogEntryEntity.SEVERITY_ERROR to "ERROR",
        LogEntryEntity.SEVERITY_WARNING to "WARN",
        LogEntryEntity.SEVERITY_INFO to "INFO",
        LogEntryEntity.SEVERITY_DEBUG to "DEBUG",
    )

    override val log: Flow<List<LogEntry>> = repository.log
        .map { entityList -> entityList.map { LogEntryEntityMapper.fromEntity(it) } }
        .flowOn(Dispatchers.Default)

    override fun clearLog() {
        repository.deleteAll()
    }

    override suspend fun copyToClipboard() {
        val logEntries = repository.log.first()
        val logText = createLogClipboardText(logEntries)

        clipboardAdapter.copy(
            label = resourceProvider.getString(R.string.clip_key_mapper_log),
            logText,
        )
    }

    private fun createLogClipboardText(logEntries: List<LogEntryEntity>): String {
        return buildString {
            append("Key Mapper log (newest first). Note: it may be cut off due to clipboard limits")
            appendLine()
            appendLine()

            logEntries
                .reversed()
                .joinToString(separator = "\n", transform = ::entryToString)
                .also { append(it) }
        }
    }

    private fun entryToString(entry: LogEntryEntity): String {
        val date = dateFormat.format(Date(entry.time))

        return "$date ${severityString[entry.severity]} ${entry.message}"
    }

    override fun shareFile() {
        val fileName = "logs/key_mapper_log_${FileUtils.createFileDate()}.txt"

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val logEntries = repository.log.first()
                val logText = logEntries.joinToString(separator = "\n", transform = ::entryToString)

                val file: IFile = fileAdapter.getPrivateFile(fileName)
                file.createFile()

                with(file.outputStream()?.bufferedWriter()) {
                    this?.write(logText)
                    this?.flush()
                }

                val publicUri = fileAdapter.getPublicUriForPrivateFile(file)

                ShareUtils.shareFile(ctx, publicUri.toUri(), buildConfigProvider.packageName)
            }
        }
    }
}

interface DisplayLogUseCase {
    val log: Flow<List<LogEntry>>
    fun clearLog()
    suspend fun copyToClipboard()
    fun shareFile()
}
