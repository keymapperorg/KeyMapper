package io.github.sds100.keymapper.base.promode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ProModeSetupViewModel @Inject constructor(
    delegate: ProModeSetupDelegate,
    navigationProvider: NavigationProvider,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ProModeSetupDelegate by delegate,
    NavigationProvider by navigationProvider,
    ResourceProvider by resourceProvider {

    fun onBackClick() {
        viewModelScope.launch {
            popBackStack()
        }
    }
}
