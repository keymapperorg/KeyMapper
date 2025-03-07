package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.isFixable
import io.github.sds100.keymapper.util.ui.ChipUi
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FingerprintMapListViewModel(
    private val coroutineScope: CoroutineScope,
    private val useCase: ListFingerprintMapsUseCase,
    resourceProvider: ResourceProvider,
) : PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider,
    NavigationViewModel by NavigationViewModelImpl() {

    private val listItemCreator = FingerprintMapListItemCreator(
        useCase,
        resourceProvider,
    )

    private val _state = MutableStateFlow<State<List<FingerprintMapListItem>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _requestFingerprintMapsBackup = MutableSharedFlow<Unit>()
    val requestFingerprintMapsBackup = _requestFingerprintMapsBackup.asSharedFlow()

    init {
        val rebuildUiState = MutableSharedFlow<List<FingerprintMap>>()

        combine(
            rebuildUiState,
            useCase.showDeviceDescriptors,
        ) { fingerprintMaps, showDeviceDescriptors ->
            val listItems =
                fingerprintMaps.map { listItemCreator.create(it, showDeviceDescriptors) }

            _state.value = State.Data(listItems)
        }.flowOn(Dispatchers.Default).launchIn(coroutineScope)

        coroutineScope.launch {
            useCase.fingerprintMaps.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        coroutineScope.launch {
            useCase.invalidateActionErrors.collectLatest {
                rebuildUiState.emit(useCase.fingerprintMaps.firstOrNull() ?: return@collectLatest)
            }
        }
    }

    fun onEnabledSwitchChange(id: FingerprintMapId, checked: Boolean) {
        if (checked) {
            useCase.enableFingerprintMap(id)
        } else {
            useCase.disableFingerprintMap(id)
        }
    }

    fun onCardClick(id: FingerprintMapId) {
        coroutineScope.launch {
            navigate("config_fingerprint_map", NavDestination.ConfigFingerprintMap(id))
        }
    }

    fun onBackupAllClick() {
        runBlocking { _requestFingerprintMapsBackup.emit(Unit) }
    }

    fun onResetClick() {
        coroutineScope.launch {
            val dialog = PopupUi.Dialog(
                title = getString(R.string.dialog_title_reset_fingerprint_maps),
                message = getString(R.string.dialog_message_reset_fingerprint_maps),
                positiveButtonText = getString(R.string.pos_yes),
                negativeButtonText = getString(R.string.neg_cancel),
            )

            val response = showPopup("reset_fingerprintmaps", dialog)

            if (response == DialogResponse.POSITIVE) {
                useCase.resetFingerprintMaps()
            }
        }
    }

    fun onActionChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showSnackBarAndFixError(chipModel.error)
        }
    }

    fun onConstraintsChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showSnackBarAndFixError(chipModel.error)
        }
    }

    private fun showSnackBarAndFixError(error: Error) {
        coroutineScope.launch {
            val actionText = if (error.isFixable) {
                getString(R.string.snackbar_fix)
            } else {
                null
            }

            val snackBar = PopupUi.SnackBar(
                message = error.getFullMessage(this@FingerprintMapListViewModel),
                actionText = actionText,
            )

            showPopup("fix_error", snackBar) ?: return@launch

            if (error.isFixable) {
                onFixError(error)
            }
        }
    }

    private fun onFixError(error: Error) {
        coroutineScope.launch {
            if (error == Error.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)) {
                coroutineScope.launch {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@FingerprintMapListViewModel,
                        popupViewModel = this@FingerprintMapListViewModel,
                        neverShowDndTriggerErrorAgain = { useCase.neverShowDndTriggerError() },
                        fixError = { useCase.fixError(error) },
                    )
                }
            } else {
                ViewModelHelper.showFixErrorDialog(
                    resourceProvider = this@FingerprintMapListViewModel,
                    popupViewModel = this@FingerprintMapListViewModel,
                    error,
                ) {
                    useCase.fixError(error)
                }
            }
        }
    }
}
