package io.github.sds100.keymapper.actions.uielementinteraction

import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class InteractWithScreenElementViewModel(
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val _interactionTypes = INTERACTIONTYPE.values().map { it.name }

    private val _returnResult = MutableSharedFlow<InteractWithScreenElementResult>()
    val returnResult = _returnResult.asSharedFlow()

    private val _elementId = MutableStateFlow<String?>(null)
    private val _packageName = MutableStateFlow<String?>(null)
    private val _fullName = MutableStateFlow<String?>(null)
    private val _onlyIfVisible: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    private val _interactionType: MutableStateFlow<INTERACTIONTYPE?> =
        MutableStateFlow(INTERACTIONTYPE.CLICK)
    private val _description: MutableStateFlow<String?> = MutableStateFlow(null)

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

    val onlyIfVisible = _onlyIfVisible.stateIn(viewModelScope, SharingStarted.Lazily, false)

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
            INTERACTIONTYPE.CLICK.name -> _interactionType.value = INTERACTIONTYPE.CLICK
            INTERACTIONTYPE.LONG_CLICK.name -> _interactionType.value = INTERACTIONTYPE.LONG_CLICK
            else -> _interactionType.value = INTERACTIONTYPE.CLICK
        }
    }

    private fun setDescription(description: String) {
        this._description.value = description
    }

    private fun setOnlyIfVisible(onlyIfVisible: Boolean) {
        this._onlyIfVisible.value = onlyIfVisible
    }

    fun onInteractionTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        this.setInteractionType(_interactionTypes[position])
    }

    fun onChooseUiElementButtonClick() {
        viewModelScope.launch {
            onSelectUiElement()
        }
    }

    fun onOnlyIfVisibleCheckboxChange(checked: Boolean) {
        this.setOnlyIfVisible(checked)
    }

    fun loadResult(result: InteractWithScreenElementResult) {

        Timber.d("loadResult %s", result.toString())

        viewModelScope.launch {
            setElementId(result.elementId)
            setPackageName(result.packageName)
            setFullName(result.fullName)
            setInteractionType(result.interactionType.name)
            setOnlyIfVisible(result.onlyIfVisible)
            setDescription(result.description)
        }
    }

    private suspend fun onSelectUiElement() {
        val uiElementInfo =
            navigate(NavDestination.ID_CHOOSE_UI_ELEMENT, NavDestination.ChooseUiElement) ?: return
        setElementId(uiElementInfo.elementName)
        setPackageName(uiElementInfo.packageName)
        setFullName(uiElementInfo.fullName)
    }

    val isDoneButtonEnabled: StateFlow<Boolean> =
        combine(_elementId, _packageName, _fullName) { elementId, packageName, fullName ->
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
            val interactiontype = _interactionType.value ?: return@launch

            val description = showPopup(
                "ui_element_description",
                PopupUi.Text(
                    getString(R.string.hint_interact_with_screen_element_description),
                    allowEmpty = true,
                    text = _description.value ?: ""
                )
            ) ?: return@launch

            _returnResult.emit(
                InteractWithScreenElementResult(
                    elementId,
                    packageName,
                    fullName,
                    onlyIfVisible,
                    interactiontype,
                    description
                )
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InteractWithScreenElementViewModel(resourceProvider) as T
        }
    }
}