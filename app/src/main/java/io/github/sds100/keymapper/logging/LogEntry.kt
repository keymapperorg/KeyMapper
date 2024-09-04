package io.github.sds100.keymapper.logging

/**
 * Created by sds100 on 13/05/2021.
 */
data class LogEntry(
    val id: Int,
    val time: Long,
    val severity: LogSeverity,
    val message: String,
)
