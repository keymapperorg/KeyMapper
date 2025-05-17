package io.github.sds100.keymapper.floating

import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ListFloatingLayoutsViewModel(
    val coroutineScope: CoroutineScope,
    val useCase: ListFloatingLayoutsUseCase,
    resourceProvider: ResourceProvider,
) : PopupViewModel by PopupViewModelImpl() {
    val state: StateFlow<FloatingLayoutsState> = MutableStateFlow(FloatingLayoutsState.NotPurchased)
    val showFabText: Boolean = false

    fun onNewLayoutClick() {
    }
}

sealed class FloatingLayoutsState {
    data object Loading : FloatingLayoutsState()
    data object NotPurchased : FloatingLayoutsState()
    data object Purchased : FloatingLayoutsState()
}
