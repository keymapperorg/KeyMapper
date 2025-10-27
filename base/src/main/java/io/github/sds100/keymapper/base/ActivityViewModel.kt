package io.github.sds100.keymapper.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.onboarding.SetupAccessibilityServiceDelegate
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val setupAccessibilityServiceDelegate: SetupAccessibilityServiceDelegate,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    var previousNightMode: Int? = null

    fun onCantFindAccessibilitySettings() {
        setupAccessibilityServiceDelegate.showCantFindAccessibilitySettingsDialog()
    }

    fun launchProModeSetup() {
        viewModelScope.launch {
            navigate("pro_mode_setup", NavDestination.ProModeSetup)
        }
    }
}
