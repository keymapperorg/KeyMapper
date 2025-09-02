package io.github.sds100.keymapper.base.logging

data class LogEntry(
    val id: Int,
    val time: Long,
    val severity: LogSeverity,
    val message: String,
)
