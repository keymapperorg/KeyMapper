package io.github.sds100.keymapper.system.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.apps.PACKAGE_INFO_TYPES
import io.github.sds100.keymapper.system.apps.PackageUtils
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.formatSeconds
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SimpleListItem
import kotlinx.coroutines.Dispatchers
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

class ChooseUiElementViewModel constructor(
    useCase: DisplayUiElementsUseCase,
    resourceProvider: ResourceProvider,
    recordUiElements: RecordUiElementsUseCase,
    serviceAdapter: ServiceAdapter,
) : ViewModel(),
    ResourceProvider by resourceProvider
{

    private val _serviceAdapter = serviceAdapter

    val searchQuery = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(
        UiElementsListState(State.Loading)
    )
    val state = _state.asStateFlow()

    private val allAppListItems = useCase.uiElements.map { state ->
        state.mapData { uiElements ->
            uiElements.buildListItems()
        }
    }.flowOn(Dispatchers.Default)

    private val _returnResult = MutableSharedFlow<UiElementInfo>()
    val returnResult = _returnResult.asSharedFlow()

    val recordButtonText: StateFlow<String> = recordUiElements.state.map { recordUiElementsState ->
        when (recordUiElementsState) {
            is RecordUiElementsState.CountingDown -> getString(R.string.extra_label_interact_with_screen_element_record_button_text_active, formatSeconds(recordUiElementsState.timeLeft))
            is RecordUiElementsState.Stopped -> getString(R.string.extra_label_interact_with_screen_element_record_button_text_start)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, getString(R.string.extra_label_interact_with_screen_element_record_button_text_start))

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

    fun stopRecording() {
        viewModelScope.launch(Dispatchers.Default) {
            _serviceAdapter.send(Event.StopRecordingUiElements)
        }
    }

    init {

        viewModelScope.launch {
            useCase.updateUiElementsList()
        }

        combine(
            allAppListItems,
            searchQuery
        ) { allAppListItems, query ->
            when (allAppListItems) {
                is State.Data -> {
                    allAppListItems.data.filterByQuery(query).collectLatest { filteredListItems ->
                        _state.value = UiElementsListState(filteredListItems)
                    }
                }

                is State.Loading -> _state.value = UiElementsListState(State.Loading)
            }
        }.launchIn(viewModelScope)
    }

    fun onListItemClick(id: String) {
        Timber.d("onListItemClick: %s", id)

        stopRecording()

        val elementViewId = PackageUtils.getInfoFromFullViewId(id, PACKAGE_INFO_TYPES.TYPE_VIEW_ID)
        val elementPackageName = PackageUtils.getInfoFromFullViewId(id, PACKAGE_INFO_TYPES.TYPE_PACKAGE_NAME)

        if (elementViewId != null && elementPackageName != null) {
            viewModelScope.launch {
                _returnResult.emit(
                    UiElementInfo(
                        elementName = elementViewId,
                        packageName = elementPackageName,
                        fullName = id
                    )
                )
            }
        }
    }

    fun onRecordButtonClick() {
        Timber.d("onRecordButtonClick isRecording: %s", _isRecording.value.toString())
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
            _serviceAdapter.send(Event.StopRecordingUiElements)
            _serviceAdapter.send(Event.ClearRecordedUiElements)
        }
    }

    private suspend fun List<UiElementInfo>.buildListItems(): List<SimpleListItem> = flow {
        forEach { uiElementInfo ->
            val listItem = DefaultSimpleListItem(
                id = uiElementInfo.fullName,
                title = uiElementInfo.elementName,
                subtitle = uiElementInfo.packageName,
            )

            emit(listItem)
        }
    }.flowOn(Dispatchers.Default).toList().sortedBy { it.id.lowercase(Locale.getDefault()) }

    override fun onCleared() {
        super.onCleared()

        stopRecording()
    }

    class Factory(
        private val useCase: DisplayUiElementsUseCase,
        private val resourceProvider: ResourceProvider,
        private val recordUiElements: RecordUiElementsUseCase,
        private val serviceAdapter: ServiceAdapter,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ChooseUiElementViewModel(useCase, resourceProvider, recordUiElements, serviceAdapter) as T
    }
}


data class UiElementsListState(
    val listItems: State<List<SimpleListItem>>
)
