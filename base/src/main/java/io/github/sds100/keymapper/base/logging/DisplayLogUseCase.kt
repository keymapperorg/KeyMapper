package io.github.sds100.keymapper.base.logging

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.clipboard.ClipboardAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.common.state.State
import io.github.sds100.keymapper.common.state.ifIsData
import io.github.sds100.keymapper.common.state.mapData
import io.github.sds100.keymapper.base.util.ui.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class DisplayLogUseCaseImpl(
    private val repository: LogRepository,
    private val resourceProvider: ResourceProvider,
    private val clipboardAdapter: ClipboardAdapter,
    private val fileAdapter: FileAdapter,
) : DisplayLogUseCase {
    override val log: Flow<State<List<LogEntry>>> = repository.log
        .map { state ->
            state.mapData { entityList -> entityList.map { LogEntryEntityMapper.fromEntity(it) } }
        }
        .flowOn(Dispatchers.Default)

    override fun clearLog() {
        repository.deleteAll()
    }

    override suspend fun copyToClipboard(entryId: Set<Int>) {
        repository.log.first().ifIsData { logEntries ->
            val logText = LogUtils.createLogText(logEntries.filter { it.id in entryId })

            clipboardAdapter.copy(
                label = resourceProvider.getString(R.string.clip_key_mapper_log),
                logText,
            )
        }
    }

    override suspend fun saveToFile(uri: String, entryId: Set<Int>) {
        val file = fileAdapter.getFileFromUri(uri)

        repository.log.first().ifIsData { logEntries ->
            val logText = LogUtils.createLogText(logEntries.filter { it.id in entryId })

            file.outputStream()!!.bufferedWriter().use { it.write(logText) }
        }
    }
}

interface DisplayLogUseCase {
    val log: Flow<State<List<LogEntry>>>
    fun clearLog()
    suspend fun copyToClipboard(entryId: Set<Int>)
    suspend fun saveToFile(uri: String, entryId: Set<Int>)
}
