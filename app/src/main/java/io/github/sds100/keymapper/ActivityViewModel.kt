package io.github.sds100.keymapper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.util.ui.BaseViewModel
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 23/07/2021.
 */
class ActivityViewModel(
    resourceProvider: ResourceProvider
) : BaseViewModel(resourceProvider) {
    var previousNightMode: Int? = null

    private val _openUrl: MutableSharedFlow<String> = MutableSharedFlow()
    val openUrl: SharedFlow<String> = _openUrl.asSharedFlow()

    fun onCantFindAccessibilitySettings() {
        viewModelScope.launch {
            ViewModelHelper.handleCantFindAccessibilitySettings(this@ActivityViewModel)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ActivityViewModel(resourceProvider) as T
        }
    }
}