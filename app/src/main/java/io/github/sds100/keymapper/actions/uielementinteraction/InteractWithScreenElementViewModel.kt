package io.github.sds100.keymapper.actions.uielementinteraction

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.apps.DisplayAppsUseCase
import io.github.sds100.keymapper.system.ui.UiElementInfo
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

    private val interactionTypes = InteractionType.values().map { it.name }
    private val interactionType: MutableStateFlow<InteractionType?> =
        MutableStateFlow(InteractionType.values().first())

    private val _returnResult = MutableSharedFlow<InteractWithScreenElementResult>()
    val returnResult = _returnResult.asSharedFlow()

    var showPackageInfoOnly = false

    private val uiElement: MutableStateFlow<UiElementInfo?> = MutableStateFlow(null)
    val elementId: StateFlow<String?> =
        uiElement
            .map { it?.elementName }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val packageName: StateFlow<String?> =
        uiElement
            .map { it?.packageName }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val fullName: StateFlow<String?> =
        uiElement
            .map { it?.fullName }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val appName: StateFlow<String?> =
        uiElement
            .map { model ->
                model?.let { displayAppsUseCase.getAppName(it.packageName).valueOrNull() }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    var appIcon: StateFlow<Drawable?> =
        uiElement
            .map { model ->
                model?.let { displayAppsUseCase.getAppIcon(it.packageName).valueOrNull() }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    var interactOnlyIfVisible: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    val description: MutableStateFlow<String?> = MutableStateFlow(null)

    val interactionTypeSpinnerSelection = interactionType.map {
        it ?: return@map 0

        this.interactionTypes.indexOf(it.name)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private fun setInteractionType(type: String) {
        interactionType.value = InteractionType.valueOf(type.uppercase(Locale.ROOT))
    }

    fun onInteractionTypeSelected(position: Int) {
        this.setInteractionType(interactionTypes[position])
    }

    fun onChooseUiElementButtonClick() {
        viewModelScope.launch {
            onSelectUiElement()
        }
    }

    fun onOnlyIfVisibleCheckboxChange(checked: Boolean) {
        interactOnlyIfVisible.value = checked
    }

    fun loadResult(result: InteractWithScreenElementResult) {
        viewModelScope.launch {
            uiElement.value = result.uiElement
            interactionType.value = result.interactionType
            interactOnlyIfVisible.value = result.onlyIfVisible
            description.value = result.description
        }
    }

    private suspend fun onSelectUiElement() {
        uiElement.value =
            navigate(NavDestination.ID_CHOOSE_UI_ELEMENT, NavDestination.ChooseUiElement) ?: return
    }

    val isDoneButtonEnabled: StateFlow<Boolean> =
        uiElement.map { it != null }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun onDoneClick() {
        viewModelScope.launch {
            if (uiElement.value == null) {
                return@launch
            }

            val onlyIfVisible = interactOnlyIfVisible.value ?: return@launch
            val interactionType = interactionType.value ?: return@launch

            val description = showPopup(
                "ui_element_description",
                PopupUi.Text(
                    getString(R.string.hint_interact_with_screen_element_description),
                    allowEmpty = true,
                    text = description.value ?: "",
                ),
            ) ?: return@launch

            _returnResult.emit(
                InteractWithScreenElementResult(
                    uiElement.value!!,
                    onlyIfVisible,
                    interactionType,
                    description,
                ),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
        private val displayAppsUseCase: DisplayAppsUseCase,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InteractWithScreenElementViewModel(resourceProvider, displayAppsUseCase) as T
    }
}
