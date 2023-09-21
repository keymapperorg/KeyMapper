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
import io.github.sds100.keymapper.system.ui.UiElementInfo
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.formatSeconds
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SimpleListItem
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PickScreenElementViewModel(
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val _interactionTypes = arrayOf(INTERACTION_TYPES.CLICK.name, INTERACTION_TYPES.LONG_CLICK.name )

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

    fun onChooseUiElementButtonClick() {
        viewModelScope.launch {
            onSelectUiElement()
        }
    }

    private suspend fun onSelectUiElement() {
        val uiElementInfo = navigate(NavDestination.ID_CHOOSE_UI_ELEMENT, NavDestination.ChooseUiElement) ?:return
        setElementId(uiElementInfo.elementName)
        setPackageName(uiElementInfo.packageName)
        setFullName(uiElementInfo.fullName)
    }

    val isDoneButtonEnabled: StateFlow<Boolean> = combine(_elementId, _packageName, _fullName) { elementId, packageName, fullName ->
        elementId ?: return@combine false
        packageName ?: return@combine false
        fullName ?: return@combine false

        elementId.isNotEmpty() && packageName.isNotEmpty() && fullName.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun onDoneClick() {
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

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PickScreenElementViewModel(resourceProvider) as T
        }
    }
}