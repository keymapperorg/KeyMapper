package io.github.sds100.keymapper.base.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.ifIsData
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.base.utils.ui.MultiSelectProvider
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProviderImpl
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.SelectionState
import io.github.sds100.keymapper.base.utils.ui.TintType
import io.github.sds100.keymapper.base.utils.ui.showDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val useCase: DisplayLogUseCase,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider
) : ViewModel(),
    DialogProvider by dialogProvider,
    ResourceProvider by resourceProvider {
    private val multiSelectProvider: MultiSelectProvider = MultiSelectProvider()

    private val _listItems = MutableStateFlow<State<List<LogEntryListItem>>>(State.Loading)
    val listItems = _listItems.asStateFlow()

    private val dateFormat = LogUtils.DATE_FORMAT

    private val _pickFileToSaveTo = MutableSharedFlow<Unit>()
    val pickFileToSaveTo = _pickFileToSaveTo.asSharedFlow()

    val appBarState: StateFlow<LogAppBarState> = multiSelectProvider.state
        .map { selectionState ->
            when (selectionState) {
                is SelectionState.Selecting -> LogAppBarState.MULTI_SELECTING
                else -> LogAppBarState.NORMAL
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, LogAppBarState.NORMAL)

    private val showShortMessages = MutableStateFlow(true)

    private val _goBack = MutableSharedFlow<Unit>()
    val goBack = _goBack.asSharedFlow()

    val dragSelectionHandler = object : DragSelectionProcessor.ISelectionHandler {
        override fun getSelection(): MutableSet<Int> = multiSelectProvider.getSelectedIds().map { it.toInt() }.toMutableSet()

        override fun isSelected(index: Int): Boolean {
            listItems.value.ifIsData {
                val id = it.getOrNull(index)?.id ?: return false

                return multiSelectProvider.isSelected(id.toString())
            }

            return false
        }

        override fun updateSelection(
            start: Int,
            end: Int,
            isSelected: Boolean,
            calledFromOnStart: Boolean,
        ) {
            listItems.value.ifIsData { listItems ->
                val selectedListItems = listItems.slice(start..end)
                val selectedIds = selectedListItems.map { it.id.toString() }.toTypedArray()

                if (calledFromOnStart) {
                    multiSelectProvider.startSelecting()
                }

                if (isSelected) {
                    multiSelectProvider.select(*selectedIds)
                } else {
                    multiSelectProvider.deselect(*selectedIds)
                }
            }
        }
    }

    init {
        combine(
            useCase.log,
            showShortMessages,
            multiSelectProvider.state,
        ) { log, showShortMessages, selectionState ->
            _listItems.value = log.mapData { logEntries ->
                logEntries.map { entry ->
                    val isSelected = if (selectionState is SelectionState.Selecting) {
                        selectionState.selectedIds.contains(entry.id.toString())
                    } else {
                        false
                    }

                    createListItem(entry, showShortMessages, isSelected)
                }
            }
        }.flowOn(Dispatchers.Default).launchIn(viewModelScope)
    }

    fun onMenuItemClick(itemId: Int) {
        viewModelScope.launch {
            when (itemId) {
                R.id.action_clear -> useCase.clearLog()
                R.id.action_copy -> {
                    useCase.copyToClipboard(getSelectedLogEntries())
                    showDialog("copied", DialogModel.Toast(getString(R.string.toast_copied_log)))
                }

                R.id.action_short_messages -> {
                    showShortMessages.value = !showShortMessages.value
                }

                R.id.action_save ->
                    _pickFileToSaveTo.emit(Unit)
            }
        }
    }

    fun onListItemClick(id: Int) {
        multiSelectProvider.toggleSelection(id.toString())
    }

    fun onBackPressed() {
        if (multiSelectProvider.isSelecting()) {
            multiSelectProvider.stopSelecting()
        } else {
            viewModelScope.launch {
                _goBack.emit(Unit)
            }
        }
    }

    fun onPickFileToSaveTo(uri: String) {
        viewModelScope.launch {
            useCase.saveToFile(uri, getSelectedLogEntries())
        }
    }

    private suspend fun getSelectedLogEntries(): Set<Int> = if (multiSelectProvider.isSelecting()) {
        multiSelectProvider.getSelectedIds().map { it.toInt() }.toSet()
    } else {
        val logState = useCase.log.first()

        if (logState is State.Data) {
            logState.data.map { it.id }.toSet()
        } else {
            emptySet()
        }
    }

    private fun createListItem(
        logEntry: LogEntry,
        shortMessage: Boolean,
        isSelected: Boolean,
    ): LogEntryListItem {
        val textTint = if (logEntry.severity == LogSeverity.ERROR) {
            TintType.Error
        } else {
            TintType.OnSurface
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
            message = message,
            isSelected = isSelected,
        )
    }
}

enum class LogAppBarState {
    MULTI_SELECTING,
    NORMAL,
}
