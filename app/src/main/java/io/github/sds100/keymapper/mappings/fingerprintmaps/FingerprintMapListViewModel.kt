package io.github.sds100.keymapper.mappings.fingerprintmaps

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class FingerprintMapListViewModel @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val useCase: ListFingerprintMapsUseCase,
    resourceProvider: ResourceProvider,
) : PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider,
    NavigationViewModel by NavigationViewModelImpl() {

    private val listItemCreator = FingerprintMapListItemCreator(
        useCase,
        resourceProvider
    )

    private val _state = MutableStateFlow<State<List<FingerprintMapListItem>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _requestFingerprintMapsBackup = MutableSharedFlow<Unit>()
    val requestFingerprintMapsBackup = _requestFingerprintMapsBackup.asSharedFlow()

    init {
        val rebuildUiState = MutableSharedFlow<List<FingerprintMap>>()

        combine(
            rebuildUiState,
            useCase.showDeviceDescriptors
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
                negativeButtonText = getString(R.string.neg_cancel)
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
                actionText = actionText
            )

            showPopup("fix_error", snackBar) ?: return@launch

            if (error.isFixable) {
                useCase.fixError(error)
            }
        }
    }
}