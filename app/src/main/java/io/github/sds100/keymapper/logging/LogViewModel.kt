package io.github.sds100.keymapper.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by sds100 on 14/05/2021.
 */

class LogViewModel(
    private val useCase: DisplayLogUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), PopupViewModel by PopupViewModelImpl(), ResourceProvider by resourceProvider {

    private val _listItems = MutableStateFlow<State<List<LogEntryListItem>>>(State.Loading)
    val listItems = _listItems.asStateFlow()

    private val dateFormat = LogUtils.DATE_FORMAT

    private val _pickFileToSaveTo = MutableSharedFlow<Unit>()
    val pickFileToSaveTo = _pickFileToSaveTo.asSharedFlow()

    private val showShortMessages = MutableStateFlow(true)

    init {
        combine(useCase.log,
            showShortMessages) { log, showShortMessages ->
            _listItems.value = log.mapData { logEntries ->
                logEntries.map { createListItem(it, showShortMessages) }
            }
        }.launchIn(viewModelScope)
    }

    fun onMenuItemClick(itemId: Int) {
        when (itemId) {
            R.id.action_clear -> useCase.clearLog()
            R.id.action_copy -> viewModelScope.launch {
                useCase.copyToClipboard()
                showPopup("copied", PopupUi.Toast(getString(R.string.toast_copied_log)))
            }
            R.id.action_short_messages -> {
                showShortMessages.value = !showShortMessages.value
            }
            R.id.action_save -> viewModelScope.launch {
                _pickFileToSaveTo.emit(Unit)
            }
        }
    }

    fun onPickFileToSaveTo(uri: String) {
        viewModelScope.launch {
            useCase.saveToFile(uri)
        }
    }

    private fun createListItem(logEntry: LogEntry, shortMessage: Boolean): LogEntryListItem {
        val textTint = if (logEntry.severity == LogSeverity.ERROR) {
            TintType.ERROR
        } else {
            TintType.ON_SURFACE
        }

        val message: String = if (shortMessage) {
            logEntry.message.split(',').getOrElse(0) { logEntry.message }
        } else {
            logEntry.message
        }

        return LogEntryListItem(
            id = logEntry.id,
            time = dateFormat.format(Date(logEntry.time)),
            textTint = textTint,
            message = message
        )
    }

    class Factory(
        private val useCase: DisplayLogUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            LogViewModel(useCase, resourceProvider) as T
    }
}