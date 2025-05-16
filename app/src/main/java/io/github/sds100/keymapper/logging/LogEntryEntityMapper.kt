package io.github.sds100.keymapper.logging

import io.github.sds100.keymapper.data.entities.LogEntryEntity


object LogEntryEntityMapper {
    fun toEntity(model: LogEntry): LogEntryEntity {
        val severity = when (model.severity) {
            LogSeverity.ERROR -> LogEntryEntity.SEVERITY_ERROR
            LogSeverity.DEBUG -> LogEntryEntity.SEVERITY_DEBUG
            LogSeverity.INFO -> LogEntryEntity.SEVERITY_INFO
        }

        return LogEntryEntity(
            model.id,
            model.time,
            severity,
            model.message,
        )
    }

    fun fromEntity(model: LogEntryEntity): LogEntry {
        val severity = when (model.severity) {
            LogEntryEntity.SEVERITY_ERROR -> LogSeverity.ERROR
            LogEntryEntity.SEVERITY_DEBUG -> LogSeverity.DEBUG
            LogEntryEntity.SEVERITY_INFO -> LogSeverity.INFO

            else -> LogSeverity.DEBUG
        }

        return LogEntry(
            model.id,
            model.time,
            severity,
            model.message,
        )
    }
}
