package io.github.sds100.keymapper.actions.uielementinteraction

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.apps.DisplayAppsUseCase
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class InteractWithScreenElementViewModel(
    resourceProvider: ResourceProvider,
    displayAppsUseCase: DisplayAppsUseCase,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl(),
    DisplayAppsUseCase by displayAppsUseCase {

    private val _interactionTypes = INTERACTIONTYPE.values().map { it.name }
    private val _interactionType: MutableStateFlow<INTERACTIONTYPE?> =
        MutableStateFlow(INTERACTIONTYPE.values().first())

    private val _returnResult = MutableSharedFlow<InteractWithScreenElementResult>()
    val returnResult = _returnResult.asSharedFlow()

    val elementId = MutableStateFlow<String?>(null)
    val packageName = MutableStateFlow<String?>(null)
    val fullName = MutableStateFlow<String?>(null)
    val appName = MutableStateFlow<String?>(null)
    var appIcon: MutableStateFlow<Drawable?> = MutableStateFlow(null)
    var onlyIfVisible: MutableStateFlow<Boolean?> = MutableStateFlow(true)

    val description: MutableStateFlow<String?> = MutableStateFlow(null)

    val interactionTypeSpinnerSelection = _interactionType.map {
        it ?: return@map 0

        this._interactionTypes.indexOf(it.name)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private fun setInteractionType(type: String) {
        _interactionType.value = INTERACTIONTYPE.valueOf(type.uppercase(Locale.ROOT))
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
        onlyIfVisible.value = checked
    }

    fun loadResult(result: InteractWithScreenElementResult) {
        viewModelScope.launch {
            elementId.value = result.elementId
            packageName.value = result.packageName
            fullName.value = result.fullName
            appName.value = result.appName
            _interactionType.value = result.interactionType
            onlyIfVisible.value = result.onlyIfVisible
            description.value = result.description
            appIcon.value = getAppIcon(result.packageName).valueOrNull()
        }
    }

    private suspend fun onSelectUiElement() {
        val uiElementInfo =
            navigate(NavDestination.ID_CHOOSE_UI_ELEMENT, NavDestination.ChooseUiElement) ?: return
        elementId.value = uiElementInfo.elementName
        packageName.value = uiElementInfo.packageName
        fullName.value = uiElementInfo.fullName
        appName.value = uiElementInfo.appName
        appIcon.value = getAppIcon(uiElementInfo.packageName).valueOrNull()
    }

    val isDoneButtonEnabled: StateFlow<Boolean> =
        combine(
            elementId,
            packageName,
            fullName,
            appName
        ) { elementId, packageName, fullName, appName ->
            elementId ?: return@combine false
            packageName ?: return@combine false
            fullName ?: return@combine false
            appName ?: return@combine false

            elementId.isNotEmpty() && packageName.isNotEmpty() && fullName.isNotEmpty() && appName.isNotEmpty()
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun onDoneClick() {
        viewModelScope.launch {
            val elementId = elementId.value ?: return@launch
            val packageName = packageName.value ?: return@launch
            val fullName = fullName.value ?: return@launch
            val appName = appName.value ?: return@launch
            val onlyIfVisible = onlyIfVisible.value ?: return@launch
            val interactiontype = _interactionType.value ?: return@launch

            val description = showPopup(
                "ui_element_description",
                PopupUi.Text(
                    getString(R.string.hint_interact_with_screen_element_description),
                    allowEmpty = true,
                    text = description.value ?: ""
                )
            ) ?: return@launch

            _returnResult.emit(
                InteractWithScreenElementResult(
                    elementId,
                    packageName,
                    fullName,
                    appName,
                    onlyIfVisible,
                    interactiontype,
                    description
                )
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
        private val displayAppsUseCase: DisplayAppsUseCase
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InteractWithScreenElementViewModel(resourceProvider, displayAppsUseCase) as T
        }
    }
}