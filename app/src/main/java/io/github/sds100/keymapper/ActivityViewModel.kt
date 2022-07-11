package io.github.sds100.keymapper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by sds100 on 23/07/2021.
 */
@HiltViewModel
class ActivityViewModel @Inject constructor(
    resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    var previousNightMode: Int? = null

    fun onCantFindAccessibilitySettings() {
        viewModelScope.launch {
            ViewModelHelper.handleCantFindAccessibilitySettings(
                resourceProvider = this@ActivityViewModel,
                popupViewModel = this@ActivityViewModel
            )
        }
    }
}