package io.github.sds100.keymapper.mappings.fingerprintmaps

import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class FingerprintMapListViewModel(
    private val coroutineScope: CoroutineScope,
    private val useCase: ListFingerprintMapsUseCase,
    resourceProvider: ResourceProvider,
) : PopupViewModel by PopupViewModelImpl(), ResourceProvider by resourceProvider {

    private val listItemCreator = FingerprintMapListItemCreator(
        useCase,
        resourceProvider
    )

    private val _state = MutableStateFlow<ListUiState<FingerprintMapListItem>>(ListUiState.Loading)
    val state = _state.asStateFlow()

    private val _launchConfigFingerprintMap = MutableSharedFlow<FingerprintMapId>()
    val launchConfigFingerprintMap = _launchConfigFingerprintMap.asSharedFlow()

    private val _requestFingerprintMapsBackup = MutableSharedFlow<Unit>()
    val requestFingerprintMapsBackup = _requestFingerprintMapsBackup.asSharedFlow()

    init {
        val rebuildUiState = MutableSharedFlow<FingerprintMapGroup>()

        coroutineScope.launch {
            rebuildUiState.collectLatest { fingerprintMaps ->
                _state.value = withContext(Dispatchers.Default) {
                    mutableListOf(
                        listItemCreator.create(
                            FingerprintMapId.SWIPE_DOWN,
                            fingerprintMaps.swipeDown
                        ),
                        listItemCreator.create(
                            FingerprintMapId.SWIPE_UP,
                            fingerprintMaps.swipeUp
                        ),
                        listItemCreator.create(
                            FingerprintMapId.SWIPE_LEFT,
                            fingerprintMaps.swipeLeft
                        ),
                        listItemCreator.create(
                            FingerprintMapId.SWIPE_RIGHT,
                            fingerprintMaps.swipeRight
                        )
                    ).createListState()
                }
            }
        }

        coroutineScope.launch {
            useCase.fingerprintMaps.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        coroutineScope.launch {
            useCase.invalidateErrors.collectLatest {
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
        runBlocking { _launchConfigFingerprintMap.emit(id) }
    }

    fun onBackupAllClick() {
        runBlocking { _requestFingerprintMapsBackup.emit(Unit) }
    }

    fun onResetClick() {
        useCase.resetFingerprintMaps()
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
            val actionText = if (error.isFixable){
                getString(R.string.snackbar_fix)
            }else{
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