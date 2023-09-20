package io.github.sds100.keymapper.actions.tapscreenelement

import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.repositories.ViewIdRepository
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerState
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.formatSeconds
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SimpleListItem
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PickScreenElementViewModel(
    resourceProvider: ResourceProvider,
    viewIdRepository: ViewIdRepository,
    serviceAdapter: ServiceAdapter,
    recordUiElements: RecordUiElementsUseCase
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val _viewIdRepository = viewIdRepository
    private val _serviceAdapter = serviceAdapter
    private val _interactionTypes = arrayOf(INTERACTION_TYPES.CLICK.name, INTERACTION_TYPES.LONG_CLICK.name )

    private val _listItems = MutableStateFlow<State<List<SimpleListItem>>>(State.Loading)
    val listItems = _listItems.asStateFlow()
    val listItemsSearchQuery: MutableStateFlow<String?> = MutableStateFlow<String?>(null)

    private val _returnResult = MutableSharedFlow<PickScreenElementResult>()
    val returnResult = _returnResult.asSharedFlow()

    private val _elementId = MutableStateFlow<String?>(null)
    private val _packageName = MutableStateFlow<String?>(null)
    private val _fullName = MutableStateFlow<String?>(null)
    private val _onlyIfVisible: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    private val _description: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _interactionType: MutableStateFlow<INTERACTION_TYPES?> = MutableStateFlow(INTERACTION_TYPES.CLICK)

    val elementId = _elementId.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val packageName = _packageName.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val fullName = _fullName.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val interactionTypeSpinnerSelection = _interactionType.map {
        it ?: return@map 0

        this._interactionTypes.indexOf(it.name)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private fun setElementId(id: String) {
        this._elementId.value = id
    }

    private fun setPackageName(name: String) {
        this._packageName.value = name
    }

    private fun setFullName(name: String) {
        this._fullName.value = name
    }

    private fun setInteractionType(type: String) {

        when (type) {
            INTERACTION_TYPES.CLICK.name -> _interactionType.value = INTERACTION_TYPES.CLICK
            INTERACTION_TYPES.LONG_CLICK.name -> _interactionType.value = INTERACTION_TYPES.LONG_CLICK
            else -> _interactionType.value = INTERACTION_TYPES.CLICK
        }
    }

    fun onInteractionTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        this.setInteractionType(_interactionTypes[position])
    }

    fun onListItemClick(elementId: String, packageName: String) {
        Timber.d("onListItemClick %s %s", elementId, packageName)

        setElementId(elementId)
        setPackageName(packageName)
        setFullName("${packageName}/${elementId}")
    }

    private val _isRecording: StateFlow<Boolean> = recordUiElements.state.map { recordUiElementsState ->
        when (recordUiElementsState) {
            is RecordUiElementsState.CountingDown -> true
            is RecordUiElementsState.Stopped -> false
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val recordButtonText: StateFlow<String> = recordUiElements.state.map { recordUiElementsState ->
        when (recordUiElementsState) {
            is RecordUiElementsState.CountingDown -> getString(R.string.extra_label_pick_screen_element_record_button_text_active, formatSeconds(recordUiElementsState.timeLeft))
            is RecordUiElementsState.Stopped -> getString(R.string.extra_label_pick_screen_element_record_button_text_start)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, getString(R.string.extra_label_pick_screen_element_record_button_text_start))

    val recordDescriptionText: StateFlow<String> = recordUiElements.state.map {recordUiElementsState ->
        when  (recordUiElementsState) {
            is RecordUiElementsState.CountingDown -> getString(R.string.extra_label_pick_screen_element_record_description_text_active)
            is RecordUiElementsState.Stopped -> getString(R.string.extra_label_pick_screen_element_record_description_text_start)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, getString(R.string.extra_label_pick_screen_element_record_description_text_start))

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

    fun onSearchInputChange() {
        Timber.d("onSearchInputChange")
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            _listItems.value = State.Data(buildListItems())
        }
    }

    private suspend fun buildListItems(): List<SimpleListItem> {
        Timber.d("buildListItems: Query: %s", listItemsSearchQuery.value.toString())
        _viewIdRepository.viewIdList.collect { data ->

            val listItems = arrayListOf<DefaultSimpleListItem>()

            data.dataOrNull()?.forEachIndexed { _, viewIdEntity ->
                listItems.add(
                    DefaultSimpleListItem(
                        id = viewIdEntity.id.toString(),
                        title = viewIdEntity.viewId,
                        subtitle = viewIdEntity.packageName,
                    )
                )
            }

            viewModelScope.launch(Dispatchers.Default) {
                _listItems.value = State.Data(listItems)
            }

        }

        return arrayListOf()
    }

    val isDoneButtonEnabled: StateFlow<Boolean> = combine(_elementId, _packageName, _fullName) { elementId, packageName, fullName ->
        elementId ?: return@combine false
        packageName ?: return@combine false
        fullName ?: return@combine false

        elementId.isNotEmpty() && packageName.isNotEmpty() && fullName.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun onDoneClick() {
        viewModelScope.launch(Dispatchers.Default) {
            _serviceAdapter.send(Event.StopRecordingUiElements)
        }

        viewModelScope.launch {
            val elementId = _elementId.value ?: return@launch
            val packageName = _packageName.value ?: return@launch
            val fullName = _fullName.value ?: return@launch
            val onlyIfVisible = _onlyIfVisible.value ?: return@launch

            val description = showPopup(
                "coordinate_description",
                PopupUi.Text(
                    getString(R.string.hint_tap_coordinate_title),
                    allowEmpty = true,
                    text = _description.value ?: ""
                )
            ) ?: return@launch

            _returnResult.emit(PickScreenElementResult(elementId, packageName, fullName, onlyIfVisible, description))
        }
    }

    override fun onCleared() {
        super.onCleared()

        viewModelScope.launch(Dispatchers.Default) {
            _serviceAdapter.send(Event.StopRecordingUiElements)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
        private val viewIdRepository: ViewIdRepository,
        private val serviceAdapter: ServiceAdapter,
        private val recordUiElements: RecordUiElementsUseCase
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PickScreenElementViewModel(resourceProvider, viewIdRepository, serviceAdapter, recordUiElements) as T
        }
    }
}