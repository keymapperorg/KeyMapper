package io.github.sds100.keymapper.base.logging

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LogViewModel @Inject constructor(private val displayLogUseCase: DisplayLogUseCase) :
    ViewModel() {
    @SuppressLint("ConstantLocale")
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm:ss.SSS", Locale.getDefault())

    val log: StateFlow<List<LogListItem>> = displayLogUseCase.log
        .map { list ->
            list.map {
                LogListItem(
                    id = it.id,
                    time = dateFormat.format(it.time),
                    message = it.message,
                    severity = it.severity,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun onCopyToClipboardClick() {
        viewModelScope.launch {
            displayLogUseCase.copyToClipboard()
        }
    }

    fun onShareFileClick() {
        viewModelScope.launch {
            displayLogUseCase.shareFile()
        }
    }

    fun onClearLogClick() {
        displayLogUseCase.clearLog()
    }
}
