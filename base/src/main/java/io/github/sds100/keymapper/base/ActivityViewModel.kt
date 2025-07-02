package io.github.sds100.keymapper.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.NavigationProviderImpl
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProviderImpl
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by NavigationProviderImpl() {

    var handledActivityLaunchIntent: Boolean = false
    var previousNightMode: Int? = null

    fun onCantFindAccessibilitySettings() {
        viewModelScope.launch {
            ViewModelHelper.handleCantFindAccessibilitySettings(
                resourceProvider = this@ActivityViewModel,
                dialogProvider = this@ActivityViewModel,
            )
        }
    }
}
