package io.github.sds100.keymapper.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    var previousNightMode: Int? = null

    fun onCantFindAccessibilitySettings() {
        viewModelScope.launch {
            ViewModelHelper.handleCantFindAccessibilitySettings(
                resourceProvider = this@ActivityViewModel,
                dialogProvider = this@ActivityViewModel,
            )
        }
    }

    fun launchProModeSetup() {
        viewModelScope.launch {
            navigate("pro_mode_setup", NavDestination.ProModeSetup)
        }
    }
}
