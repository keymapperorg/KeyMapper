package io.github.sds100.keymapper.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.MultiSelectProvider
import io.github.sds100.keymapper.util.ui.MultiSelectProviderImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SelectionState
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.ui.showPopup
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

/**
 * Created by sds100 on 14/05/2021.
 */

class LogViewModel(
    private val useCase: DisplayLogUseCase,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider {
    private val multiSelectProvider: MultiSelectProvider<Int> = MultiSelectProviderImpl()

    private val _listItems = MutableStateFlow<State<List<LogEntryListItem>>>(State.Loading)
    val listItems = _listItems.asStateFlow()

    private val dateFormat = LogUtils.DATE_FORMAT

    private val _pickFileToSaveTo = MutableSharedFlow<Unit>()
    val pickFileToSaveTo = _pickFileToSaveTo.asSharedFlow()

    val appBarState: StateFlow<LogAppBarState> = multiSelectProvider.state
        .map { selectionState ->
            when (selectionState) {
                is SelectionState.Selecting<*> -> LogAppBarState.MULTI_SELECTING
                else -> LogAppBarState.NORMAL
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, LogAppBarState.NORMAL)

    private val showShortMessages = MutableStateFlow(true)

    private val _goBack = MutableSharedFlow<Unit>()
    val goBack = _goBack.asSharedFlow()

    val dragSelectionHandler = object : DragSelectionProcessor.ISelectionHandler {
        override fun getSelection(): MutableSet<Int> =
            multiSelectProvider.getSelectedIds().toMutableSet()

        override fun isSelected(index: Int): Boolean {
            listItems.value.ifIsData {
                val id = it.getOrNull(index)?.id ?: return false

                return multiSelectProvider.isSelected(id)
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
                val selectedIds = selectedListItems.map { it.id }.toTypedArray()

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
                    val isSelected = if (selectionState is SelectionState.Selecting<*>) {
                        selectionState.selectedIds.contains(entry.id)
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
                    showPopup("copied", PopupUi.Toast(getString(R.string.toast_copied_log)))
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
        multiSelectProvider.toggleSelection(id)
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
        multiSelectProvider.getSelectedIds()
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

    class Factory(
        private val useCase: DisplayLogUseCase,
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            LogViewModel(useCase, resourceProvider) as T
    }
}

enum class LogAppBarState {
    MULTI_SELECTING,
    NORMAL,
}
