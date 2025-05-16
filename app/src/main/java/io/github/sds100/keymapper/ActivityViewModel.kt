package io.github.sds100.keymapper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import kotlinx.coroutines.launch

class ActivityViewModel(
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    var handledActivityLaunchIntent: Boolean = false
    var previousNightMode: Int? = null

    fun onCantFindAccessibilitySettings() {
        viewModelScope.launch {
            ViewModelHelper.handleCantFindAccessibilitySettings(
                resourceProvider = this@ActivityViewModel,
                popupViewModel = this@ActivityViewModel,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ActivityViewModel(resourceProvider) as T
    }
}
