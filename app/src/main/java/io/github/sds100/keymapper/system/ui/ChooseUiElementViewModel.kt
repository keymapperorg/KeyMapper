package io.github.sds100.keymapper.system.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.apps.DisplayAppsUseCase
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.formatSeconds
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

class ChooseUiElementViewModel(
    resourceProvider: ResourceProvider,
    private val recordUiElements: RecordUiElementsUseCase,
    serviceAdapter: ServiceAdapter,
    displayAppsUseCase: DisplayAppsUseCase,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DisplayAppsUseCase by displayAppsUseCase {

    private val _serviceAdapter = serviceAdapter

    val searchQuery = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(
        UiElementsListState(State.Loading)
    )
    val state = _state.asStateFlow()

    private val allUiElementListItems: Flow<List<UiElementInfoListItem>> = recordUiElements.uiElements.map { model ->
        buildListItems(model)
    }.flowOn(Dispatchers.Default)

    private val _returnResult = MutableSharedFlow<UiElementInfo>()
    val returnResult = _returnResult.asSharedFlow()

    val recordButtonText: StateFlow<String> = recordUiElements.state.map { recordUiElementsState ->
        when (recordUiElementsState) {
            is RecordUiElementsState.CountingDown -> getString(
                R.string.button_label_choose_ui_element_record_button_text_active,
                formatSeconds(recordUiElementsState.timeLeft)
            )

            is RecordUiElementsState.Stopped -> getString(R.string.button_label_choose_ui_element_record_button_text_start)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, getString(R.string.button_label_choose_ui_element_record_button_text_start))

    val recordDescriptionText: StateFlow<String> = recordUiElements.state.map { recordUiElementsState ->
        when  (recordUiElementsState) {
            is RecordUiElementsState.CountingDown -> getString(R.string.extra_label_interact_with_screen_element_record_description_text_active)
            is RecordUiElementsState.Stopped -> getString(R.string.extra_label_interact_with_screen_element_record_description_text_start)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, getString(R.string.extra_label_interact_with_screen_element_record_description_text_start))

    private val _isRecording: StateFlow<Boolean> = recordUiElements.state.map { recordUiElementsState ->
        when (recordUiElementsState) {
            is RecordUiElementsState.CountingDown -> true
            is RecordUiElementsState.Stopped -> false
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val recordButtonEnabled: StateFlow<Boolean> = serviceAdapter.state.map{ accessibilityServiceState ->
        accessibilityServiceState == ServiceState.ENABLED
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly,  false)

    val recordButtonAlpha: StateFlow<Float> = serviceAdapter.state.map{ accessibilityServiceState ->
        when (accessibilityServiceState) {
            ServiceState.ENABLED -> 1f
            else -> 0.5f
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly,  1f)

    fun stopRecording() {
        viewModelScope.launch(Dispatchers.Default) {
            _serviceAdapter.send(Event.StopRecordingUiElements)
        }
    }

    init {
        combine(
            allUiElementListItems,
            searchQuery
        ) { listItems, query ->
            listItems.filterByQuery(query).collectLatest { filteredListItems ->
                _state.value = UiElementsListState(filteredListItems)
            }
        }.launchIn(viewModelScope)
    }

    fun onListItemClick(id: String) {
        Timber.d("onListItemClick: %s", id)

        stopRecording()

        val model = recordUiElements.uiElements.value.find { it.fullName == id }

        if (model != null) {
            viewModelScope.launch {
                _returnResult.emit(model)
            }
        }
    }

    fun onRecordButtonClick() {
        if (_isRecording.value) {
            viewModelScope.launch(Dispatchers.Default) {
                _serviceAdapter.send(Event.StopRecordingUiElements)
            }
        } else {
            viewModelScope.launch(Dispatchers.Default) {
                _serviceAdapter.send(Event.StartRecordingUiElements)
            }
        }
    }

    fun onClearListButtonClick() {
        viewModelScope.launch(Dispatchers.Default) {
            recordUiElements.stopRecording()
            recordUiElements.clearElements()
        }
    }

    private suspend fun buildListItems(model: List<UiElementInfo>): List<UiElementInfoListItem> = flow {
        for (uiElementInfo in model) {
            val icon = getAppIcon(uiElementInfo.packageName).valueOrNull()
            val appName = getAppName(uiElementInfo.packageName).valueOrNull()

            val listItem = UiElementInfoListItem(
                id = uiElementInfo.fullName,
                title = uiElementInfo.elementName,
                subtitle = appName ?: uiElementInfo.packageName,
                icon = if (icon != null) IconInfo(icon) else null
            )

            emit(listItem)
        }
    }.flowOn(Dispatchers.Default).toList().sortedBy { it.id.lowercase(Locale.getDefault()) }

    override fun onCleared() {
        super.onCleared()

        stopRecording()
    }

    class Factory(
        private val resourceProvider: ResourceProvider,
        private val recordUiElements: RecordUiElementsUseCase,
        private val serviceAdapter: ServiceAdapter,
        private val displayAppsUseCase: DisplayAppsUseCase,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ChooseUiElementViewModel(resourceProvider, recordUiElements, serviceAdapter, displayAppsUseCase) as T
    }
}


data class UiElementsListState(
    val listItems: State<List<UiElementInfoListItem>>
)
