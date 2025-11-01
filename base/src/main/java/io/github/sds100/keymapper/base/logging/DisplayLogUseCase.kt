package io.github.sds100.keymapper.base.logging

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.data.repositories.LogRepository
import io.github.sds100.keymapper.system.clipboard.ClipboardAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class DisplayLogUseCaseImpl @Inject constructor(
    private val repository: LogRepository,
    private val resourceProvider: ResourceProvider,
    private val clipboardAdapter: ClipboardAdapter,
    private val fileAdapter: FileAdapter,
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
        val logText = createLogText(logEntries)

        clipboardAdapter.copy(
            label = resourceProvider.getString(R.string.clip_key_mapper_log),
            logText,
        )
    }

    private fun createLogText(logEntries: List<LogEntryEntity>): String {
        return logEntries.joinToString(separator = "\n") { entry ->
            val date = dateFormat.format(Date(entry.time))

            return@joinToString "$date ${severityString[entry.severity]} ${entry.message}"
        }
    }
}

interface DisplayLogUseCase {
    val log: Flow<List<LogEntry>>
    fun clearLog()
    suspend fun copyToClipboard()
}
