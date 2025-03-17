package io.github.sds100.keymapper.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 17/11/20.
 */
class HomeMenuViewModel(
    private val coroutineScope: CoroutineScope,
    private val showImePicker: ShowInputMethodPickerUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val _chooseBackupFile = MutableSharedFlow<Unit>()
    val chooseBackupFile = _chooseBackupFile.asSharedFlow()

    private val _chooseRestoreFile = MutableSharedFlow<Unit>()
    val chooseRestoreFile = _chooseRestoreFile.asSharedFlow()

    var showMenuBottomSheet by mutableStateOf(false)

    fun onShowInputMethodPickerClick() {
        showImePicker.show(fromForeground = true)
        showMenuBottomSheet = false
    }

    fun onOpenSettingsClick() {
        // dismiss afterwards so it is more responsive
        coroutineScope.launch {
            navigate("settings", NavDestination.Settings)
            showMenuBottomSheet = false
        }
    }

    fun onOpenAboutClick() {
        coroutineScope.launch {
            navigate("about", NavDestination.About)
            showMenuBottomSheet = false
        }
    }

    fun onBackupAllClick() {
        runBlocking {
            _chooseBackupFile.emit(Unit)
            showMenuBottomSheet = false
        }
    }

    fun onRestoreClick() {
        runBlocking {
            _chooseRestoreFile.emit(Unit)
            showMenuBottomSheet = false
        }
    }

    fun onReportBugClick() {
        coroutineScope.launch {
            navigate("report-bug", NavDestination.ReportBug)
            showMenuBottomSheet = false
        }
    }

    fun onCreateBackupFileActivityNotFound() {
        val dialog = PopupUi.Dialog(
            message = getString(R.string.dialog_message_no_app_found_to_create_file),
            positiveButtonText = getString(R.string.pos_ok),
        )

        coroutineScope.launch {
            showPopup("create_document_activity_not_found", dialog)
        }
    }

    fun onChooseRestoreFileActivityNotFound() {
        val dialog = PopupUi.Dialog(
            message = getString(R.string.dialog_message_no_app_found_to_choose_a_file),
            positiveButtonText = getString(R.string.pos_ok),
        )

        coroutineScope.launch {
            showPopup("get_content_activity_not_found", dialog)
        }
    }
}

enum class ToggleMappingsButtonState {
    PAUSED,
    RESUMED,
    SERVICE_DISABLED,
    SERVICE_CRASHED,
}
