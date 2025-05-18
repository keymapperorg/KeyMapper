package io.github.sds100.keymapper.base.logging

import android.util.Log
import io.github.sds100.keymapper.data.repositories.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@Inject
class KeyMapperLoggingTree constructor(
    private val coroutineScope: CoroutineScope,
    preferenceRepository: io.github.sds100.keymapper.data.repositories.PreferenceRepository,
    private val logRepository: LogRepository,
) : Timber.Tree() {
    private val logEverything: StateFlow<Boolean> = preferenceRepository.get(io.github.sds100.keymapper.data.Keys.log)
        .map { it ?: false }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private val messagesToLog = MutableSharedFlow<io.github.sds100.keymapper.data.entities.LogEntryEntity>(
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )

    init {
        messagesToLog
            .onEach {
                logRepository.insertSuspend(it)
            }
            .flowOn(Dispatchers.Default)
            .launchIn(coroutineScope)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // error and info logs should always log even if the user setting is turned off
        if (!logEverything.value && priority != Log.ERROR && priority != Log.INFO) {
            return
        }

        val severity = when (priority) {
            Log.ERROR -> io.github.sds100.keymapper.data.entities.LogEntryEntity.SEVERITY_ERROR
            Log.DEBUG -> io.github.sds100.keymapper.data.entities.LogEntryEntity.SEVERITY_DEBUG
            Log.INFO -> io.github.sds100.keymapper.data.entities.LogEntryEntity.SEVERITY_INFO
            else -> io.github.sds100.keymapper.data.entities.LogEntryEntity.SEVERITY_DEBUG
        }

        messagesToLog.tryEmit(
            io.github.sds100.keymapper.data.entities.LogEntryEntity(
                id = 0,
                time = Calendar.getInstance().timeInMillis,
                severity = severity,
                message = message,
            ),
        )
    }
}
